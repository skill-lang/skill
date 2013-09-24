package de.ust.skill.generator.scala

import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir.Declaration

trait DeclarationInterfaceMaker extends GeneralOutputMaker {
  override def make {
    super.make
    IR.foreach({ d ⇒
      makeDeclarationInterface(open(d.getName()+".scala"), d)
    })
  }

  private def makeDeclarationInterface(out: PrintWriter, d: Declaration) {
    //package
    if (packagePrefix.length > 0) {
      out.write(s"package ${packagePrefix.substring(0, packagePrefix.length - 1)}\n\n")
    }

    //imports
    if (null == d.getSuperType()) {
      out.write(s"import ${packagePrefix}api.KnownType\n\n")
    }

    //class prefix
    out.write(s"trait ${d.getName()} ${
      if (null != d.getSuperType()) { s"extends ${d.getSuperType().getName()}" }
      else { "extends KnownType" }
    } {\n")

    //body
    d.getFields().foreach({ f ⇒
      val name = f.getName.capitalize

      if (f.isConstant) {
        // constants do not have a setter
        out.write(s"\n  def get$name(): ${_T(f.getType())} = ${f.constantValue}\n")
      } else {
        // add a warning to auto fields
        if (f.isAuto) {
          out.write("\n  /** auto aka not serialized */")
        }

        // standard field data interface
        out.write(s"\n  def get$name(): ${_T(f.getType())}\n")
        out.write(s"  def set$name($name: ${_T(f.getType())}): Unit\n")
      }
    })

    out.write("}");

    out.close()
  }
}
