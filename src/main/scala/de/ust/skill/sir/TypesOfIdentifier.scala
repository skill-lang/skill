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
 *  A nicer version of a skill name that can be used to adapt to a target language naming convention.
 */
sealed class Identifier (_skillID : SkillID) extends SkillObject(_skillID) {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  private[sir] def this(_skillID : SkillID, parts : scala.collection.mutable.ArrayBuffer[java.lang.String], skillname : java.lang.String) {
    this(_skillID)
    _parts = parts
    _skillname = skillname
  }

  final protected var _parts : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String]()
  /**
   *  parts used to create the skill name
   */
  def parts : scala.collection.mutable.ArrayBuffer[java.lang.String] = _parts
  final private[sir] def Internal_parts = _parts
  /**
   *  parts used to create the skill name
   */
  def `parts_=`(parts : scala.collection.mutable.ArrayBuffer[java.lang.String]) : scala.Unit = { _parts = parts }
  final private[sir] def `Internal_parts_=`(v : scala.collection.mutable.ArrayBuffer[java.lang.String]) = _parts = v

  final protected var _skillname : java.lang.String = null
  /**
   *  the plain skill name
   */
  def skillname : java.lang.String = _skillname
  final private[sir] def Internal_skillname = _skillname
  /**
   *  the plain skill name
   */
  def `skillname_=`(skillname : java.lang.String) : scala.Unit = { _skillname = skillname }
  final private[sir] def `Internal_skillname_=`(v : java.lang.String) = _skillname = v

  override def prettyString : String = s"Identifier(#$skillID, parts: ${parts}, skillname: ${skillname})"

  override def getTypeName : String = "identifier"

  override def toString = "Identifier#"+skillID
}

object Identifier {
  def unapply(self : Identifier) = Some(self.parts, self.skillname)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: Identifier])
      extends Identifier(_skillID) with UnknownObject[Identifier] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}
