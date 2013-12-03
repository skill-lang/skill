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

import scala.collection.mutable.ArrayBuffer

import ${packagePrefix}internal.SerializableState

/**
 * The public interface to the SKilL state, which keeps track of serializable objects and provides (de)serialization
 *  capabilities.
 *
 * @author Timm Felden
 */
trait SkillState {
  /**
   * Creates a new SKilL file at target. The recommended file extension is ".sf".
   */
  def write(target: Path): Unit

  /**
   * Appends new content to the read SKilL file.
   *
   * @pre canAppend
   */
  def append: Unit

  /**
   * Checks, if the changes made to a state can be appended to the read file. This will also return false, if the state
   * has been created and not read from a file.
   */
  def canAppend: Boolean

  /**
   * retrieves a string from the known strings; this can cause disk access, due to the lazy nature of the implementation
   *
   * @throws ArrayOutOfBoundsException if index is not valid
   */
  def getString(index: Long): String
  /**
   * adds a string to the state
   */
  def addString(string: String): Unit
""")

    //access to declared types
    IR.foreach({ t ⇒
      val name = t.getName()
      val Name = name.capitalize
      val sName = name.toLowerCase()
      val tName = "_root_."+packagePrefix + name

      val addArgs = t.getAllFields().filter { f ⇒ !f.isConstant && !f.isIgnored }.map({
        f ⇒ s"${f.getName().capitalize}: ${mapType(f.getType())}"
      }).mkString(", ")

      // singletons get a get$Name function, which returns the single instance
      if (!t.getRestrictions.collect { case r: SingletonRestriction ⇒ r }.isEmpty) {
        out.write(s"""
  /**
   * returns the $name instance
   */
  def get$Name: $tName
""")
      } else {
        out.write(s"""
  /**
   * returns a $name iterator
   */
  def get${Name}s(): Iterator[$tName]
  /**
   * returns a $name iterator which iterates over known instances in type order
   */
  def get${Name}sInTypeOrder(): Iterator[$tName]
  /**
   * adds a new $name to the $name pool
   */
  def add$Name($addArgs): $tName
""")
      }
    })

    // second part: reading of files
    out.write("""}

object SkillState {
  /**
   * Creates a new and empty SKilL state.
   */
  def create: SkillState = SerializableState.create

  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def read(target: Path): SkillState = SerializableState.read(target)
}""")

    out.close()
  }
}
