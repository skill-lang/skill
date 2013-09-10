package de.ust.skill.generator.scala.internal.parsers

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait ByteStreamParsersMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/pool/ByteStreamParsers.scala")
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