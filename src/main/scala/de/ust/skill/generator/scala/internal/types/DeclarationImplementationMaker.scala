package de.ust.skill.generator.scala.internal.types

import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Type

trait DeclarationImplementationMaker {
  protected def makeDeclarationImplementation(out: PrintWriter, d: Declaration) {
    out.write(s"""package ${packagePrefix}internal.types

import ${packagePrefix}SerializableState

class Date(var _date:Long) extends _root_.expected.Date {

""")

    // body
    d.getFields().foreach({ f ⇒
      out.write(s"""
  override def ${f.getName()} = _${f.getName()}
  override def set${f.getName().capitalize}(${f.getName()}: ${_T(f.getType())})= _${f.getName()} = ${f.getName()}
""")
    })

    out.write("}")
    out.close()
  }

  /**
   * Assume the existence of a translation function for types.
   */
  protected def _T(t: Type): String

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix(): String
}