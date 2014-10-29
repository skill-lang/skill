/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 29.10.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.api

import scala.reflect.ClassTag

import de.ust.skill.generator.genericBinding.internal.SkillType
import de.ust.skill.generator.genericBinding.internal.FieldDeclaration

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
