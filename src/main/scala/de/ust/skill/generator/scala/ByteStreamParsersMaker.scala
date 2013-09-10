package de.ust.skill.generator.scala

import java.io.PrintWriter

trait ByteStreamParsersMaker {
  protected def makeByteStreamParsers(out: PrintWriter) {
    //package
    out.write(s"package ${packagePrefix}internal.pool\n\n")

    //(imports are part of the template) 
    //the body itself is always the same
    copyFromTemplate(out, "ByteStreamParsers.scala.template")

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