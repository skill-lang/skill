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

import scala.Array.canBuildFrom
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.language.implicitConversions

import ${packagePrefix}internal._
import ${packagePrefix}internal.pool._

/**
 * see skill ref man §6.2
 *
 * @author Timm Felden
 * @note Lazy deserialization with multiple blocks will require a block counter which is stored along offsets
 */
final private class FileParser extends ByteStreamParsers {
  /**
   * @return the correct type of storage pool containing the correct data and type
   */
  def makePool(σ: SerializableState,
               name: String,
               t: UserType,
               parent: Option[StoragePool],
               size: Long,
               idx: Long): StoragePool = name match {
""")

    // make pool (depends on IR)
    IR.foreach({ d ⇒
      out.write(s"""    case "${d.getName().toLowerCase()}" ⇒ new ${d.getName()}StoragePool(t, size, σ)
""")
    })

    out.write("""
    case _      ⇒ new StoragePool(name, t, parent, size, idx)
  }
""")

    // the remaining body is always the same
    copyFromTemplate(out, "FileParser.scala.template")

    //class prefix
    out.close()
  }
}