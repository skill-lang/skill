package de.ust.skill.generator.scala

import de.ust.skill.ir.Declaration
import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.ir.Type
import scala.collection.mutable.Buffer

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
    out.write(s"trait ${d.getName()} ${
      if (null != d.getSuperType()) s"extends ${d.getSuperType().getTypeName()}"
      else ""
    } {\n")

    //body
    out.write("  //////// FIELD INTERACTION ////////\n\n")
    d.getFields().foreach({ f ⇒
      val name = f.getName.capitalize

      out.write(s"  def get$name(): ${_T(f.getType())}\n")
      out.write(s"  def set$name($name: ${_T(f.getType())}): Unit\n\n")
    })

    //class postfix
    out.write("  //////// OBJECT CREATION AND DESTRUCTION ////////\n\n")

    out.write("  //////// UTILS ////////\n\n")

    //nice toString
    {
      val ts = d.getAllFields.map({ f ⇒ s""""${f.getName()}: "+get${f.getName().capitalize}""" })
      if (null != d.getSuperType())
        out.write("  override\n")
      out.write(ts.mkString(
        // TODO maybe we should add a this:this.toString; we have to check toString first
        s"""  def prettyString(): String = "${d.getName()}("+""",
        "+\", \"+",
        "+\")\"\n"))
    }

    out.write("}");

    out.close()
  }
}