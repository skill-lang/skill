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
import ${packagePrefix}internal.pool.StringPool

/**
 * The public interface to the SKilL state, which keeps track of serializable objects and provides (de)serialization
 *  capabilities.
 *
 * @author Timm Felden
 */
trait SkillState {
  import SkillState._

  /**
   * Creates a new SKilL file at target. The recommended file extension is ".sf".
   *
   * @note Updates fromPath iff fromPath==null, i.e. if the state has been created out of thin air.
   */
  def write(target: Path): Unit

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

  val String: StringAccess
""")

    //access to declared types
    for (t ← IR) {
      val name = t.getName
      out.write(s"""  val ${name}: ${name}Access
""")
    }

    // second part: reading of files
    out.write("""}

object SkillState {
  trait StringAccess {
    def get(index: Long): String
    def add(string: String)
    def size: Int
  }""")

    for (t ← IR) {

      val name = t.getName
      val tName = "_root_."+packagePrefix + name

      val addArgs = t.getAllFields().filter { f ⇒ !f.isConstant && !f.isIgnored }.map({
        f ⇒ s"${f.getName().capitalize}: ${mapType(f.getType())}"
      }).mkString(", ")

      // singletons get a get$Name function, which returns the single instance
      if (!t.getRestrictions.collect { case r: SingletonRestriction ⇒ r }.isEmpty) {
        out.write(s"""
  trait ${name}Access {
    /**
     * returns the $name instance
     */
    def get: $tName
  }
""")
      } else {
        out.write(s"""
  trait ${name}Access {
    /**
     * returns a fast $name iterator
     */
    def all: Iterator[$tName]
    /**
     * returns a type ordered $name iterator
     */
    def allInTypeOrder: Iterator[$tName]
    /**
     * adds a new $name to the $name pool
     */
    def apply($addArgs): $tName
    /**
     * returns a fast $name iterator
     */
    def iterator: Iterator[$tName]
    /**
     * returns the number of dynimc instances of this type, i.e. including subtypes
     */
    def size: Int
  }
""")
      }
    }

    out.write("""
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
