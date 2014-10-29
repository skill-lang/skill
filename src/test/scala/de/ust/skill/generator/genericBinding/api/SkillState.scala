/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 29.10.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.api

import java.nio.file.Path
import java.io.File

import scala.collection.mutable.ArrayBuffer

import de.ust.skill.generator.genericBinding.internal.FileParser
import de.ust.skill.generator.genericBinding.internal.SerializableState
import de.ust.skill.generator.genericBinding.internal.SkillType

/**
 * The public interface to the SKilL state, which keeps track of serializable objects and provides (de)serialization
 *  capabilities.
 *
 * @author Timm Felden
 */
trait SkillState {


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

  /**
   * Checks restrictions in types. Restrictions are checked before write/append, where an error is raised if they do not
   * hold.
   */
  def checkRestrictions : Boolean
}

object SkillState {
  /**
   * Creates a new and empty SKilL state.
   */
  def create: SkillState = SerializableState.create

  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def read(path: Path): SkillState = FileParser.read(path)
  def read(file: File): SkillState = read(file.toPath)
  def read(path: String): SkillState = read(new File(path))
}