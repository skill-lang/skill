package de.ust.skill.generator.scala.internal.types

import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.Type



trait DeclarationImplementationMaker extends GeneralOutputMaker {
  override def make {
    super.make
    IR.foreach({ d ⇒
      makeDeclaration(open("internal/types/"+d.getName()+".scala"), d)
    })
  }

  private def makeDeclaration(out: PrintWriter, d: Declaration) {
    val name = d.getName()
    val fields = d.getAllFields.filter(!_.isConstant)

    // head
    out.write(s"""package ${packagePrefix}internal.types

final class $name(
  """)

    // data
    out.write(fields.map({ f ⇒ s"private var _${f.getName()}: ${mapType(f.getType())}" }).toArray.mkString("", ",\n  ", ""))

    out.write(s""")
    extends _root_.${packagePrefix}$name {
  def this() {
    this(${fields.map(defaultValue(_)).mkString(", ")})
  }
""")

    // getters & setters
    fields.foreach({ f ⇒
      val name = f.getName()
      val Name = name.capitalize
      out.write(s"""
  final def get$Name = _$name
  final def set$Name($Name: ${mapType(f.getType())}) = _$name = $Name

""")
    });

    // pretty string
    out.write(s"""  override def prettyString(): String = "${d.getName()}(this: "+this""")
    d.getAllFields.foreach({ f ⇒ out.write(s"""+", ${f.getName()}: "+get${f.getName().capitalize}""") })
    out.write("+\")\"\n")

    out.write("}")
    out.close()
  }
}
