/*  ___ _  ___ _ _                                                                                                    *\
** / __| |/ (_) | |     Your SKilL scala Binding                                                                      **
** \__ \ ' <| | | |__   generated: 01.02.2019                                                                         **
** |___/_|\_\_|_|____|  by: feldentm                                                                                  **
\*                                                                                                                    */
package de.ust.skill.sir

import de.ust.skill.common.scala.SkillID
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.UnknownObject

/**
 *  an option passed to a custom field
 */
sealed class CustomFieldOption (_skillID : SkillID) extends SkillObject(_skillID) {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  private[sir] def this(_skillID : SkillID, arguments : scala.collection.mutable.ArrayBuffer[java.lang.String], name : java.lang.String) {
    this(_skillID)
    _arguments = arguments
    _name = name
  }

  final protected var _arguments : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String]()
  def arguments : scala.collection.mutable.ArrayBuffer[java.lang.String] = _arguments
  final private[sir] def Internal_arguments = _arguments
  def `arguments_=`(arguments : scala.collection.mutable.ArrayBuffer[java.lang.String]) : scala.Unit = { _arguments = arguments }
  final private[sir] def `Internal_arguments_=`(v : scala.collection.mutable.ArrayBuffer[java.lang.String]) = _arguments = v

  final protected var _name : java.lang.String = null
  def name : java.lang.String = _name
  final private[sir] def Internal_name = _name
  def `name_=`(name : java.lang.String) : scala.Unit = { _name = name }
  final private[sir] def `Internal_name_=`(v : java.lang.String) = _name = v

  override def prettyString : String = s"CustomFieldOption(#$skillID, arguments: ${arguments}, name: ${name})"

  override def getTypeName : String = "customfieldoption"

  override def toString = "CustomFieldOption#"+skillID
}

object CustomFieldOption {
  def unapply(self : CustomFieldOption) = Some(self.arguments, self.name)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: CustomFieldOption])
      extends CustomFieldOption(_skillID) with UnknownObject[CustomFieldOption] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}
