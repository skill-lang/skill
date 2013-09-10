package de.ust.skill.generator.scala.internal.parsers

import java.io.PrintWriter

trait FileParserMaker {
  protected def makeFileParser(out: PrintWriter) {
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream

import scala.Array.canBuildFrom
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.language.implicitConversions

import ${packagePrefix}internal.pool._
""")

    //the body itself is always the same
    copyFromTemplate(out, "FileParser.scala.template")

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