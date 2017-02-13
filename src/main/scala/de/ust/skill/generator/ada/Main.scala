/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import java.util.Date

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.MutableList

import de.ust.skill.generator.ada.api.APIBodyMaker
import de.ust.skill.generator.ada.api.APISpecMaker
import de.ust.skill.generator.ada.api.internal.InternalStringsMaker
import de.ust.skill.generator.ada.api.internal.PoolsMaker
import de.ust.skill.generator.ada.internal.InternalMaker
import de.ust.skill.generator.ada.internal.KnownFieldsMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.ContainerType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.FieldLike
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.generator.common.HeaderInfo

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * A generator turns a set of skill declarations into an Ada interface providing means of manipulating skill files
 * containing instances of the respective UserTypes.
 *
 * @author Timm Felden, Dennis Przytarski
 */
class Main extends FakeMain
    with APIBodyMaker
    with APISpecMaker
    with InternalStringsMaker
    with InternalMaker
    with KnownFieldsMaker
    with PackageBodyMaker
    with PackageSpecMaker
    with PoolsMaker {

  // fix gnat bug
  lineLength = 79
  override def comment(d : Declaration) : String = d.getComment.format("", "   -- ", lineLength, "   ")
  override def comment(f : FieldLike) : String = f.getComment.format("", "   -- ", lineLength, "   ")
  
  override def packageDependentPathPostfix = ""

  /**
   * Translates the types into the skill type id's.
   */
  override protected def mapTypeToId(t : Type, f : Field) : String = t match {
    case t : GroundType ⇒
      if (f.isConstant()) {
        t.getName.lower match {
          case "i8"  ⇒ 0.toString
          case "i16" ⇒ 1.toString
          case "i32" ⇒ 2.toString
          case "i64" ⇒ 3.toString
          case "v64" ⇒ 4.toString
        }
      } else {
        t.getName.lower match {
          case "annotation" ⇒ 5.toString
          case "bool"       ⇒ 6.toString
          case "i8"         ⇒ 7.toString
          case "i16"        ⇒ 8.toString
          case "i32"        ⇒ 9.toString
          case "i64"        ⇒ 10.toString
          case "v64"        ⇒ 11.toString
          case "f32"        ⇒ 12.toString
          case "f64"        ⇒ 13.toString
          case "string"     ⇒ 14.toString
        }
      }

    case t : ConstantLengthArrayType ⇒ 15.toString
    case t : VariableLengthArrayType ⇒ 17.toString
    case t : ListType                ⇒ 18.toString
    case t : SetType                 ⇒ 19.toString
    case t : MapType                 ⇒ 20.toString

    case t : Declaration             ⇒ s"""Long (Types.Element (${name(t)}_Type_Skillname).id)"""
  }

  /**
   * Translates the types into Ada types.
   */
  override protected def mapType(t : Type) : String = "Standard." + (t match {
    case t : GroundType ⇒ t.getName.lower match {
      case "annotation" ⇒ "Skill.Types.Annotation"

      case "bool"       ⇒ "Boolean"

      case "i8"         ⇒ "Skill.Types.I8"
      case "i16"        ⇒ "Skill.Types.I16"
      case "i32"        ⇒ "Skill.Types.I32"
      case "i64"        ⇒ "Skill.Types.I64"
      case "v64"        ⇒ "Skill.Types.V64"

      case "f32"        ⇒ "Skill.Types.F32"
      case "f64"        ⇒ "Skill.Types.F64"

      case "string"     ⇒ "Skill.Types.String_Access"
    }

    case t : SetType                 ⇒ "Skill.Containers.Boxed_Set"
    case t : MapType                 ⇒ "Skill.Types.Boxed_Map"
    case t : SingleBaseTypeContainer ⇒ "Skill.Containers.Boxed_Array"

    case t : Declaration             ⇒ s"${PackagePrefix}.${name(t)}"
  })

  /**
   * creates call to right "boxed"-function
   */
  protected def boxCall(t : Type) : String = t match {
    case t : GroundType              ⇒ s"Standard.Skill.Field_Types.Builtin.${t.getName.capital}_Type_P.Boxed"
    case t : SetType                 ⇒ "Standard.Skill.Field_Types.Builtin.Set_Type_P.Boxed"
    case t : SingleBaseTypeContainer ⇒ "Standard.Skill.Field_Types.Builtin.Const_Arrays_P.Boxed"
    case t : MapType                 ⇒ "Standard.Skill.Field_Types.Builtin.Map_Type_P.Boxed"
    case t                           ⇒ s"$poolsPackage.${name(t)}_P.Boxed"
  }
  /**
   * creates call to right "unboxed"-function
   */
  protected def unboxCall(t : Type) : String = t match {
    case t : GroundType    ⇒ s"Standard.Skill.Field_Types.Builtin.${t.getName.capital}_Type_P.Unboxed"
    case t : ContainerType ⇒ fullTypePackage(t) + ".Unboxed"
    case t                 ⇒ s"$poolsPackage.${name(t)}_P.Unboxed"
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
    val fields = d.getAllFields.filterNot { f ⇒ f.isConstant || f.isIgnored }
    if (fields.isEmpty)
      ""
    else
      (for (f ← fields)
        yield s"${escapedLonely(f.getSkillName())} : ${mapType(f.getType)}"
      ).mkString("; ", "; ", "")
  }

  /**
   * Provides the package prefix.
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix : String = null
  override protected def PackagePrefix() : String = _PackagePrefix
  private var _PackagePrefix : String = null

  override def setPackage(names : List[String]) {
    if (!names.isEmpty) {
      _packagePrefix = names.map(_.toLowerCase).reduce(_ + "-" + _)
      _PackagePrefix = names.map(_.toLowerCase.capitalize).reduce(_ + "." + _)
      _poolsPackage = s"Skill.Types.Pools.${names.map(_.toLowerCase.capitalize).reduce(_ + "_" + _)}_Pools"
    }
  }

  private var _poolsPackage = "Skill.Types.Pools.SF_Pools"
  override protected def poolsPackage : String = _poolsPackage

  override def makeHeader(headerInfo : HeaderInfo) : String = {
    // create header from options
    val headerLineLength = 51
    val headerLine1 = Some((headerInfo.line1 match {
      case Some(s) ⇒ s
      case None    ⇒ headerInfo.license.map("LICENSE: " + _).getOrElse("Your SKilL Scala Binding")
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    val headerLine2 = Some((headerInfo.line2 match {
      case Some(s) ⇒ s
      case None ⇒ "generated: " + (headerInfo.date match {
        case Some(s) ⇒ s
        case None    ⇒ (new java.text.SimpleDateFormat("dd.MM.yyyy")).format(new Date)
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    val headerLine3 = Some((headerInfo.line3 match {
      case Some(s) ⇒ s
      case None ⇒ "by: " + (headerInfo.userName match {
        case Some(s) ⇒ s
        case None    ⇒ System.getProperty("user.name")
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))

    s"""--  ___ _  ___ _ _                                                            --
-- / __| |/ (_) | |       ${headerLine1.get} --
-- \\__ \\ ' <| | | |__     ${headerLine2.get} --
-- |___/_|\\_\\_|_|____|    ${headerLine3.get} --
--                                                                            --
pragma Ada_2012;
"""
  }

  override def setOption(option : String, value : String) : Unit = option match {
    case "visitors" ⇒ createVisitors = ("true".equals(value));
    case unknown    ⇒ sys.error(s"unkown Argument: $unknown")
  }
  override def helpText = """
visitors          true/false  if set to true, the a visitor for each base type will be generated
"""

  override def customFieldManual = """
!with string+    Argument strings are added to the head of the generated file and each included with a with."""

  override protected def defaultValue(f : Field) = {
    def defaultValue(t : Type) = t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0"
      case "bool"                               ⇒ "False"
      case _                                    ⇒ "null"
    }
    f.getType match {
      case t : GroundType    ⇒ defaultValue(t)
      case t : ContainerType ⇒ s"${fullTypePackage(t)}.Make"
      case _                 ⇒ "null"
    }
  }

  /**
   * escape keywords and reserved words as well
   */
  override final def escapedLonely(target : String) : String = target.toLowerCase match {
    //     keywords and reserved words get a suffix "_2"
    case "abort" | "else" | "new" | "return" | "abs" | "elsif" | "not" | "reverse" | "abstract" | "end" | "null" |
      "accept" | "entry" | "select" | "access" | "exception" | "of" | "separate" | "aliased" | "exit" | "or" |
      "some" | "all" | "others" | "subtype" | "and" | "for" | "out" | "synchronized" | "array" | "function" |
      "overriding" | "at" | "tagged" | "generic" | "package" | "task" | "begin" | "goto" | "pragma" | "terminate" |
      "body" | "private" | "then" | "if" | "procedure" | "type" | "case" | "in" | "protected" | "constant" |
      "interface" | "until" | "is" | "raise" | "use" | "declare" | "range" | "delay" | "limited" | "record" |
      "when" | "delta" | "loop" | "rem" | "while" | "digits" | "renames" | "with" | "do" | "mod" | "requeue" |
      "xor" | "boolean" ⇒ return target + "_2"
    case _ ⇒ escaped(target)
  }

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  final def escaped(target : String) : String = target.map {
    case ':'                                       ⇒ "_0"
    case c if Character.isUnicodeIdentifierPart(c) ⇒ c.toString
    case c                                         ⇒ "ZZ" + c.toHexString
  }.reduce(_ + _)
}
