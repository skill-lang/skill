/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.doxygen

import de.ust.skill.ir._
import de.ust.skill.parser.Parser
import java.io.File
import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList
import de.ust.skill.generator.common.Generator
import java.util.Date

/**
 * Entry point of the Doxygen generator.
 *
 * The used language is C++ (or rather something that doxygen recoginezes as C++).
 *
 * @note Using C++ has the effect, that neither interfaces nor enums with fields can be represented correctly.
 * @note None of the languages supported by doxygen seems to provide all required features. We may fix this by
 * switching to scaladoc or by hoping that doxygen will support scala or swift eventually.
 */
object Main {
  private def printHelp : Unit = println("""
usage:
  [options] skillPath outPath

Opitions:
  -p packageName      set a package name used by all emitted code.
  -h1|h2|h3 content   overrides the content of the respective header line
  -u userName         set a user name
  -date date          set a custom date
""")

  /**
   * Takes an argument skill file name and generates an Ada binding.
   */
  def main(args : Array[String]) : Unit = {
    var m = new Main

    //processing command line arguments
    if (2 > args.length) {
      printHelp
    } else {

      m.setOptions(args.slice(0, args.length - 2)).ensuring(m._packagePrefix != "", "You have to specify a non-empty package name!")
      val skillPath = args(args.length - 2)
      m.outPath = args(args.length - 1)

      //parse argument code
      m.setIR(Parser.process(new File(skillPath)).toList)

      // create output using maker chain
      m.make;
    }
  }
}

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
    with UserTypeMaker {

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

    case t : ConstantLengthArrayType ⇒ s"${d.getSkillName.capitalize}_${f.getSkillName.capitalize}_Array"
    case t : VariableLengthArrayType ⇒ s"${d.getSkillName.capitalize}_${f.getSkillName.capitalize}_Vector.Vector"
    case t : ListType                ⇒ s"${d.getSkillName.capitalize}_${f.getSkillName.capitalize}_List.List"
    case t : SetType                 ⇒ s"${d.getSkillName.capitalize}_${f.getSkillName.capitalize}_Set.Set"
    case t : MapType                 ⇒ s"${d.getSkillName.capitalize}_${f.getSkillName.capitalize}_Map.Map"

    case t : Declaration             ⇒ s"${escaped(t.getName.ada)}_Type_Access"
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

    1 to IR.length foreach { _ ⇒
      for (_d ← IR) {
        // element is sub type and not collected
        if (d == _d.getSuperType && -1 == rval.indexOf(_d)) rval += _d
        // element is listed sub type and not collected
        if (-1 < rval.indexOf(_d.getSuperType) && -1 == rval.indexOf(_d)) rval += _d
      }
    }

    rval
  }

  /**
   * Gets the fields as parameters of a given type.
   */
  def printParameters(d : UserType) : String = {
    var output = "";
    var hasFields = false;
    output += d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map({ f ⇒
      hasFields = true;
      s"${f.getSkillName()} : ${mapType(f.getType, d, f)}"
    }).mkString("; ", "; ", "")
    if (hasFields) output else ""
  }

  /**
   * Provides the package prefix.
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    _packagePrefix = names.foldRight("")(_+"."+_)
  }

  override private[doxygen] def header : String = _header
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

    s"""/*  ___ _  ___ _ _                                                            *\\
 * / __| |/ (_) | |       ${headerLine1.get} *
 * \\__ \\ ' <| | | |__     ${headerLine2.get} *
 * |___/_|\\_\\_|_|____|    ${headerLine3.get} *
\\*                                                                            */
"""
  }

  override def setOptions(args : Array[String]) {
    var index = 0

    while (index < args.length) args(index) match {
      case unknown ⇒ sys.error(s"unkown Argument: $unknown")
    }
  }

  // unused
  override protected def defaultValue(f : Field) = ???

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   * TODO not correct
   */
  protected def escaped(target : String) : String = {

    target match {
      // keywords get a suffix "_2"
      case "abort" | "else" | "new" | "return" | "abs" | "elsif" | "not" | "reverse" | "abstract" | "end" | "null" |
        "accept" | "entry" | "select" | "access" | "exception" | "of" | "separate" | "aliased" | "exit" | "or" |
        "some" | "all" | "others" | "subtype" | "and" | "for" | "out" | "synchronized" | "array" | "function" |
        "overriding" | "at" | "tagged" | "generic" | "package" | "task" | "begin" | "goto" | "pragma" | "terminate" |
        "body" | "private" | "then" | "if" | "procedure" | "type" | "case" | "in" | "protected" | "constant" |
        "interface" | "until" | "is" | "raise" | "use" | "declare" | "range" | "delay" | "limited" | "record" |
        "when" | "delta" | "loop" | "rem" | "while" | "digits" | "renames" | "with" | "do" | "mod" | "requeue" |
        "xor" ⇒ return target+"_2"

      // the string is fine anyway
      case _ ⇒ return target
    }
  }
}
