/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import de.ust.skill.io.PrintWriter
import de.ust.skill.ir._

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

trait FieldDeclarationMaker extends GeneralOutputMaker {
  final def makeFields(out : PrintWriter) {

    // project IR, so that reflection implements the binary file format correctly
    val IR = this.types.removeSpecialDeclarations().getUsertypes

    for (t ← IR.asScala) {
      val autoFieldIndex : Map[Field, Int] = t.getFields.asScala.filter(_.isAuto()).zipWithIndex.toMap

      for (f ← t.getFields.asScala) {
        // the field before interface projection
        val originalF = this.types.removeTypedefs().removeEnums().get(t.getSkillName).asInstanceOf[UserType]
          .getAllFields.asScala.find(_.getName == f.getName).get

        // the type before the interface projection
        val fieldActualType = mapType(originalF.getType)

        val tIsBaseType = t.getSuperType == null

        val nameT = mapType(t)
        val nameF = knownField(f)

        // casting access to data array using index i
        val declareD = s"d: [] = owner.basePool.data"
        val fieldAccess = "d[i]"

        out.write(s"""
class $nameF(${
          if (f.isAuto) "AutoField"
          else "KnownDataField"
        }):
    \"\"\"
    ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
    \"\"\"
    def __init__(self, fType, owner):
        super(f0, self).__init__(fType, "${f.getSkillName}", owner${
            if (f.isAuto) ", " + -autoFieldIndex(f)
            else ""
        })
        ${
          if (f.isAuto) "" // auto fields are always correctly typed
          else s"""
        if ${
            originalF.getType match {
              case t : GroundType ⇒ s"fType.typeID != ${typeID(f.getType) - (if (f.isConstant) 7 else 0)}"
              case t : InterfaceType ⇒
                if (t.getSuperType.getSkillName.equals("annotation")) "type.typeID != 5"
              case t : UserType ⇒ s"""!fType.name == "${f.getType.getSkillName}""""
              case _            ⇒ "false)  # TODO type check!"
            }
          }:
            raise SkillException("Expected field type ${f.getType.toString} in ${t.getName.capital}.${f.getName.camel} but found " + fType)"""
        }

    def get(self, ref):
        ${
          if (f.isConstant) s"return ${mapType(t)}.get${escaped(f.getName.capital)}()"
          else s"return ref.${name(f)}"
        }

    def set(self, ref, value):
        ${
          if (f.isConstant) s"""raise Exception("${f.getName.camel} is a constant!")"""
          else s"ref.${name(f)} = value"
        }""")
      }
    }
  }

  /**
   * create local variables holding type representants with correct type to help
   * the compiler
   */
  private final def prelude(t : Type, readHack : Boolean = false, target : String = "self.fType") : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ s"""
        t = $target"""
      case "string" ⇒
        if (readHack) """
        t = owner.owner.strings"""
        else """
        t = fType"""

      case _ ⇒ ""
    }

    case t : ConstantLengthArrayType ⇒ s"""
        fType = self.fType
        size = len(fType)"""
    case t : VariableLengthArrayType ⇒ s"""
        fType = self.fType"""
    case t : ListType ⇒ s"""
        fType = self.fType"""
    case t : SetType ⇒ s"""
        fType = self.fType"""
    case t : MapType ⇒
      locally {
        s"""
        fType = self.fType"""
      }

    case t : InterfaceType if t.getSuperType.getSkillName == "annotation" ⇒ s"""
        if isinstance($target, Annotation):
            t = (Annotation) (FieldType<?>) $target;"""

    case t : UserType if readHack ⇒ s"""
        t = $target"""
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

    val pre = prelude(f.getType, readHack = true)

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
      case "annotation" ⇒ "t.readSingleField(inStream)"
      case "string"     ⇒ "t.get(inStream.v32())"
      case _            ⇒ s"""inStream.${t.getSkillName}()"""
    }
    case t : SingleBaseTypeContainer ⇒ readCodeInner(t.getBaseType)

    case t : InterfaceType if t.getSuperType.getSkillName == "annotation" ⇒ s"t.readSingleField(inStream)"

    case t : UserType ⇒ "t.getByID(inStream.v32())"
    case _ ⇒ "fType.readSingleField(in)"
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

      case fieldType : ConstantLengthArrayType ⇒ s"""v = $fieldAccess;
            
            if (v.size() != type.length)
                throw new IllegalArgumentException("constant length array has wrong size");

            ${offsetCodeInner(fieldType.getBaseType, "v")}"""

      case fieldType : SingleBaseTypeContainer ⇒ s"""v = $fieldAccess
            int size = null == v ? 0 : v.size();
            if (0 == size)
                result++;
            else {
                result += V64.singleV64Offset(size);
                ${offsetCodeInner(fieldType.getBaseType, "v")}
            }"""

      case fieldType : MapType ⇒ s"""v = $fieldAccess
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
      true)

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
        x = $fieldAccess
        for e in x:
            ${writeCode(t.getBaseType, "e")}
        """

    case t : SingleBaseTypeContainer ⇒ s"""
        x = $fieldAccess;
        size = 0 if x is None else len(x)
        if size == 0:
            out.i8(0);
        else:
            out.v64(size);
            for e in x:
                ${writeCode(t.getBaseType, "e")}

        """

    case t : InterfaceType if t.getSuperType.getSkillName == "annotation" ⇒ s"t.writeSingleField($fieldAccess, out)"

    case t : UserType ⇒ s"""v = $fieldAccess;
            if v is None:
                out.i8(0);
            else
                out.v64(v.getSkillID())"""
    case _ ⇒ s"fType.writeSingleField($fieldAccess, out)"
  }
}
