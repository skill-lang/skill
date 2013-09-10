package de.ust.skill.generator.scala

import java.io.File
import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import scala.io.Source
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.Type
import de.ust.skill.parser.Parser
import de.ust.skill.generator.scala.internal.IteratorMaker
import de.ust.skill.generator.scala.internal.SerializableStateMaker
import de.ust.skill.generator.scala.internal.TypeInfoMaker
import de.ust.skill.generator.scala.internal.parsers.ByteStreamParsersMaker
import de.ust.skill.generator.scala.internal.parsers.FileParserMaker
import de.ust.skill.generator.scala.internal.pool.StringPoolMaker
import de.ust.skill.generator.scala.internal.pool.StoragePoolMaker

/**
 * A generator turns a set of skill declarations into a scala interface providing means of manipulating skill files
 * containing instances of the respective definitions.
 *
 * @author Timm Felden
 */
object Main
    extends FileParserMaker
    with DeclarationInterfaceMaker
    with IteratorMaker
    with TypeInfoMaker
    with SerializableStateMaker
    with ByteStreamParsersMaker
    with StringPoolMaker
    with StoragePoolMaker {

  def printHelp {
    println("usage:")
    println("[options] skillPath outPath")
  }

  /**
   * Takes an argument skill file name and generates a scala binding.
   */
  def main(args: Array[String]): Unit = {
    //processing command line arguments
    if (2 > args.length) {
      printHelp
      return
    }
    val skillPath = args(args.length - 2)
    val outPath = args(args.length - 1)

    //parse argument code
    val IR = (new Parser).process(new File(skillPath))

    //generate public interface for type declarations
    IR.foreach({ d ⇒
      makeDeclarationInterface(Writer(outPath + d.getName()+".scala"), d)
    })
    makeSerializableState(Writer(outPath+"internal/SerializableState.scala"), IR)

    //generate general code
    makeStringPool(Writer(outPath+"internal/pool/StringPool.scala"))
    makeStoragePool(Writer(outPath+"internal/pool/StoragePool.scala"))
    makeByteStreamParsers(Writer(outPath+"internal/pool/ByteStreamParsers.scala"))
    makeFileParser(Writer(outPath+"internal/FileParser.scala"))
    makeIterator(Writer(outPath+"internal/Iterator.scala"))
    makeTypeInfo(Writer(outPath+"internal/TypeInfo.scala"))
  }

  /**
   * Translates types into scala type names.
   */
  override protected def _T(t: Type): String = t match {
    case t: GroundType ⇒ t.getName() match {
      case "i8"  ⇒ "Byte"
      case "i16" ⇒ "Short"
      case "i32" ⇒ "Int"
      case "i64" ⇒ "Long"
      case "v64" ⇒ "Long"
    }
    case t: Declaration ⇒ t.getName()
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
  override protected def packagePrefix(): String = ""
}