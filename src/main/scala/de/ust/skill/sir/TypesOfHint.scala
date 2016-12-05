/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 05.12.2016                               *
 * |___/_|\_\_|_|____|    by: feldentm                                        *
\*                                                                            */
package de.ust.skill.sir

import de.ust.skill.common.scala.SkillID
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.UnknownObject

/**
 *  A hint including name and parsed arguments
 */
sealed class Hint (_skillID : SkillID) extends SkillObject(_skillID) {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  private[sir] def this(_skillID : SkillID, arguments : scala.collection.mutable.ArrayBuffer[java.lang.String], name : java.lang.String) {
    this(_skillID)
    _arguments = arguments
    _name = name
  }

  final protected var _arguments : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String]()
  /**
   *  if a string has arguments at all, they are stored as plain text
   */
  def arguments : scala.collection.mutable.ArrayBuffer[java.lang.String] = _arguments
  final private[sir] def Internal_arguments = _arguments
  /**
   *  if a string has arguments at all, they are stored as plain text
   */
  def `arguments_=`(arguments : scala.collection.mutable.ArrayBuffer[java.lang.String]) : scala.Unit = { _arguments = arguments }
  final private[sir] def `Internal_arguments_=`(v : scala.collection.mutable.ArrayBuffer[java.lang.String]) = _arguments = v

  final protected var _name : java.lang.String = null
  def name : java.lang.String = _name
  final private[sir] def Internal_name = _name
  def `name_=`(name : java.lang.String) : scala.Unit = { _name = name }
  final private[sir] def `Internal_name_=`(v : java.lang.String) = _name = v

  override def prettyString : String = s"Hint(#$skillID, arguments: ${arguments}, name: ${name})"

  override def getTypeName : String = "hint"

  override def toString = "Hint#"+skillID
}

object Hint {
  def unapply(self : Hint) = Some(self.arguments, self.name)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: Hint])
      extends Hint(_skillID) with UnknownObject[Hint] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}
