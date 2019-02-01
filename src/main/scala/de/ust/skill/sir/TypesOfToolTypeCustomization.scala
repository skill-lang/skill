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
 *  this type is used to allow tools to define their own non-standard set of hints and restrictions
 */
sealed class ToolTypeCustomization (_skillID : SkillID) extends SkillObject(_skillID) with Annotations {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  private[sir] def this(_skillID : SkillID, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) {
    this(_skillID)
    _hints = hints
    _restrictions = restrictions
  }

  override def prettyString : String = s"ToolTypeCustomization(#$skillID, hints: ${hints}, restrictions: ${restrictions})"

  override def getTypeName : String = "tooltypecustomization"

  override def toString = "ToolTypeCustomization#"+skillID
}

object ToolTypeCustomization {
  def unapply(self : ToolTypeCustomization) = Some(self.hints, self.restrictions)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: ToolTypeCustomization])
      extends ToolTypeCustomization(_skillID) with UnknownObject[ToolTypeCustomization] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}
