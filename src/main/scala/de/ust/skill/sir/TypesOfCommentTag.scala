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
 *  The tag part of a comment. Can be used for easy inspection of comments.
 */
sealed class CommentTag (_skillID : SkillID) extends SkillObject(_skillID) {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  private[sir] def this(_skillID : SkillID, name : java.lang.String, text : scala.collection.mutable.ArrayBuffer[java.lang.String]) {
    this(_skillID)
    _name = name
    _text = text
  }

  final protected var _name : java.lang.String = null
  def name : java.lang.String = _name
  final private[sir] def Internal_name = _name
  def `name_=`(name : java.lang.String) : scala.Unit = { _name = name }
  final private[sir] def `Internal_name_=`(v : java.lang.String) = _name = v

  final protected var _text : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String]()
  def text : scala.collection.mutable.ArrayBuffer[java.lang.String] = _text
  final private[sir] def Internal_text = _text
  def `text_=`(text : scala.collection.mutable.ArrayBuffer[java.lang.String]) : scala.Unit = { _text = text }
  final private[sir] def `Internal_text_=`(v : scala.collection.mutable.ArrayBuffer[java.lang.String]) = _text = v

  override def prettyString : String = s"CommentTag(#$skillID, name: ${name}, text: ${text})"

  override def getTypeName : String = "commenttag"

  override def toString = "CommentTag#"+skillID
}

object CommentTag {
  def unapply(self : CommentTag) = Some(self.name, self.text)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: CommentTag])
      extends CommentTag(_skillID) with UnknownObject[CommentTag] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}
