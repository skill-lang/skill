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
          val autoFieldIndex: Map[Field, Int] = t.getFields.asScala.filter(_.isAuto()).zipWithIndex.toMap

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

              out.write(
                  s"""

class $nameF(${
                      if (f.isAuto) "AutoField"
                      else "KnownDataField"
                  }):
    \"\"\"
    ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
    \"\"\"
    def __init__(self, fType, owner):
        super($nameF, self).__init__(fType, "${f.getSkillName}", owner${
                      if (f.isAuto) ", " + -autoFieldIndex(f)
                      else ""
                  })
        ${
                      if (f.isAuto) "" // auto fields are always correctly typed
                      else
                          s"""
        if ${
                              originalF.getType match {
                                  case t: GroundType ⇒ s"self.fieldType().typeID() != ${typeID(f.getType) - (if (f.isConstant) 7 else 0)}"
                                  case t: InterfaceType ⇒
                                      if (t.getSuperType.getSkillName.equals("annotation")) "type.typeID() != 5"
                                  case t: UserType ⇒ s"""self.fieldType().typeID() != "${f.getType.getSkillName}""""
                                  case _ ⇒ "False:  # TODO type check!"
                              }
                          }:
            raise SkillException("Expected field type ${f.getType.toString} in ${t.getName.capital}.${f.getName.camel} but found {}".format(fType))"""
                  }

    ${
                      if (f.isAuto) ""
                      else if (f.isConstant)
                          """
    def _rsc(self, i, h, inStream): pass

    def _osc(self, i, h): pass

    def _wsc(self, i, h, outStream): pass
"""
                      else
                          s"""
    def _rsc(self, i, h, inStream):
        ${readCode(t, originalF)}

    def _osc(self, i, h):
        ${
        val (code, isFast) = offsetCode(t, f, originalF.getType, fieldActualType)
        if (isFast)code
          else
              s"""${prelude(originalF.getType)}
        d = self.owner.basePool.data()
        result = 0
        for i in range(i, h):
            $code
        self._offset += result"""
                          }

    def _wsc(self, i, h, out):
        ${writeCode(t, originalF)}
"""
                  }
""")
          }
      }
  }

  /**
   * create local variables holding type representants with correct type to help
   * the compiler
   */
  private final def prelude(t : Type, readHack : Boolean = false, target : String = "self.fieldType()") : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ s"""
        t = $target"""
      case "string" ⇒
        if (readHack) """
        t = self.owner.owner().Strings()"""
        else """
        t = self.fieldType()"""

      case _ ⇒ ""
    }

    case t : ConstantLengthArrayType ⇒ s"""
        fType = self.fieldType()
        size = len(fType)
        ${prelude(t.getBaseType, readHack, "fType.groundType")}"""
    case t : VariableLengthArrayType ⇒ s"""
        fType = self.fieldType()
        ${prelude(t.getBaseType, readHack, "fType.groundType")}"""
    case t : ListType ⇒ s"""
        fType = self.fieldType()
        ${prelude(t.getBaseType, readHack, "fType.groundType")}"""
    case t : SetType ⇒ s"""
        fType = self.fieldType()
        ${prelude(t.getBaseType, readHack, "fType.groundType")}"""
    case t : MapType ⇒
      locally {
        s"""
        fType = self.fieldType()"""
      }

    case t : InterfaceType if t.getSuperType.getSkillName == "annotation" ⇒ s"""
        if isinstance($target, Annotation):
            t = (Annotation) (FieldType<?>) $target"""

    case t : UserType if readHack ⇒ s"""
        t = $target"""
    case _ ⇒ ""
  }

  /**
   * creates code to read all field elements
   */
  private final def readCode(t : UserType, f : Field) : String = {
    val declareD = s"d = self.owner${
      if (null == t.getSuperType) ""
      else ".basePool"
    }.data()"
    val fieldAccess = "d[i]" + s".${name(f)}"

    val pre = prelude(f.getType, readHack = true)

    val code = readCodeInner(f.getType)

    s"""$declareD$pre
        for i in range(i, h):${
      f.getType match {
        case t : ConstantLengthArrayType ⇒ s"""
            v = []
            for _ in range(0,size):
                v.append($code)
            $fieldAccess = v"""
        case t : VariableLengthArrayType ⇒ s"""
            size = inStream.v64()
            v = []
            for k in range(0, size):
                v.append($code)
            $fieldAccess = v"""
        case t : ListType ⇒ s"""
            size = inStream.v64()
            v = []
            for k in range(0, size):
                v.append($code)
            $fieldAccess = v"""
        case t : SetType ⇒ s"""
            size = inStream.v64()
            v = set()
            t = self.fieldType().groundType
            for k in range(0, size):
                v.add($code)
            $fieldAccess = v"""
        case _ ⇒ s"""
            $fieldAccess = $code"""
      }
    }
"""
  }

  private final def readCodeInner(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ "t.readSingleField(inStream)"
      case "string"     ⇒ "t.get(inStream.v64())"
      case _            ⇒ s"""inStream.${t.getSkillName}()"""
    }
    case t : SingleBaseTypeContainer ⇒ readCodeInner(t.getBaseType)

    case t : InterfaceType if t.getSuperType.getSkillName == "annotation" ⇒ s"t.readSingleField(inStream)"

    case t : UserType ⇒ "t.getByID(inStream.v64())"
    case _ ⇒ "fType.readSingleField(inStream)"
  }

  /**
   * generates offset calculation code and a prelude with fields used in the
   * inner loop
   *
   * @return (prelude, code, isFast)
   */
  private final def offsetCode(t : UserType, f : Field, fieldType : Type, fieldActualType : String) : (String, Boolean) = {

    val fieldAccess = "d[i]" + s".${name(f)}"

    val code = fieldType match {

      case fieldType : GroundType ⇒ fieldType.getSkillName match {

        case "annotation" ⇒ s"""v = $fieldAccess
            if v is None:
                result += 2
            else:
                result += self.fieldType().singleOffset(v)"""

        case "string" ⇒ s"""v = $fieldAccess
            if v is None:
                result += 1
            else:
                result += self.fieldType().singleOffset(v)"""

        case "i8" | "bool" ⇒ return fastOffsetCode(0);

        case "i16"         ⇒ return fastOffsetCode(1);

        case "i32" | "f32" ⇒ return fastOffsetCode(2);

        case "i64" | "f64" ⇒ return fastOffsetCode(3);

        case "v64"         ⇒ s"""result += V64.singleV64Offset($fieldAccess)"""
        case _ ⇒ s"""
        raise AttributeError()"""
      }

      case fieldType : ConstantLengthArrayType ⇒ s"""v = $fieldAccess
            if len(v) != len(self.fieldType()):
                raise Exception("constant length array has wrong size")

            ${offsetCodeInner(fieldType.getBaseType, "v")}"""

      case fieldType : SingleBaseTypeContainer ⇒ s"""v = $fieldAccess
            if v is None:
                size = 0
            else:
                size = len(v)
            if 0 == size:
                result += 1
            else:
                result += V64.singleV64Offset(size)
                ${offsetCodeInner(fieldType.getBaseType, "v")}
            """

      case fieldType : MapType ⇒ s"""v = $fieldAccess
            if v is None or len(v) == 0:
                result += 1
            else:
                result += V64.singleV64Offset(len(v))
                result += fType.keyType.calculateOffset(v.keySet())
                result += fType.valueType.calculateOffset(v.values())
            """

      case fieldType : UserType ⇒ s"""instance = $fieldAccess
            if instance is None:
                result += 1
                continue
            result += V64.singleV64Offset(instance.getSkillID())"""

      case _ ⇒ s"""raise AttributeError()"""
    }

    (code, false)
  }

  private final def offsetCodeInner(t : Type, target : String) : String = t match {
    case fieldType : GroundType ⇒ fieldType.getSkillName match {
      case "i8" | "bool" ⇒ return "result += size";

      case "i16"         ⇒ return "result += (size<<1)";

      case "i32" | "f32" ⇒ return "result += (size<<2)";

      case "i64" | "f64" ⇒ return "result += (size<<3)";

      case "v64" ⇒ s"""for x in $target:
                    result += V64.singleV64Offset(x)"""

      case _ ⇒ s"""result += self.fieldType().groundType.calculateOffset($target)"""
    }

    case t : UserType ⇒ s"""for x in $target:
                    if x is None:
                        result += 1
                    else:
                        result += V64.singleV64Offset(x.getSkillID())"""

    case _ ⇒ s"""result += self.fieldType().groundType.calculateOffset($target)"""
  }

  private final def fastOffsetCode(shift : Int) =
    (
      if (shift != 0) s"self._offset += (h-i) << $shift"
      else "self._offset += (h-i)",
      true)

  /**
   * creates code to write exactly one field element
   */
  private final def writeCode(t : UserType, f : Field) : String = {
    val declareD = s"d = self.owner${
      if (null == t.getSuperType) ""
      else ".basePool"
    }.data()"
    val fieldAccess = "d[i]" + s".${name(f)}"

    val pre = prelude(f.getType)

    val code = writeCode(f.getType, fieldAccess)

    s"""$declareD$pre
        for i in range(i, h):
            $code
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
            x = $fieldAccess
            size = 0 if x is None else len(x)
            if size == 0:
                out.i8(0)
            else:
                out.v64(size)
                for e in x:
                    ${writeCode(t.getBaseType, "e")}

        """

    case t : InterfaceType if t.getSuperType.getSkillName == "annotation" ⇒ s"t.writeSingleField($fieldAccess, out)"

    case t : UserType ⇒ s"""v = $fieldAccess
            if v is None:
                out.i8(0)
            else:
                out.v64(v.getSkillID())"""
    case _ ⇒ s"self.fieldType().writeSingleField($fieldAccess, out)"
  }
}
