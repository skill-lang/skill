package de.ust.skill.generator.scala.internal.parsers

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait FileParserMaker extends GeneralOutputMaker {
  override def make {
    super.make
    val out = open("internal/parsers/FileParser.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.parsers

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.language.implicitConversions

import ${packagePrefix}internal._
import ${packagePrefix}internal.pool._

/**
 * see skill ref man §6.2.
 *
 * The file parser reads a file and gathers type declarations.
 * It creates a new SerializableState which contains types for these type declaration.
 *
 * @author Timm Felden
 */
final private class FileParser extends ByteStreamParsers {
  /**
   * creates storage pools in type order
   */
  private def makeState() {
    def makePool(t: UserType): AbstractPool = {
      val result = t.name match {
""")

    // make pool (depends on IR)
    IR.foreach({ d ⇒
      out.write(s"""        case "${d.getName().toLowerCase()}" ⇒ new ${d.getName()}StoragePool(t, σ, blockCounter)
""")
    })

    // the remaining body is always the same
    copyFromTemplate(out, "FileParser.scala.template")

    //class prefix
    out.close()
  }
}