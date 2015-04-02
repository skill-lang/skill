/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.statistics

import de.ust.skill.ir._
import de.ust.skill.parser.Parser
import java.io.File
import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList
import de.ust.skill.generator.common.Generator
import java.util.Date

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * The used language is C++ (or rather something that doxygen recoginezes as C++).
 *
 * @note Using C++ has the effect, that neither interfaces nor enums with fields can be represented correctly.
 * @note None of the languages supported by doxygen seems to provide all required features. We may fix this by
 * switching to scaladoc or by hoping that doxygen will support scala or swift eventually.
 *
 * @author Timm Felden
 */
class Main extends FakeMain
    with EnumTypeMaker
    with InterfaceTypeMaker
    with TypedefMaker
    with UserTypeMaker {

  override def comment(d : Declaration) = d.getComment.format("/*!\n", " * ", 80, " */\n").replace('<', '⟨').replace('>', '⟩')
  override def comment(f : Field) = f.getComment.format("    /*!\n", "     * ", 80, "     */\n").replace('<', '⟨').replace('>', '⟩')

  /**
   * Translates the types into Ada types.
   */
  override protected def mapType(t : Type) : String = t match {
    case t : GroundType ⇒ t.getName.lower match {
      case "annotation" ⇒ "void*"

      case "bool"       ⇒ "bool"

      case "i8"         ⇒ "int8_t"
      case "i16"        ⇒ "int16_t"
      case "i32"        ⇒ "int32_t"
      case "i64"        ⇒ "int64_t"
      case "v64"        ⇒ "v64"

      case "f32"        ⇒ "float"
      case "f64"        ⇒ "double"

      case "string"     ⇒ "std::string"
    }

    case t : ConstantLengthArrayType ⇒ s"${mapType(t.getBaseType())}[${t.getLength()}]"
    case t : VariableLengthArrayType ⇒ s"${mapType(t.getBaseType())}[]"
    case t : ListType                ⇒ s"std::list<${mapType(t.getBaseType())}>"
    case t : SetType                 ⇒ s"std::set<${mapType(t.getBaseType())}>"
    case t : MapType                 ⇒ t.getBaseTypes().map(mapType(_)).reduceRight { (n, r) ⇒ s"std::map<$n, $r>" }

    case t : Declaration             ⇒ escaped(t.getName.capital)
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

  override def setOption(option : String, value : String) = option match {
    case unknown ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def printHelp : Unit = println("""
Opitions (doxygen):
  (none)
""")

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
