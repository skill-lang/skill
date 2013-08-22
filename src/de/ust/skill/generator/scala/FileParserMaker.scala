package de.ust.skill.generator.scala

import java.io.PrintWriter

trait FileParserMaker {
  protected def makeFileParser(out: PrintWriter) {
    //package
    out.write("package " /* + prefix */ +"internal\n\n")

    //imports
    out.write("import java.io.BufferedInputStream\n")
    out.write("import java.io.ByteArrayInputStream\n")
    out.write("import java.io.File\n")
    out.write("import java.io.FileInputStream\n")
    out.write("import scala.collection.mutable.ArrayBuffer\n")
    out.write("import scala.collection.mutable.HashMap\n")
    out.write("import scala.language.implicitConversions\n")
    out.write("import " /* + prefix */ +"internal.reflection._\n")
    out.write("import " /* + prefix */ +"internal.pool._\n\n")

    //the body itself is always the same
    copyFromTemplate(out, "FileParser.scala.template")

    //class prefix
    out.close()
  }

  /**
   * Assume template copy functionality.
   */
  protected def copyFromTemplate(out: PrintWriter, template: String): Unit
}