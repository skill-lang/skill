/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 19.11.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.api

import java.nio.file.Path
import java.io.File

import scala.collection.mutable.ArrayBuffer

import _root_.de.ust.skill.generator.genericBinding.internal.FileParser
import _root_.de.ust.skill.generator.genericBinding.internal.State
import _root_.de.ust.skill.generator.genericBinding.internal.SkillType

/**
 * The public interface to the in-memory representation of a SKilL file.
 * This class provides access to instances of types stored in a file as well as state management capabilities.
 *
 * @note The well-formedness of a file can be checked at any time using the check() method
 * @todo changePath
 * @todo changeWriteMode
 * @author Timm Felden
 */
trait SkillFile {


  val String : StringAccess

  def all : Iterator[Access[_ <: SkillType]]

  /**
   * Checks restrictions in types. Restrictions are checked before write/append, where an error is raised if they do not
   * hold.
   */
  def check : Unit

  /**
   * Check consistency and write changes to disk.
   * @note this will not sync the file to disk, but it will block until all in-memory changes are written to buffers.
   * @note if check fails, then the state is guaranteed to be unmodified compared to the state before flush
   */
  def flush : Unit

  /**
   * Same as flush, but will also sync and close file, thus the state is not usable afterwards.
   */
  def close : Unit
}

/**
 * Modes for file handling.
 */
sealed abstract class Mode;
sealed abstract class OpenMode extends Mode;
sealed abstract class WriteMode extends Mode;
object Create extends OpenMode;
object Read extends OpenMode;
object Write extends WriteMode;
object Append extends WriteMode;

object SkillFile {

  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def open(path : Path, flags : Mode*) : SkillFile = State.open(path, flags.to)
  def open(file : File, flags : Mode*) : SkillFile = State.open(file.ensuring(exists(_)).toPath, flags.to)
  def open(path : String, flags : Mode*) : SkillFile = State.open(new File(path).ensuring(exists(_)).toPath, flags.to)

  // ensures existence :)
  private def exists(f : File) = {
    if (!f.exists())
      f.createNewFile()
    true
  }
}
