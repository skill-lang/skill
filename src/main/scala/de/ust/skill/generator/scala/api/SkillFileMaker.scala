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

trait SkillFileMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/SkillFile.scala")

    //package & imports
    out.write(s"""package ${packagePrefix}api

import java.nio.file.Path
import java.io.File

import scala.collection.mutable.ArrayBuffer

import ${packagePrefix}internal.FileParser
import ${packagePrefix}internal.State
import ${packagePrefix}internal.SkillType

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
${
      (for (t ← IR) yield s"  val ${t.getName.capital} : ${t.getName.capital}Access").mkString("\n")
    }

  val String : StringAccess

  def all : Iterator[Access[_ <: SkillType]]

  /**
   * Checks restrictions in types. Restrictions are checked before write/append, where an error is raised if they do not
   * hold.
   */
  def check : Boolean

  /**
   * Check consistency and write changes to disk.
   * @note this will not sync the file to disk, but it will block until all in-memory changes are written to buffers.
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
""")

    out.close()
  }
}
