/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api

import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.restriction.SingletonRestriction

trait AccessMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/Access.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}api

import scala.reflect.ClassTag

import ${packagePrefix}internal.SkillType
import ${packagePrefix}internal.FieldDeclaration

/**
 * @author Timm Felden
 */
trait Access[T <: SkillType] extends Iterable[T] {
  /**
   * the SKilL name of T
   */
  val name : String

  /**
   * the SKilL name of the super type of T, if any
   */
  val superName : Option[String]

  /**
   * @return iterator over all instances of T
   */
  def all : Iterator[T]
  /**
   * just for convenience
   */
  def iterator : Iterator[T]
  /**
   * @return a type ordered Container iterator over all instances of T
   */
  def allInTypeOrder : Iterator[T]

  /**
   * @return an iterator over all field declarations, even those provided by the binary skill file
   */
  def allFields : Iterator[FieldDeclaration[_]]

  override def size : Int
  override def foreach[U](f : T ⇒ U) : Unit
  override def toArray[B >: T : ClassTag] : Array[B]
}

trait StringAccess {
  def get(index : Long) : String
  def add(string : String)
  def all : Iterator[String]
  def size : Int
}
""")
    for (t ← IR) {
      if (t.getRestrictions.collect { case r : SingletonRestriction ⇒ r }.isEmpty)
        out.write(s"""
trait ${t.getName.capital}Access extends Access[$packagePrefix${t.getName.capital}] {
  /**
   * create a new ${t.getName} instance
   */
  def apply(${makeConstructorArguments(t)}) : _root_.$packagePrefix${t.getName.capital}
}
""")
      else
        out.write(s"""
trait ${t.getName.capital}Access extends Access[$packagePrefix${t.getName.capital}] {
  /**
   * @return the instance
   */
  def get: $packagePrefix${t.getName.capital}
}
""")
    }

    out.close()
  }
}
