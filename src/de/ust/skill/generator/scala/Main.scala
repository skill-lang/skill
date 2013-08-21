package de.ust.skill.generator.scala

import java.io.File
import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir._
import de.ust.skill.parser.Parser

object Main {

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
      val out = new PrintWriter(new File(outPath + d.getName()+".scala"))
      //package

      //imports

      //class prefix
      out.write("trait " + d.getName() + " {\n")

      //body
      out.write("//////// FIELD INTERACTION ////////\n")
      d.getFields().foreach({ f ⇒
        out.write("def "+f.getName()+"(): "+_T(f.getType())+"\n")
        out.write("def set"+f.getName().capitalize+"("+f.getName()+": "+_T(f.getType())+"):Unit\n")

      })


      //class postfix
      out.write("//////// OBJECT CREATION AND DESTRUCTION ////////\n")
      
      
      
      out.write("//////// UTILS ////////\n")
      
      //nice toString
      out.write("\noverride def toString(): String = \"" + d.getName() + "(\"")
      d.getFields().foreach({ f ⇒
        out.write("+" + f.getName()+"+\", \"")
      })
      out.write("+\")\"\n")
      
      out.write("}");

      out.close()
    })

    //generate general code

    //generate IR specific code?
  }

  private def _T(t: Type): String = t match {
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