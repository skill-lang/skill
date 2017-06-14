/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ArrayBuffer

import de.ust.skill.generator.java.GeneralOutputMaker
import de.ust.skill.io.PrintWriter
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.UserType
import de.ust.skill.ir.Type
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.MapType

trait FieldDeclarationMaker extends GeneralOutputMaker {
  final def makeFields(out : PrintWriter) {

    // project IR, so that reflection implements the binary file format correctly
    val IR = this.types.removeSpecialDeclarations.getUsertypes

    for (t ← IR) {
      val autoFieldIndex : Map[Field, Int] = t.getFields.filter(_.isAuto()).zipWithIndex.toMap

      for (f ← t.getFields) {
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
        val declareD = s"final ${mapType(t.getBaseType)}[] d = ((${access(t.getBaseType)}) owner.basePool()).data();"
        val fieldAccess = (
          if (null == t.getSuperType) "d[i]"
          else s"((${mapType(t)})d[i])") + s".${name(f)}"

        out.write(s"""
/**
 * ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
 */
${
          suppressWarnings
        }static final class $nameF extends ${
          if (f.isAuto) "AutoField"
          else "KnownDataField"
        }<$fieldActualType, ${mapType(t)}>${
          var interfaces = new ArrayBuffer[String]()

          // mark ignored fields as ignored; read function is inherited
          if (f.isIgnored()) interfaces += "IgnoredField"

          // mark interface fields
          if (f.getType.isInstanceOf[InterfaceType]) interfaces += "InterfaceField"

          if (interfaces.isEmpty) ""
          else interfaces.mkString(" implements ", ", ", "")
        } {

    public $nameF(FieldType<$fieldActualType> type, ${access(t)} owner) {
        super(type, "${f.getSkillName}"${
          if (f.isAuto()) ", " + -autoFieldIndex(f)
          else ""
        }, owner);
            // TODO insert known restrictions?
    }
${
          if (f.isAuto) ""
          else if (f.isConstant) """
    @Override
    protected final void rsc(SimpleChunk c, MappedInStream in) {
    }
    @Override
    protected final long osc(SimpleChunk c) {
        return 0L;
    }
    @Override
    protected final void wsc(SimpleChunk c, MappedOutStream out) throws IOException {
    }
"""
          else s"""
    @Override
    protected final void rsc(SimpleChunk c, MappedInStream in) {
        ${readCode(t, originalF)}
    }
    @Override
    protected final long osc(SimpleChunk c) {${
            val (pre, code, isFast) = offsetCode(t, f, originalF.getType, fieldActualType);
            if (isFast)
              code
            else s"""$pre
        final ${mapType(t.getBaseType)}[] d = ((${access(t.getBaseType)}) owner.basePool).data();
        long result = 0L;
        int i = (int) c.bpo;
        for (final int h = i + (int) c.count; i != h; i++) {
            $code
        }
        return result;"""
          }
    }
    @Override
    protected final void wsc(SimpleChunk c, MappedOutStream out) throws IOException {
        ${writeCode(t, originalF)}
    }
"""
        }
    @Override
    public $fieldActualType get(SkillObject ref) {
        ${
          if (f.isConstant()) s"return ${mapType(t)}.get${escaped(f.getName.capital)}();"
          else s"return ((${mapType(t)}) ref).${name(f)};"
        }
    }

    @Override
    public void set(SkillObject ref, $fieldActualType value) {
        ${
          if (f.isConstant()) s"""throw new IllegalAccessError("${f.getName.camel} is a constant!");"""
          else s"((${mapType(t)}) ref).${name(f)} = value;"
        }
    }
}
""")
      }
    }
  }

  /**
   * creates code to read all field elements
   */
  private final def readCode(t : UserType, f : Field) : String = {
    val declareD = s"final ${mapType(t.getBaseType)}[] d = ((${access(t.getBaseType)}) owner${
      if (null == t.getSuperType) ""
      else ".basePool"
    }).data();"
    val fieldAccess = (
      if (null == t.getSuperType) "d[i]"
      else s"((${mapType(t)})d[i])") + s".${name(f)}"

    val pre = f.getType match {
      case t : GroundType ⇒ t.getSkillName match {
        case "annotation" ⇒ """
        final Annotation t = (Annotation) type;"""
        case "string" ⇒ """
        final StringPool t = (StringPool) owner.owner().Strings();"""
        case _ ⇒ ""
      }

      case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"""
        final ${access(t.getSuperType)} t = (${access(t.getSuperType)})
                FieldDeclaration.<${mapType(t.getSuperType)},${mapType(t)}>cast(this.type);"""

      case t : InterfaceType ⇒ """
        final Annotation t;
        // TODO we have to replace Annotation by the respective unrooted pool upon field creation to get rid of this distinction
        if ((FieldType<?>)type instanceof Annotation)
            t = (Annotation) (FieldType<?>) type;
        else
            t = ((UnrootedInterfacePool<?>) type).getType();"""

      case t : UserType ⇒ s"""
        final ${access(t)} t = ((${access(t)}) type);"""
      case _ ⇒ ""
    }

    val code = f.getType match {
      case t : GroundType ⇒ t.getSkillName match {
        case "annotation" ⇒ "t.readSingleField(in)"
        case "string"     ⇒ "t.get(in.v64())"
        case _            ⇒ s"""in.${t.getSkillName}()"""
      }

      case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"(${mapType(f.getType)}) t.getByID(in.v64())"

      case t : UserType ⇒ "t.getByID(in.v64())"
      case _ ⇒ "type.readSingleField(in)"
    }

    s"""$declareD$pre
        int i = (int) c.bpo;
        for (final int h = (int) (i + c.count); i != h; i++) {
            $fieldAccess = $code;
        }
"""
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
    val fieldAccess = s""".${name(f)}"""

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
      if (shift != 0) s"return c.count << $shift;"
      else "return c.count;",
      true
    )

  /**
   * creates code to write exactly one field element
   */
  private final def writeCode(t : UserType, f : Field) : String = {
    val declareD = s"final ${mapType(t.getBaseType)}[] d = ((${access(t.getBaseType)}) owner${
      if (null == t.getSuperType) ""
      else ".basePool"
    }).data();"
    val fieldAccess = (
      if (null == t.getSuperType) "d[i]"
      else s"((${mapType(t)})d[i])") + s".${name(f)}"

    val pre = f.getType match {
      case t : GroundType ⇒ t.getSkillName match {
        case "annotation" ⇒ """
        final Annotation t = (Annotation) type;"""
        case "string" ⇒ """
        final StringType t = (StringType) type;"""
        case _ ⇒ ""
      }

      case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ "" // TODO

      case t : InterfaceType ⇒ s"""
        final Annotation t;
        // TODO we have to replace Annotation by the respective unrooted pool upon field creation to get rid of this distinction
        if ((FieldType<?>)type instanceof Annotation)
            t = (Annotation) (FieldType<?>) type;
        else
            t = ((UnrootedInterfacePool<?>) type).getType();"""

      case t : UserType ⇒ s"""
        final ${access(t)} t = ((${access(t)}) type);"""
      case _ ⇒ ""
    }

    val code = f.getType match {
      case t : GroundType ⇒ t.getSkillName match {
        case "annotation" ⇒ s"t.writeSingleField($fieldAccess, out)"
        case "string"     ⇒ s"t.writeSingleField($fieldAccess, out)"
        case _            ⇒ s"""out.${t.getSkillName}($fieldAccess)"""
      }

      case t : UserType ⇒ s"""${mapType(f.getType)} v = $fieldAccess;
            if(null == v)
                out.i8((byte)0);
            else
                out.v64(v.getSkillID())"""
      case _ ⇒ s"type.writeSingleField($fieldAccess, out)"
    }

    s"""$declareD$pre
        int i = (int) c.bpo;
        for (final int h = (int) (i + c.count); i != h; i++) {
            $code;
        }
"""
  }
}
