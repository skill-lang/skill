package de.ust.skill.generator.scala

import java.io.{File, PrintWriter}

import scala.collection.JavaConversions._
import scala.io.Source

import de.ust.skill.generator.scala.api.{KnownTypeMaker, SkillStateMaker}
import de.ust.skill.generator.scala.internal._
import de.ust.skill.generator.scala.internal.parsers._
import de.ust.skill.generator.scala.internal.pool._
import de.ust.skill.generator.scala.internal.types.DeclarationImplementationMaker
import de.ust.skill.ir._
import de.ust.skill.parser.Parser

/**
 * Entry point of the scala generator.
 */
object Main {
  private def printHelp: Unit = println("""
usage:
  [options] skillPath outPath

Opitions:
  -p packageName   set a package name used by all emitted code.
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

      m.setOptions(args.slice(0, args.length - 2))
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
 * A generator turns a set of skill declarations into a scala interface providing means of manipulating skill files
 * containing instances of the respective definitions.
 *
 * @author Timm Felden
 */
class Main
    extends FileParserMaker
    with DeclarationInterfaceMaker
    with DeclarationImplementationMaker
    with DeclaredPoolsMaker
    with PoolIteratorMaker
    with BlockInfoMaker
    with KnownTypeMaker
    with KnownPoolMaker
    with BasePoolMaker
    with SubPoolMaker
    with SkillStateMaker
    with SkillExceptionMaker
    with TypeInfoMaker
    with FieldDeclarationMaker
    with SerializableStateMaker
    with ByteStreamParsersMaker
    with FieldParserMaker
    with StringPoolMaker
    with ByteReaderMaker
    with AbstractPoolMaker {

  var outPath: String = null
  var IR: List[Declaration] = null

  /**
   * Translates types into scala type names.
   */
  override protected def _T(t: Type): String = t match {
    case t: GroundType ⇒ t.getName() match {
      // TODO BUG #2
      case "annotation" ⇒ "AnyRef"

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

    case t: ConstantLengthArrayType ⇒ s"Array[${_T(t.getBaseType())}]"
    case t: VariableLengthArrayType ⇒ s"Array[${_T(t.getBaseType())}]"
    case t: ListType                ⇒ s"List[${_T(t.getBaseType())}]"
    case t: SetType                 ⇒ s"Set[${_T(t.getBaseType())}]"
    case t: MapType ⇒ {
      val types = t.getBaseTypes().reverse.map(_T(_))
      types.tail.fold(types.head)({ (U, t) ⇒ s"Map[$t, $U]" });
    }

    case t: Declaration ⇒ "_root_."+packagePrefix + t.getName()
  }

  /**
   * Reads a template file and copies the input to out.
   */
  override protected def copyFromTemplate(out: PrintWriter, template: String) {
    Source.fromFile("src/main/scala/de/ust/skill/generator/scala/templates/"+template).getLines.foreach({ s ⇒ out.write(s+"\n") })
  }

  /**
   * provides the package prefix
   *
   * TODO provide a mechanism to actually set this
   */
  override protected def packagePrefix(): String = _packagePrefix
  private var _packagePrefix = ""

  private def setOptions(args: Array[String]) {
    var index = 0
    while (index < args.length) args(index) match {
      case "-p"    ⇒ _packagePrefix = args(index + 1)+"."; index += 2;

      case unknown ⇒ sys.error(s"unkown Argument: $unknown")
    }
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
}
