/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.haskell



import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.MutableList

import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.FieldLike
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.main.HeaderInfo

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * Port of the original Java implementation of Rafael Harths Haskell back-end.
 *
 * @author Timm Felden
 */
final class Main extends FakeMain
    with AccessMaker
    with DependenciesMaker
    with FollowMaker
    with InterfaceMaker
    with TypesMaker {

  override def make {
    super.make
  }

  lineLength = 80
  override def comment(d : Declaration) : String = d.getComment.format("", "-- ", lineLength, "")
  override def comment(f : FieldLike) : String = f.getComment.format("", "-- ", lineLength, "")

  override def packageDependentPathPostfix = ""
  override def defaultCleanMode = "file";

  /**
   * Translates the types to Haskell types.
   */
  override protected def mapType(t : Type, followReferences : Boolean) : String = t match {
    case t : GroundType ⇒ t.getName.lower match {
      case "annotation" ⇒
        if (followReferences) "Maybe Pointer"
        else "Ref"

      case "bool"   ⇒ "Bool"

      case "i8"     ⇒ "Int8"
      case "i16"    ⇒ "Int16"
      case "i32"    ⇒ "Int32"
      case "i64"    ⇒ "Int64"
      case "v64"    ⇒ "Int64"

      case "f32"    ⇒ "Float"
      case "f64"    ⇒ "Double"

      case "string" ⇒ "String"
    }

    case t : ConstantLengthArrayType ⇒ s"[${mapType(t.getBaseType, followReferences)}]"
    case t : VariableLengthArrayType ⇒ s"[${mapType(t.getBaseType, followReferences)}]"
    case t : ListType                ⇒ s"[${mapType(t.getBaseType, followReferences)}]"
    case t : SetType                 ⇒ s"[${mapType(t.getBaseType, followReferences)}]"
    case t : MapType ⇒ t.getBaseTypes.map(t ⇒ mapType(t, followReferences)).reduceRight[String] {
      case (k, v) ⇒ s"M.Map $k ($v)"
    }

    case t : Declaration ⇒
      if (followReferences) "Maybe " + name(t)
      else "Ref"
  }

  override protected def BoxedDataConstructor(t : Type) : String = t.getSkillName match {
    case "bool"   ⇒ "GBool"
    case "string" ⇒ "GString"
    case "i8"     ⇒ "GInt8"
    case "i16"    ⇒ "GInt16"
    case "i32"    ⇒ "GInt32"
    case "i64"    ⇒ "GInt64"
    case "v64"    ⇒ "GV64"
    case "f32"    ⇒ "GFloat"
    case "f64"    ⇒ "GDouble"
    case _        ⇒ "GRef"
  }

  /**
   * Gets all super types of a given type.
   */
  protected def getSuperTypes(d : UserType) : MutableList[Type] = {
    if (null == d.getSuperType) MutableList[Type]()
    else getSuperTypes(d.getSuperType) += d.getSuperType
  }

  /**
   * Gets all sub types of a given type.
   */
  protected def getSubTypes(d : UserType) : MutableList[Type] = {
    var rval = MutableList[Type]()

    IR.reverse.foreach { _d ⇒
      if (d == _d.getSuperType && -1 == rval.indexOf(_d)) {
        rval += _d
        rval ++= getSubTypes(_d)
      }
    }

    rval.distinct
  }

  /**
   * Gets the fields as parameters of a given type.
   */
  def printParameters(d : UserType) : String = {
    var output = "";
    var hasFields = false;
    output += d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map({ f ⇒
      hasFields = true;
      s"${f.getSkillName()} : ${mapType(f.getType, ???)}"
    }).mkString("; ", "; ", "")
    if (hasFields) output else ""
  }

  /**
   * Provides the package prefix.
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    if (!names.isEmpty)
      _packagePrefix = names.map(_.toLowerCase).mkString(".");
  }

  override def makeHeader(headerInfo : HeaderInfo) : String = headerInfo.format(this, "--", "", "--", "", "--", "")

  override def setOption(option : String, value : String) : Unit = option.toLowerCase match {
    case "unsafe" ⇒ unsafe = value == "true"
    case unknown  ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def helpText : String = """
  unsafe                 remove all generated runtime type checks, if set to "true"
"""

  override def customFieldManual : String = "not supported"

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  final def escaped(target : String) : String = target match {
    case "auto" | "_Bool" | "break" | "case" | "char" | "_Complex" | "const" | "continue" | "default" | "do" | "double"
      | "else" | "enum" | "extern" | "float" | "for" | "goto" | "if" | "_Imaginary" | "inline" | "int" | "long" |
      "register" | "restrict" | "return" | "short" | "signed" | "sizeof" | "static" | "struct" | "switch" | "typedef"
      | "union" | "unsigned" | "void" | "volatile" | "while" ⇒ "ZZ_" + target

    // the string is fine anyway
    case _ ⇒ target
  }

  override protected def makeConstructorArguments(t : UserType) : String = (for (f ← t.getAllFields; if !f.isConstant())
    yield s""", ${mapType(f.getType, ???)} _${name(f)}""").mkString

  override protected def defaultValue(f : Field) = f.getType match {
    case t : GroundType ⇒ t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0f"
      case "bool"                               ⇒ "false"
      case _                                    ⇒ "null"
    }

    // TODO compound types would behave more nicely if they would be initialized with empty collections instead of null
    // @note some collections use 0 as empty (e.g. list)

    case _ ⇒ "0"
  }
}
