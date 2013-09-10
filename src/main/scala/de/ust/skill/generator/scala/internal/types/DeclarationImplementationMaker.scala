package de.ust.skill.generator.scala.internal.types

import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Type
import de.ust.skill.generator.scala.GeneralOutputMaker

trait DeclarationImplementationMaker extends GeneralOutputMaker {
  override def make {
    super.make
    IR.foreach({ d ⇒
      makeDeclaration(open("internal/types/"+d.getName()+".scala"), d)
    })
  }

  private def makeDeclaration(out: PrintWriter, d: Declaration) {
    val name = d.getName()
    val fields = d.getFields

    // head
    out.write(s"""package ${packagePrefix}internal.types

class $name(
  """)

    // data
    out.write(fields.map({ f ⇒ s"var _${f.getName()}: ${_T(f.getType())}" }).toArray.mkString("", ",\n  ", ""))

    out.write(s""")
    extends _root_.${packagePrefix}$name {
""")

    // getters & setters
    fields.foreach({ f ⇒
      val name = f.getName()
      out.write(s"""
  override def $name = _$name
  override def set${name.capitalize}($name: ${_T(f.getType())}) = _$name = $name

""")
    })

    out.write("}")
    out.close()
  }
}