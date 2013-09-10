package de.ust.skill.generator.scala

import de.ust.skill.ir.Declaration
import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.ir.Type

trait DeclarationInterfaceMaker extends GeneralOutputMaker {
  override def make {
    super.make
    IR.foreach({ d ⇒
      makeDeclarationInterface(open(d.getName()+".scala"), d)
    })
  }

  private def makeDeclarationInterface(out: PrintWriter, d: Declaration) {
    //package
    if (packagePrefix.length > 0)
      out.write(s"package ${packagePrefix.substring(0, packagePrefix.length - 1)}\n\n")

    //imports

    //class prefix
    out.write("trait "+d.getName()+" {\n")

    //body
    out.write("  //////// FIELD INTERACTION ////////\n\n")
    d.getFields().foreach({ f ⇒
      val name = f.getName
      val Name = f.getName.capitalize

      out.write(s"  def $name(): ${_T(f.getType())}\n")
      out.write(s"  def set$Name($name: ${_T(f.getType())}): Unit\n\n")
    })

    //class postfix
    out.write("  //////// OBJECT CREATION AND DESTRUCTION ////////\n\n")

    out.write("  //////// UTILS ////////\n\n")

    //nice toString
    out.write(d.getFields.map({ f ⇒ f.getName() }).toArray.mkString(
      s"""  override def toString(): String = "${d.getName()}("+""",
      "+\", \"+",
      "+\")\"\n"))

    out.write("}");

    out.close()
  }
}