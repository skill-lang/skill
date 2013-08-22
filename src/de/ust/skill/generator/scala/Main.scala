package de.ust.skill.generator.scala

import java.io.File
import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir._
import de.ust.skill.parser.Parser

object Main extends FileParserMaker with DeclarationInterfaceMaker {

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
    IR.foreach({ d ⇒ makeDeclarationInterface(new PrintWriter(new File(outPath + d.getName()+".scala")), d) })

    //generate general code
    makeFileParser(new PrintWriter(new File(outPath+"internal/FileParser.scala")))

    //generate IR specific code?
  }

  override def _T(t: Type): String = t match {
    case t: GroundType ⇒ t.getName() match {
      case "i8"  ⇒ "Byte"
      case "i16" ⇒ "Short"
      case "i32" ⇒ "Int"
      case "i64" ⇒ "Long"
      case "v64" ⇒ "Long"
    }
    case t: Declaration ⇒ t.getName()
  }
}