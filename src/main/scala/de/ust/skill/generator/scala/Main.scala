package de.ust.skill.generator.scala

import java.io.File
import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import scala.io.Source
import de.ust.skill.generator.scala.internal.FieldDeclarationMaker
import de.ust.skill.generator.scala.internal.IteratorMaker
import de.ust.skill.generator.scala.internal.SerializableStateMaker
import de.ust.skill.generator.scala.internal.TypeInfoMaker
import de.ust.skill.generator.scala.internal.parsers.ByteStreamParsersMaker
import de.ust.skill.generator.scala.internal.parsers.FileParserMaker
import de.ust.skill.generator.scala.internal.pool.StoragePoolMaker
import de.ust.skill.generator.scala.internal.pool.StringPoolMaker
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.Type
import de.ust.skill.parser.Parser
import de.ust.skill.generator.scala.internal.parsers.ByteReaderMaker
import de.ust.skill.generator.scala.internal.parsers.FieldParserMaker
import de.ust.skill.generator.scala.internal.SkillExceptionMaker
import de.ust.skill.generator.scala.internal.types.DeclarationImplementationMaker
import de.ust.skill.generator.scala.internal.pool.DeclaredPoolsMaker
import de.ust.skill.ir.CompoundType
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import scala.Boolean

/**
 * Entry point of the scala generator.
 */
object Main {
  def printHelp {
    println("usage:")
    println("[options] skillPath outPath")
  }

  /**
   * Takes an argument skill file name and generates a scala binding.
   */
  def main(args: Array[String]): Unit = {
    var m = new Main

    //processing command line arguments
    if (2 > args.length) {
      printHelp
      return
    }
    m.setOptions(args.slice(0, args.length - 2))
    val skillPath = args(args.length - 2)
    m.outPath = args(args.length - 1)

    //parse argument code
    m.IR = (new Parser).process(new File(skillPath)).toList

    // create output using maker chain
    m.make;
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
    with IteratorMaker
    with SkillExceptionMaker
    with TypeInfoMaker
    with FieldDeclarationMaker
    with SerializableStateMaker
    with ByteStreamParsersMaker
    with FieldParserMaker
    with StringPoolMaker
    with ByteReaderMaker
    with StoragePoolMaker {

  var outPath: String = null
  var IR: List[Declaration] = null

  /**
   * Translates types into scala type names.
   */
  override protected def _T(t: Type): String = t match {
    case t: GroundType ⇒ t.getName() match {
      // BUG #2
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
      val types = t.getBaseType().reverse.map(_T(_))
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
}