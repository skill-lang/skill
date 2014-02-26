/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api

import scala.collection.JavaConversions._
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.restriction.MonotoneRestriction
import de.ust.skill.ir.restriction.SingletonRestriction

trait SkillStateMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/SkillState.scala")

    //package & imports
    out.write(s"""package ${packagePrefix}api

import java.nio.file.Path
import java.io.File

import scala.collection.mutable.ArrayBuffer

/**
 * The public interface to the SKilL state, which keeps track of serializable objects and provides (de)serialization
 *  capabilities.
 *
 * @author Timm Felden
 */
trait SkillState {
${
      (for (t ← IR) yield s"  val ${t.getCapitalName}: ${t.getCapitalName}Access").mkString("\n")
    }

  val String: StringAccess

  def all: Iterator[Access[_ <: SkillType]]

  /**
   * Creates a new SKilL file at target. The recommended file extension is ".sf".
   *
   * @note Updates fromPath iff fromPath==null, i.e. if the state has been created out of thin air.
   */
  def write(target: Path): Unit

  /**
   * Write new content to the read SKilL file.
   */
  def write():Unit

  /**
   * Appends new content to the read SKilL file.
   *
   * @pre canAppend
   */
  def append(): Unit

  /**
   * Appends new content to the read SKilL file and stores the result in target.
   *
   * @pre canAppend
   */
  def append(target: Path): Unit
}

object SkillState {
  /**
   * Creates a new and empty SKilL state.
   */
  def create: SkillState = internal.SerializableState.create

  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def read(path: Path): SkillState = internal.FileParser.read(path)
  def read(file: File): SkillState = read(file.toPath)
  def read(path: String): SkillState = read(new File(path))
}""")

    out.close()
  }
}
