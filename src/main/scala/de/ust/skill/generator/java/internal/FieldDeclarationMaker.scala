/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.java.GeneralOutputMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import jdk.nashorn.internal.codegen.CompilerConstants.FieldAccess
import de.ust.skill.ir.Field

trait FieldDeclarationMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    // project IR, so that reflection implements the binary file format correctly
    val IR = this.types.removeSpecialDeclarations.getUsertypes

    for (t ← IR; f ← t.getFields) {
      // the field before interface projection
      val originalF = this.types.removeTypedefs.removeEnums.get(t.getSkillName).asInstanceOf[UserType]
        .getAllFields.find(_.getName == f.getName).get

      // the type before the interface projection
      val fieldActualType = mapType(originalF.getType, true)
      val fieldActualTypeUnboxed = mapType(originalF.getType, false)

      val tIsBaseType = t.getSuperType == null

      val nameT = mapType(t)
      val nameF = knownField(f)

      // casting access to data array using index i
      val dataAccessI = if (null == t.getSuperType) "data[i]" else s"((${mapType(t)})data[i])"
      val fieldAccess = s"""get${escaped(f.getName.capital)}()"""

      val out = files.open(s"internal/$nameF.java")
      //package
      out.write(s"""package ${packagePrefix}internal;

import java.io.IOException;
import java.util.ArrayList;

import de.ust.skill.common.java.internal.*;
import de.ust.skill.common.java.internal.fieldDeclarations.*;
import de.ust.skill.common.java.internal.fieldTypes.Annotation;
import de.ust.skill.common.java.internal.fieldTypes.MapType;
import de.ust.skill.common.java.internal.fieldTypes.SingleArgumentType;
import de.ust.skill.common.java.internal.fieldTypes.StringType;
import de.ust.skill.common.java.internal.fieldTypes.V64;
import de.ust.skill.common.java.internal.parts.*;
import de.ust.skill.common.jvm.streams.MappedInStream;
import de.ust.skill.common.jvm.streams.MappedOutStream;

""")

      out.write(s"""
/**
 * ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
 */
${
        suppressWarnings
      }final class $nameF extends ${
        if (f.isAuto) "AutoField"
        else "FieldDeclaration"
      }<$fieldActualType, ${mapType(t)}> implements
               ${
        f.getType match {
          case ft : GroundType ⇒ ft.getSkillName match {
            case "bool"                  ⇒ s"""KnownBooleanField<${mapType(t)}>"""
            case "i8"                    ⇒ s"""KnownByteField<${mapType(t)}>"""
            case "i16"                   ⇒ s"""KnownShortField<${mapType(t)}>"""
            case "i32"                   ⇒ s"""KnownIntField<${mapType(t)}>"""
            case "i64" | "v64"           ⇒ s"""KnownLongField<${mapType(t)}>"""
            case "f32"                   ⇒ s"""KnownFloatField<${mapType(t)}>"""
            case "f64"                   ⇒ s"""KnownDoubleField<${mapType(t)}>"""
            case "annotation" | "string" ⇒ s"""KnownField<$fieldActualType, ${mapType(t)}>"""
            case ft                      ⇒ "???missing specialization for type " + ft
          }
          case _ ⇒ s"""KnownField<$fieldActualType, ${mapType(t)}>"""
        }
      }${
        // mark ignored fields as ignored; read function is inherited
        if (f.isIgnored()) ", IgnoredField"
        else ""
      }${
        // mark interface fields
        if (f.getType.isInstanceOf[InterfaceType]) ", InterfaceField"
        else ""
      } {

    public $nameF(FieldType<$fieldActualType> type, ${
        if (f.isAuto()) ""
        else "int index, "
      }${name(t)}Access owner) {
        super(type, "${f.getSkillName}", ${
        if (f.isAuto()) "0"
        else "index"
      }, owner);
            // TODO insert known restrictions?
    }
${
        if (f.isAuto) "" else s"""
    @Override
    public void read(ChunkEntry ce) {${
          if (f.isConstant())
            """
        // this field is constant"""
          else{
              // preparation code
            val pre = originalF.getType match {
                case t : GroundType if "string".equals(t.getSkillName) ⇒ s"""
        final StringPool sp = (StringPool)owner.owner().Strings();"""

                case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"""
        final ${name(t.getSuperType)}Access target = (${name(t.getSuperType)}Access)
                FieldDeclaration.<${mapType(t.getSuperType)},${mapType(t)}>cast(type);"""

                case t : UserType ⇒ s"""
        final ${name(t)}Access target = (${name(t)}Access)type;"""
                case _ ⇒ ""
              } 
            
            s"""
        final Chunk last = ce.c;
        
        if (last instanceof SimpleChunk) {
            rsc((SimpleChunk) last, ce.in);
        } else {
            rbc((BulkChunk) last, ce.in);
        }
    }
    
    private final void rsc(SimpleChunk c, MappedInStream in){$pre
        final ${mapType(t.getBaseType)}[] d = ((${storagePool(t.getBaseType)}) owner.basePool()).data();
        int i = (int) c.bpo;
        final int high = i + (int) c.count;
        while (i != high) {
            ${readField(t, originalF, fieldActualType)}
            i++;
        }
    }
    
    private final void rbc(BulkChunk c, MappedInStream in){$pre
        final ${mapType(t.getBaseType)}[] d = ((${storagePool(t.getBaseType)}) owner.basePool()).data();
        ArrayList<Block> blocks = owner.blocks();
        int blockIndex = 0;
        final int endBlock = c.blockCount;
        while (blockIndex < endBlock) {
            Block b = blocks.get(blockIndex++);
            int i = (int) b.bpo;
            final int high = i + (int) b.count;
            while (i != high) {
                ${readField(t, originalF, fieldActualType)}
                i++;
            }
        }"""
        }
        }
    }

    @Override
    public long offset() {${
          if (f.isConstant())
            """
        return 0; // this field is constant"""
          else {
            val (pre, code, isFast) = offsetCode(t, f, originalF.getType, fieldActualType);

            if (isFast) s"""
        Chunk last = dataChunks.getLast().c;
        $code"""
            else s"""
        Chunk last = dataChunks.getLast().c;
        
        if (last instanceof SimpleChunk) {
            return osc((SimpleChunk) last);
        } else {
            return obc((BulkChunk) last);
        }
    }
    
    private final long osc(SimpleChunk c){$pre
        final ${mapType(t.getBaseType)}[] d = ((${storagePool(t.getBaseType)}) owner.basePool()).data();
        long result = 0L;
        int i = (int) c.bpo;
        final int high = i + (int) c.count;
        while (i != high) {
            $code
            i++;
        }
        return result;
    }
    
    private final long obc(BulkChunk c){$pre
        final ${mapType(t.getBaseType)}[] d = ((${storagePool(t.getBaseType)}) owner.basePool()).data();
        long result = 0L;
        ArrayList<Block> blocks = owner.blocks();
        int blockIndex = 0;
        final int endBlock = c.blockCount;
        while (blockIndex < endBlock) {
            Block b = blocks.get(blockIndex++);
            int i = (int) b.bpo;
            final int high = i + (int) b.count;
            while (i != high) {
                $code
                i++;
            }
        }
        return result;"""
          }
        }
    }

    @Override
    public void write(MappedOutStream out) throws IOException {${
          if (f.isConstant())
            """
        // this field is constant"""
          else {
            s"""
        Chunk last = dataChunks.getLast().c;

        if (last instanceof SimpleChunk) {
            wsc((SimpleChunk) last, out);
        } else {
            wbc((BulkChunk) last, out);
        }
    }
    
    private final void wsc(SimpleChunk c, MappedOutStream out) throws IOException {
        final ${mapType(t.getBaseType)}[] d = ((${storagePool(t.getBaseType)}) owner.basePool()).data();
        long result = 0L;
        int i = (int) c.bpo;
        final int high = i + (int) c.count;
        while (i != high) {
            ${writeCode(t, originalF)}
            i++;
        }
    }
    
    private final void wbc(BulkChunk c, MappedOutStream out) throws IOException {
        final ${mapType(t.getBaseType)}[] d = ((${storagePool(t.getBaseType)}) owner.basePool()).data();
        long result = 0L;
        ArrayList<Block> blocks = owner.blocks();
        int blockIndex = 0;
        final int endBlock = c.blockCount;
        while (blockIndex < endBlock) {
            Block b = blocks.get(blockIndex++);
            int i = (int) b.bpo;
            final int high = i + (int) b.count;
            while (i != high) {
                ${writeCode(t, originalF)}
                i++;
            }
        }"""
          }
        }
    }
"""
      }
    @Override
    public $fieldActualType getR(SkillObject ref) {
        ${
        if (f.isConstant()) s"return ${mapType(t)}.get${escaped(f.getName.capital)}();"
        else s"return ((${mapType(t)}) ref).get${escaped(f.getName.capital)}();"
      }
    }

    @Override
    public void setR(SkillObject ref, $fieldActualType value) {
        ${
        if (f.isConstant()) s"""throw new IllegalAccessError("${f.getName.camel} is a constant!");"""
        else s"((${mapType(t)}) ref).set${escaped(f.getName.capital)}(value);"
      }
    }

    @Override
    public $fieldActualTypeUnboxed get(${mapType(t)} ref) {
        ${
        if (f.isConstant()) s"return ${mapType(t)}.get${escaped(f.getName.capital)}();"
        else s"return ref.get${escaped(f.getName.capital)}();"
      }
    }

    @Override
    public void set(${mapType(t)} ref, $fieldActualTypeUnboxed value) {
        ${
        if (f.isConstant()) s"""throw new IllegalAccessError("${f.getName.camel} is a constant!");"""
        else s"ref.set${escaped(f.getName.capital)}(value);"
      }
    }
}
""")
      out.close()
    }
  }

  /**
   * creates code to read exactly one field element
   */
  private final def readField(t : Type, f : Field, fieldActualType : String) : String = {
    val accessData = s"((${mapType(t)}) d[i])"
    val setter = s".set${escaped(f.getName.capital)}("

    val read = f.getType match {
      case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒
        s"""($fieldActualType)target.getByID(in.v64()));"""

      case t : InterfaceType ⇒
        s"""($fieldActualType)type.readSingleField(in));"""

      case t : GroundType ⇒ t.getSkillName match {
        case "annotation" ⇒ s"""type.readSingleField(in));"""
        case "string"     ⇒ s"""sp.get(in.v64()));"""
        case _            ⇒ s"""in.${t.getSkillName}());"""
      }

      case t : UserType ⇒ s"""target.getByID(in.v64()));"""

      case _            ⇒ s"""type.readSingleField(in));"""
    }

    accessData + setter + read
  }

  /**
   * generates offset calculation code and a prelude with fields used in the
   * inner loop
   *
   * @return (prelude, code, isFast)
   */
  private final def offsetCode(t : UserType, f : Field, fieldType : Type, fieldActualType : String) : (String, String, Boolean) = {
    var prelude : String = ""

    val tIsBaseType = t.getSuperType() == null

    // casting access to data array using index i
    val dataAccessI = if (null == t.getSuperType) "d[i]" else s"((${mapType(t)})d[i])"
    val fieldAccess = s""".get${escaped(f.getName.capital)}()"""

    val code = fieldType match {

      // read next element
      case fieldType : GroundType ⇒ fieldType.getSkillName match {

        case "annotation" ⇒
          prelude = """
        final Annotation t = Annotation.cast(type);"""

          s"""${mapType(f.getType)} v = $dataAccessI$fieldAccess;
            if(null==v)
                result += 2;
            else
                result += t.singleOffset(v);"""

        case "string" ⇒
          prelude = """
        final StringType t = (StringType) type;"""

          s"""String v = $dataAccessI$fieldAccess;
            if(null==v)
                result++;
            else
                result += t.singleOffset(v);"""

        case "i8" | "bool" ⇒ return fastOffsetCode(0);

        case "i16"         ⇒ return fastOffsetCode(1);

        case "i32" | "f32" ⇒ return fastOffsetCode(2);

        case "i64" | "f64" ⇒ return fastOffsetCode(3);

        case "v64" ⇒ s"""long v = $dataAccessI$fieldAccess;

            if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
                result += 1;
            } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
                result += 2;
            } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
                result += 3;
            } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
                result += 4;
            } else if (0L == (v & 0xFFFFFFF800000000L)) {
                result += 5;
            } else if (0L == (v & 0xFFFFFC0000000000L)) {
                result += 6;
            } else if (0L == (v & 0xFFFE000000000000L)) {
                result += 7;
            } else if (0L == (v & 0xFF00000000000000L)) {
                result += 8;
            } else {
                result += 9;
            }"""
        case _ ⇒ s"""
        throw new NoSuchMethodError();"""
      }

      case fieldType : ConstantLengthArrayType ⇒
        prelude = s"""
        final SingleArgumentType<${
          mapType(fieldType)
        }, ${
          mapType(fieldType.getBaseType, true)
        }> t = (SingleArgumentType<${mapType(fieldType)}, ${mapType(fieldType.getBaseType, true)}>) type;

        final FieldType<${mapType(fieldType.getBaseType, true)}> baseType = t.groundType;"""

        s"""final ${mapVariantType(f.getType)} v = (${mapVariantType(f.getType)})$dataAccessI$fieldAccess;

            result += baseType.calculateOffset(($fieldActualType)v);"""

      case fieldType : SingleBaseTypeContainer ⇒
        prelude = s"""
        final SingleArgumentType<${
          mapType(fieldType)
        }, ${
          mapType(fieldType.getBaseType, true)
        }> t = (SingleArgumentType<${mapType(fieldType)}, ${mapType(fieldType.getBaseType, true)}>) type;

        final FieldType<${mapType(fieldType.getBaseType, true)}> baseType = t.groundType;"""

        s"""final ${mapVariantType(f.getType)} v = (${mapVariantType(f.getType)})$dataAccessI$fieldAccess;
            if(null==v || v.isEmpty())
                result++;
            else {
                result += V64.singleV64Offset(v.size());
                result += baseType.calculateOffset(($fieldActualType)v);
            }"""

      case fieldType : MapType ⇒
        prelude = s"""
        final MapType<?, ?> t = (MapType<?, ?>)(FieldType<?>) type;
        final FieldType keyType = t.keyType;
        final FieldType valueType = t.valueType;"""

        s"""final ${mapVariantType(f.getType)} v = castMap($dataAccessI$fieldAccess);
            if(null==v || v.isEmpty())
                result++;
            else {
                result += V64.singleV64Offset(v.size());
                result += keyType.calculateOffset(v.keySet());
                result += valueType.calculateOffset(v.values());
            }"""

      case fieldType : UserType ⇒
        s"""final ${mapType(f.getType)} instance = $dataAccessI$fieldAccess;
            if (null == instance) {
                result += 1;
                continue;
            }
            long v = instance.getSkillID();

            if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
                result += 1;
            } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
                result += 2;
            } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
                result += 3;
            } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
                result += 4;
            } else if (0L == (v & 0xFFFFFFF800000000L)) {
                result += 5;
            } else if (0L == (v & 0xFFFFFC0000000000L)) {
                result += 6;
            } else if (0L == (v & 0xFFFE000000000000L)) {
                result += 7;
            } else if (0L == (v & 0xFF00000000000000L)) {
                result += 8;
            } else {
                result += 9;
            }"""

      case fieldType : InterfaceType if fieldType.getSuperType.getSkillName != "annotation" ⇒
        s"""final ${mapType(fieldType)} instance = $dataAccessI$fieldAccess;
            if (null == instance) {
                result += 1;
                continue;
            }
            long v = instance.self().getSkillID();

            if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
                result += 1;
            } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
                result += 2;
            } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
                result += 3;
            } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
                result += 4;
            } else if (0L == (v & 0xFFFFFFF800000000L)) {
                result += 5;
            } else if (0L == (v & 0xFFFFFC0000000000L)) {
                result += 6;
            } else if (0L == (v & 0xFFFE000000000000L)) {
                result += 7;
            } else if (0L == (v & 0xFF00000000000000L)) {
                result += 8;
            } else {
                result += 9;
            }"""

      case fieldType : InterfaceType ⇒
        prelude = "final Annotation t = ((de.ust.skill.common.java.internal.UnrootedInterfacePool<?>)this.type).getType();"
        s"""${mapType(fieldType)} v = $dataAccessI$fieldAccess;

            if(null==v)
                result += 2;
            else
                result += t.singleOffset(v.self());"""

      case _ ⇒ s"""throw new NoSuchMethodError();"""
    }

    (prelude, code, false)
  }

  private final def fastOffsetCode(shift : Int) =
    (
      "",
      if (shift != 0) s"return last.count << $shift;"
      else "return last.count;",
      true
    )

  /**
   * creates code to write exactly one field element
   */
  private final def writeCode(t : UserType, f : Field) : String = {
    val dataAccessI = if (null == t.getSuperType) "d[i]" else s"((${mapType(t)})d[i])"
    val fieldAccess = s""".get${escaped(f.getName.capital)}()"""

    f.getType match {
      case t : GroundType ⇒ t.getSkillName match {
        case "annotation" | "string" ⇒ s"""type.writeSingleField($dataAccessI$fieldAccess, out);"""
        case _                       ⇒ s"""out.${t.getSkillName}($dataAccessI$fieldAccess);"""
      }

      case t : UserType ⇒ s"""${mapType(t)} v = $dataAccessI$fieldAccess;
            if (null == v)
                out.i8((byte) 0);
            else
                out.v64(v.getSkillID());"""
      case _ ⇒ s"""type.writeSingleField($dataAccessI$fieldAccess, out);"""
    }
  }
}
