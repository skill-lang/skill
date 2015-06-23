/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import de.ust.skill.generator.ada.api._
import de.ust.skill.generator.ada.api.internal.PackageInternalSpecMaker
import de.ust.skill.generator.ada.internal._
import de.ust.skill.ir._
import de.ust.skill.parser.Parser
import java.io.File
import java.util.Date
import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList
import de.ust.skill.generator.common.Generator

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
    with PackageBodyMaker
    with PackageInternalSpecMaker
    with PackageSpecMaker
    with SkillBodyMaker
    with SkillSpecMaker
    with ByteReaderBodyMaker
    with ByteReaderSpecMaker
    with ByteWriterBodyMaker
    with ByteWriterSpecMaker
    with FileReaderBodyMaker
    with FileReaderSpecMaker
    with FileWriterBodyMaker
    with FileWriterSpecMaker
    with StateMakerBodyMaker
    with StateMakerSpecMaker {

  // fix gnat bug
  lineLength = 79
  override def comment(d : Declaration) : String = d.getComment.format("", "   -- ", lineLength, "")
  override def comment(f : Field) : String = f.getComment.format("", "   -- ", lineLength, "")

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

    case t : Declaration             ⇒ s"""Long (Types.Element ("${t.getSkillName}").id)"""
  }

  /**
   * Translates the types into Ada types.
   */
  override protected def mapType(t : Type, d : Declaration, f : Field) : String = t match {
    case t : GroundType ⇒ t.getName.lower match {
      case "annotation" ⇒ "Skill_Type_Access"

      case "bool"       ⇒ "Boolean"

      case "i8"         ⇒ "i8"
      case "i16"        ⇒ "i16"
      case "i32"        ⇒ "i32"
      case "i64"        ⇒ "i64"
      case "v64"        ⇒ "v64"

      case "f32"        ⇒ "f32"
      case "f64"        ⇒ "f64"

      case "string"     ⇒ "String_Access"
    }

    case t : ConstantLengthArrayType ⇒ s"${name(d)}_${f.getSkillName.capitalize}_Array"
    case t : VariableLengthArrayType ⇒ s"${name(d)}_${f.getSkillName.capitalize}_Vector.Vector"
    case t : ListType                ⇒ s"${name(d)}_${f.getSkillName.capitalize}_List.List"
    case t : SetType                 ⇒ s"${name(d)}_${f.getSkillName.capitalize}_Set.Set"
    case t : MapType                 ⇒ s"${name(d)}_${f.getSkillName.capitalize}_Map.Map"

    case t : Declaration             ⇒ s"${name(t)}_Type_Access"
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
        yield s"${f.getSkillName()} : ${mapType(f.getType, f.getDeclaredIn, f)}"
      ).mkString("; ", "; ", "")
  }

  /**
   * Provides the package prefix.
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    if (!names.isEmpty) {

      if (names.size > 1)
        System.err.println(
          "The Ada package system does not support nested packages with the expected meaning, dropping prefixes...");

      _packagePrefix = names.last
    }
  }

  override private[ada] def header : String = _header
  private lazy val _header = {
    // create header from options
    val headerLineLength = 51
    val headerLine1 = Some((headerInfo.line1 match {
      case Some(s) ⇒ s
      case None    ⇒ headerInfo.license.map("LICENSE: "+_).getOrElse("Your SKilL Scala Binding")
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    val headerLine2 = Some((headerInfo.line2 match {
      case Some(s) ⇒ s
      case None ⇒ "generated: "+(headerInfo.date match {
        case Some(s) ⇒ s
        case None    ⇒ (new java.text.SimpleDateFormat("dd.MM.yyyy")).format(new Date)
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    val headerLine3 = Some((headerInfo.line3 match {
      case Some(s) ⇒ s
      case None ⇒ "by: "+(headerInfo.userName match {
        case Some(s) ⇒ s
        case None    ⇒ System.getProperty("user.name")
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))

    s"""--  ___ _  ___ _ _                                                            --
-- / __| |/ (_) | |       ${headerLine1.get} --
-- \\__ \\ ' <| | | |__     ${headerLine2.get} --
-- |___/_|\\_\\_|_|____|    ${headerLine3.get} --
--                                                                            --
"""
  }

  override def setOption(option : String, value : String) : Unit = option match {
    case unknown ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def printHelp : Unit = println("""
Opitions (ada):
  (none)
""")

  override protected def defaultValue(f : Field) = {
    def defaultValue(t : Type) = t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0"
      case "bool"                               ⇒ "False"
      case _                                    ⇒ "null"
    }
    f.getType match {
      case t : GroundType              ⇒ defaultValue(t)
      case t : ConstantLengthArrayType ⇒ s"(others => ${defaultValue(t.getBaseType())})"
      case t : VariableLengthArrayType ⇒ s"${mapType(t, f.getDeclaredIn, f).stripSuffix(".Vector")}.Empty_Vector"
      case t : ListType                ⇒ s"${mapType(t, f.getDeclaredIn, f).stripSuffix(".List")}.Empty_List"
      case t : SetType                 ⇒ s"${mapType(t, f.getDeclaredIn, f).stripSuffix(".Set")}.Empty_Set"
      case t : MapType                 ⇒ s"${mapType(t, f.getDeclaredIn, f).stripSuffix(".Map")}.Empty_Map"

      case _                           ⇒ "null"
    }
  }

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  protected def escaped(target : String) : String = target.toLowerCase match {
    // disabled escaping of keywords, because names do not appear alone anyway
    // keywords get a suffix "_2"
    //    case "abort" | "else" | "new" | "return" | "abs" | "elsif" | "not" | "reverse" | "abstract" | "end" | "null" |
    //      "accept" | "entry" | "select" | "access" | "exception" | "of" | "separate" | "aliased" | "exit" | "or" |
    //      "some" | "all" | "others" | "subtype" | "and" | "for" | "out" | "synchronized" | "array" | "function" |
    //      "overriding" | "at" | "tagged" | "generic" | "package" | "task" | "begin" | "goto" | "pragma" | "terminate" |
    //      "body" | "private" | "then" | "if" | "procedure" | "type" | "case" | "in" | "protected" | "constant" |
    //      "interface" | "until" | "is" | "raise" | "use" | "declare" | "range" | "delay" | "limited" | "record" |
    //      "when" | "delta" | "loop" | "rem" | "while" | "digits" | "renames" | "with" | "do" | "mod" | "requeue" |
    //      "xor" ⇒ return target+"_2"

    // replace ":"-characters by something that is legal in an identifier, but wont alias an Ada-style type-name
    case _ ⇒ target.replace(":", "_0")
  }
}
