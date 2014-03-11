/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.io.File
import java.util.Date
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.scala.api.AccessMaker
import de.ust.skill.generator.scala.api.SkillStateMaker
import de.ust.skill.generator.scala.api.SkillTypeMaker
import de.ust.skill.generator.scala.internal.ExceptionsMaker
import de.ust.skill.generator.scala.internal.FieldParserMaker
import de.ust.skill.generator.scala.internal.FileParserMaker
import de.ust.skill.generator.scala.internal.SerializableStateMaker
import de.ust.skill.generator.scala.internal.SerializationFunctionsMaker
import de.ust.skill.generator.scala.internal.TypeInfoMaker
import de.ust.skill.generator.scala.internal.pool.DeclaredPoolsMaker
import de.ust.skill.generator.scala.internal.streams.FileInputStreamMaker
import de.ust.skill.generator.scala.internal.streams.InStreamMaker
import de.ust.skill.generator.scala.internal.types.DeclarationImplementationMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.parser.Parser
import de.ust.skill.generator.scala.internal.FullyGenericInstanceMaker
import de.ust.skill.generator.scala.internal.InternalInstancePropertiesMaker
import de.ust.skill.generator.scala.internal.StringPoolMaker
import de.ust.skill.generator.scala.internal.streams.OutBufferMaker
import de.ust.skill.generator.scala.internal.streams.OutStreamMaker
import de.ust.skill.generator.scala.internal.streams.FileOutputStreamMaker
import de.ust.skill.generator.scala.internal.StateWriterMaker
import de.ust.skill.generator.scala.internal.RestrictionsMaker
import de.ust.skill.generator.scala.internal.ExceptionsMaker
import de.ust.skill.generator.scala.internal.StateWriterMaker
import de.ust.skill.generator.scala.internal.StateAppenderMaker

/**
 * Entry point of the scala generator.
 */
object Main {
  private def printHelp: Unit = println("""
usage:
  [options] skillPath outPath

Opitions:
  -p packageName      set a package name used by all emitted code.
  -h1|h2|h3 content   overrides the content of the respective header line
  -u userName         set a user name
  -date date          set a custom date
""")

  /**
   * Takes an argument skill file name and generates a scala binding.
   */
  def main(args: Array[String]): Unit = {
    var m = new Main

    //processing command line arguments
    if (2 > args.length) {
      printHelp
    } else {

      m.setOptions(args.slice(0, args.length - 2)).ensuring(m._packagePrefix != "", "You have to specify a non-empty package name!")
      val skillPath = args(args.length - 2)
      m.outPath = args(args.length - 1)

      //parse argument code
      m.IR = Parser.process(new File(skillPath)).toList

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
 * A generator turns a set of skill declarations into a scala interface providing means of manipulating skill files
 * containing instances of the respective definitions.
 *
 * @author Timm Felden
 */
class Main extends FakeMain
    with AccessMaker
    with api.FieldDeclarationMaker
    with DeclarationInterfaceMaker
    with DeclarationImplementationMaker
    with ExceptionsMaker
    with FieldParserMaker
    with FileInputStreamMaker
    with FileOutputStreamMaker
    with FileParserMaker
    with FullyGenericInstanceMaker
    with InStreamMaker
    with InternalInstancePropertiesMaker
    with internal.FieldDeclarationMaker
    with OutBufferMaker
    with OutStreamMaker
    with RestrictionsMaker
    with SerializableStateMaker
    with SerializationFunctionsMaker
    with SkillStateMaker
    with SkillTypeMaker
    with StateAppenderMaker
    with StateWriterMaker
    with StringPoolMaker
    with TypeInfoMaker {

  var outPath: String = null
  var IR: List[Declaration] = null

  /**
   * Translates types into scala type names.
   */
  override protected def mapType(t: Type): String = t match {
    case t: GroundType ⇒ t.getName() match {
      case "annotation" ⇒ "SkillType"

      case "bool"       ⇒ "Boolean"

      case "i8"         ⇒ "Byte"
      case "i16"        ⇒ "Short"
      case "i32"        ⇒ "Int"
      case "i64"        ⇒ "Long"
      case "v64"        ⇒ "Long"

      case "f32"        ⇒ "Float"
      case "f64"        ⇒ "Double"

      case "string"     ⇒ "String"
    }

    case t: ConstantLengthArrayType ⇒ s"$ArrayTypeName[${mapType(t.getBaseType())}]"
    case t: VariableLengthArrayType ⇒ s"$VarArrayTypeName[${mapType(t.getBaseType())}]"
    case t: ListType                ⇒ s"$ListTypeName[${mapType(t.getBaseType())}]"
    case t: SetType                 ⇒ s"$SetTypeName[${mapType(t.getBaseType())}]"
    case t: MapType ⇒ {
      val types = t.getBaseTypes().reverse.map(mapType(_))
      types.tail.fold(types.head)({ (U, t) ⇒ s"$MapTypeName[$t, $U]" });
    }

    case t: Declaration ⇒ "_root_."+packagePrefix + t.getName()
  }

  /**
   * creates argument list of a constructor call, not including potential skillID or braces
   */
  override protected def makeConstructorArguments(t: Declaration) = t.getAllFields.filterNot { f ⇒ f.isConstant || f.isIgnored }.map({ f ⇒ s"${escaped(f.getName)} : ${mapType(f.getType())}" }).mkString(", ")
  override protected def appendConstructorArguments(t: Declaration) = {
    val r = t.getAllFields.filterNot { f ⇒ f.isConstant || f.isIgnored }
    if(r.isEmpty) ""
    else r.map({ f ⇒ s"${escaped(f.getName)} : ${mapType(f.getType())}" }).mkString(", ", ", ", "")
  }

  /**
   * provides the package prefix
   */
  override protected def packagePrefix(): String = _packagePrefix
  private var _packagePrefix = ""

  override private[scala] def header: String = _header
  private var _header = ""

  private def setOptions(args: Array[String]) {
    var index = 0
    var headerLine1: Option[String] = None
    var headerLine2: Option[String] = None
    var headerLine3: Option[String] = None
    var userName: Option[String] = None
    var date: Option[String] = None

    while (index < args.length) args(index) match {
      case "-p"    ⇒ _packagePrefix = args(index + 1)+"."; index += 2;
      case "-u"    ⇒ userName = Some(args(index + 1)); index += 2;
      case "-date" ⇒ date = Some(args(index + 1)); index += 2;
      case "-h1"   ⇒ headerLine1 = Some(args(index + 1)); index += 2;
      case "-h2"   ⇒ headerLine2 = Some(args(index + 1)); index += 2;
      case "-h3"   ⇒ headerLine3 = Some(args(index + 1)); index += 2;

      case unknown ⇒ sys.error(s"unkown Argument: $unknown")
    }

    // create header from options
    val headerLineLength = 51
    headerLine1 = Some((headerLine1 match {
      case Some(s) ⇒ s
      case None    ⇒ "Your SKilL Scala Binding"
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    headerLine2 = Some((headerLine2 match {
      case Some(s) ⇒ s
      case None ⇒ "generated: "+(date match {
        case Some(s) ⇒ s
        case None    ⇒ (new java.text.SimpleDateFormat("dd.MM.yyyy")).format(new Date)
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    headerLine3 = Some((headerLine3 match {
      case Some(s) ⇒ s
      case None ⇒ "by: "+(userName match {
        case Some(s) ⇒ s
        case None    ⇒ System.getProperty("user.name")
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))

    _header = s"""/*  ___ _  ___ _ _                                                            *\\
** / __| |/ (_) | |       ${headerLine1.get} **
** \\__ \\ ' <| | | |__     ${headerLine2.get} **
** |___/_|\\_\\_|_|____|    ${headerLine3.get} **
\\*                                                                            */
"""
  }

  override protected def defaultValue(f: Field) = f.getType() match {
    case t: GroundType ⇒ t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0f"
      case "bool"                               ⇒ "false"
      case _                                    ⇒ "null"
    }

    // TODO compound types would behave more nicely if they would be initialized with empty collections instead of null

    case _ ⇒ "null"
  }

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  protected def escaped(target: String): String = target match {
    //keywords get a suffix "_", because that way at least auto-completion will work as expected
    case "abstract" | "case" | "catch" | "class" | "def" | "do" | "else" | "extends" | "false" | "final" | "finally" |
      "for" | "forSome" | "if" | "implicit" | "import" | "lazy" | "match" | "new" | "null" | "object" | "override" |
      "package" | "private" | "protected" | "return" | "sealed" | "super" | "this" | "throw" | "trait" | "true" |
      "try" | "type" | "var" | "while" | "with" | "yield" | "val" ⇒ target+"_"

    //the string is fine anyway
    case _ ⇒ target
  }
}
