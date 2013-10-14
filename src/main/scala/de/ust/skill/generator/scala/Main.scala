/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.io.{ File, PrintWriter }

import scala.collection.JavaConversions._
import scala.io.Source

import de.ust.skill.generator.scala.api.{ KnownTypeMaker, SkillStateMaker }
import de.ust.skill.generator.scala.api.GenericTypeMaker
import de.ust.skill.generator.scala.api.SkillTypeMaker
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
    with AbstractPoolMaker
    with BasePoolMaker
    with BlockInfoMaker
    with ByteReaderMaker
    with ByteStreamParsersMaker
    with DeclarationInterfaceMaker
    with DeclarationImplementationMaker
    with DeclaredPoolsMaker
    with FieldParserMaker
    with FieldDeclarationMaker
    with GenericPoolMaker
    with GenericTypeMaker
    with KnownPoolMaker
    with KnownTypeMaker
    with PoolIteratorMaker
    with SerializableStateMaker
    with SkillExceptionMaker
    with SkillStateMaker
    with SkillTypeMaker
    with StringPoolMaker
    with SubPoolMaker
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

    case t: ConstantLengthArrayType ⇒ s"Array[${mapType(t.getBaseType())}]"
    case t: VariableLengthArrayType ⇒ s"ArrayBuffer[${mapType(t.getBaseType())}]"
    case t: ListType                ⇒ s"List[${mapType(t.getBaseType())}]"
    case t: SetType                 ⇒ s"Set[${mapType(t.getBaseType())}]"
    case t: MapType ⇒ {
      val types = t.getBaseTypes().reverse.map(mapType(_))
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
