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
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType

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
static final class $nameF extends ${
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
    protected final void rsc(int i, final int h, MappedInStream in) {
    }
    @Override
    protected final void osc(int i, final int h) {
    }
    @Override
    protected final void wsc(int i, final int h, MappedOutStream out) throws IOException {
    }
"""
          else s"""
    @Override
    protected final void rsc(int i, final int h, MappedInStream in) {
        ${readCode(t, originalF)}
    }
    @Override
    protected final void osc(int i, final int h) {${
            val (code, isFast) = offsetCode(t, f, originalF.getType, fieldActualType);
            if (isFast)
              code
            else s"""${prelude(originalF.getType)}
        final ${mapType(t.getBaseType)}[] d = ((${access(t.getBaseType)}) owner.basePool).data();
        long result = 0L;
        for (; i != h; i++) {
            $code
        }
        offset += result;"""
          }
    }
    @Override
    protected final void wsc(int i, final int h, MappedOutStream out) throws IOException {
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
   * create local variables holding type representants with correct type to help
   * the compiler
   */
  private final def prelude(t : Type, readHack : Boolean = false, target : String = "this.type") : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ s"""
        final Annotation t = (Annotation) $target;"""
      case "string" ⇒
        if (readHack) """
        final StringPool t = (StringPool) owner.owner().Strings();"""
        else """
        final StringType t = (StringType) type;"""

      case _ ⇒ ""
    }

    case t : ConstantLengthArrayType ⇒ s"""
        final ConstantLengthArray<${mapType(t.getBaseType, true)}> type = (ConstantLengthArray<${mapType(t.getBaseType, true)}>) this.type;
        final int size = type.length;${prelude(t.getBaseType, readHack, "type.groundType")}"""
    case t : VariableLengthArrayType ⇒ s"""
        final VariableLengthArray<${mapType(t.getBaseType, true)}> type = (VariableLengthArray<${mapType(t.getBaseType, true)}>) this.type;${prelude(t.getBaseType, readHack, "type.groundType")}"""
    case t : ListType ⇒ s"""
        final ListType<${mapType(t.getBaseType, true)}> type = (ListType<${mapType(t.getBaseType, true)}>) this.type;${prelude(t.getBaseType, readHack, "type.groundType")}"""
    case t : SetType ⇒ s"""
        final SetType<${mapType(t.getBaseType, true)}> type = (SetType<${mapType(t.getBaseType, true)}>) this.type;${prelude(t.getBaseType, readHack, "type.groundType")}"""
    case t : MapType ⇒
      locally {
        val mt = s"MapType<${mapType(t.getBaseTypes.head, true)}, ${t.getBaseTypes.tail.map(mapType(_, true)).reduceRight((k, v) ⇒ s"$MapTypeName<$k, $v>")}>"
        s"""
        final $mt type = ($mt) this.type;
        final FieldType keyType = type.keyType;
        final FieldType valueType = type.valueType;"""
      }

    case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"""
        final ${access(t.getSuperType)} t;
        if((FieldType<?>)$target instanceof ${access(t.getSuperType)})
            t = (${access(t.getSuperType)})
                FieldDeclaration.<${mapType(t.getSuperType)},${mapType(t)}>cast($target);
        else
            t = (${access(t.getSuperType)})((InterfacePool<?,?>)$target).superPool;"""

    case t : InterfaceType ⇒ s"""
        final Annotation t;
        // TODO we have to replace Annotation by the respective unrooted pool upon field creation to get rid of this distinction
        if ((FieldType<?>)$target instanceof Annotation)
            t = (Annotation) (FieldType<?>) $target;
        else
            t = ((UnrootedInterfacePool<?>) $target).getType();"""

    case t : UserType if readHack ⇒ s"""
        final ${access(t)} t = ((${access(t)}) $target);"""
    case _ ⇒ ""
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

    val pre = prelude(f.getType, true)

    val code = readCodeInner(f.getType)

    s"""$declareD$pre
        for (; i != h; i++) {${
      f.getType match {
        case t : ConstantLengthArrayType ⇒ s"""
            int s = size;
            final ${mapType(f.getType)} v = new ArrayList<>(size);
            while (s-- > 0) {
                v.add($code);
            }
            $fieldAccess = v;"""
        case t : VariableLengthArrayType ⇒ s"""
            int size = in.v32();
            final ${mapType(f.getType)} v = new ArrayList<>(size);
            while (size-- > 0) {
                v.add($code);
            }
            $fieldAccess = v;"""
        case t : ListType ⇒ s"""
            int size = in.v32();
            final ${mapType(f.getType)} v = new LinkedList<>();
            while (size-- > 0) {
                v.add($code);
            }
            $fieldAccess = v;"""
        case t : SetType ⇒ s"""
            int size = in.v32();
            final ${mapType(f.getType)} v = new HashSet<>(size * 3 / 2);
            while (size-- > 0) {
                v.add($code);
            }
            $fieldAccess = v;"""
        case _ ⇒ s"""
            $fieldAccess = $code;"""
      }
    }
        }
"""
  }

  private final def readCodeInner(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ "t.readSingleField(in)"
      case "string"     ⇒ "t.get(in.v32())"
      case _            ⇒ s"""in.${t.getSkillName}()"""
    }
    case t : SingleBaseTypeContainer ⇒ readCodeInner(t.getBaseType)

    case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"(${mapType(t)}) t.getByID(in.v32())"
    case t : InterfaceType ⇒ s"(${mapType(t)}) t.readSingleField(in)"

    case t : UserType ⇒ "t.getByID(in.v32())"
    case _ ⇒ "type.readSingleField(in)"
  }

  /**
   * generates offset calculation code and a prelude with fields used in the
   * inner loop
   *
   * @return (prelude, code, isFast)
   */
  private final def offsetCode(t : UserType, f : Field, fieldType : Type, fieldActualType : String) : (String, Boolean) = {

    val fieldAccess = (
      if (null == t.getSuperType) "d[i]"
      else s"((${mapType(t)})d[i])") + s".${name(f)}"

    val code = fieldType match {

      case fieldType : GroundType ⇒ fieldType.getSkillName match {

        case "annotation" ⇒ s"""${mapType(f.getType)} v = $fieldAccess;
            if(null==v)
                result += 2;
            else
                result += t.singleOffset(v);"""

        case "string" ⇒ s"""String v = $fieldAccess;
            if(null==v)
                result++;
            else
                result += t.singleOffset(v);"""

        case "i8" | "bool" ⇒ return fastOffsetCode(0);

        case "i16"         ⇒ return fastOffsetCode(1);

        case "i32" | "f32" ⇒ return fastOffsetCode(2);

        case "i64" | "f64" ⇒ return fastOffsetCode(3);

        case "v64"         ⇒ s"""result += V64.singleV64Offset($fieldAccess);"""
        case _ ⇒ s"""
        throw new NoSuchMethodError();"""
      }

      case fieldType : ConstantLengthArrayType ⇒ s"""final ${mapType(f.getType)} v = (${mapType(f.getType)})(${mapVariantType(f.getType)})$fieldAccess;

            ${offsetCodeInner(fieldType.getBaseType, "v")}"""

      case fieldType : SingleBaseTypeContainer ⇒ s"""final ${mapType(f.getType)} v = (${mapType(f.getType)})(${mapVariantType(f.getType)})$fieldAccess;
            int size = null == v ? 0 : v.size();
            if (0 == size)
                result++;
            else {
                result += V64.singleV64Offset(size);
                ${offsetCodeInner(fieldType.getBaseType, "v")}
            }"""

      case fieldType : MapType ⇒ s"""final ${mapVariantType(f.getType)} v = castMap($fieldAccess);
            if(null==v || v.isEmpty())
                result++;
            else {
                result += V64.singleV64Offset(v.size());
                result += keyType.calculateOffset(v.keySet());
                result += valueType.calculateOffset(v.values());
            }"""

      case fieldType : UserType ⇒ s"""final ${mapType(f.getType)} instance = $fieldAccess;
            if (null == instance) {
                result += 1;
                continue;
            }
            result += V64.singleV64Offset(instance.getSkillID());"""

      case fieldType : InterfaceType if fieldType.getSuperType.getSkillName != "annotation" ⇒
        s"""final ${mapType(fieldType)} instance = $fieldAccess;
            if (null == instance) {
                result += 1;
                continue;
            }
            result += V64.singleV64Offset(((SkillObject) instance).getSkillID());"""

      case fieldType : InterfaceType ⇒ s"""${mapType(fieldType)} v = $fieldAccess;

            if(null==v)
                result += 2;
            else
                result += t.singleOffset((SkillObject)v);"""

      case _ ⇒ s"""throw new NoSuchMethodError();"""
    }

    (code, false)
  }

  private final def offsetCodeInner(t : Type, target : String) : String = t match {
    case fieldType : GroundType ⇒ fieldType.getSkillName match {
      case "i8" | "bool" ⇒ return "result += size;";

      case "i16"         ⇒ return "result += (size<<1);";

      case "i32" | "f32" ⇒ return "result += (size<<2);";

      case "i64" | "f64" ⇒ return "result += (size<<3);";

      case "v64" ⇒ s"""for(long x : $target)
                    result += V64.singleV64Offset(x);"""

      case _ ⇒ s"""result += type.groundType.calculateOffset($target);"""
    }
    case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"""for(${mapType(t.getSuperType)} x : $target)
                    result += null==x?1:V64.singleV64Offset(x.getSkillID());"""

    case t : UserType ⇒ s"""for(${mapType(t)} x : $target)
                    result += null==x?1:V64.singleV64Offset(x.getSkillID());"""

    case _ ⇒ s"""result += type.groundType.calculateOffset($target);"""
  }

  private final def fastOffsetCode(shift : Int) =
    (
      if (shift != 0) s"offset += (h-i) << $shift;"
      else "offset += (h-i);",
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

    val pre = prelude(f.getType)

    val code = writeCode(f.getType, fieldAccess)

    s"""$declareD$pre
        for (; i != h; i++) {
            $code;
        }
"""
  }

  private final def writeCode(t : Type, fieldAccess : String) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ s"t.writeSingleField($fieldAccess, out)"
      case "string"     ⇒ s"t.writeSingleField($fieldAccess, out)"
      case _            ⇒ s"""out.${t.getSkillName}($fieldAccess)"""
    }

    case t : ConstantLengthArrayType ⇒ s"""
        final ${mapType(t)} x = $fieldAccess;
        for (${mapType(t.getBaseType)} e : x){
            ${writeCode(t.getBaseType, "e")};
        }"""

    case t : SingleBaseTypeContainer ⇒ s"""
        final ${mapType(t)} x = $fieldAccess;
        final int size = null == x ? 0 : x.size();
        if (0 == size) {
            out.i8((byte) 0);
        } else {
            out.v64(size);
            for (${mapType(t.getBaseType)} e : x){
                ${writeCode(t.getBaseType, "e")};
            }
        }"""

    case t : InterfaceType if t.getSuperType.getSkillName != "annotation" ⇒ s"""SkillObject v = (SkillObject)$fieldAccess;
            if(null == v)
                out.i8((byte)0);
            else
                out.v64(v.getSkillID())"""

    case t : InterfaceType ⇒ s"t.writeSingleField((SkillObject)$fieldAccess, out)"

    case t : UserType ⇒ s"""${mapType(t)} v = $fieldAccess;
            if(null == v)
                out.i8((byte)0);
            else
                out.v64(v.getSkillID())"""
    case _ ⇒ s"type.writeSingleField($fieldAccess, out)"
  }
}
