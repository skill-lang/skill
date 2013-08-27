package de.ust.skill.generator.scala

import java.io.PrintWriter

trait IteratorMaker {
  protected def makeIterator(out: PrintWriter) {
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import ${packagePrefix}internal.StoragePool

""")

    //the body itself is always the same
    copyFromTemplate(out, "Iterator.scala.template")

    //class prefix
    out.close()
  }

  /**
   * Assume template copy functionality.
   */
  protected def copyFromTemplate(out: PrintWriter, template: String): Unit

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix(): String
}