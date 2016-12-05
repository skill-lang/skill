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
 *  A comment that explains types or fields.
 *  @author  Timm Felden
 */
sealed class Comment (_skillID : SkillID) extends SkillObject(_skillID) {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  private[sir] def this(_skillID : SkillID, tags : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CommentTag], text : scala.collection.mutable.ArrayBuffer[java.lang.String]) {
    this(_skillID)
    _tags = tags
    _text = text
  }

  final protected var _tags : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CommentTag] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CommentTag]()
  def tags : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CommentTag] = _tags
  final private[sir] def Internal_tags = _tags
  def `tags_=`(tags : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CommentTag]) : scala.Unit = { _tags = tags }
  final private[sir] def `Internal_tags_=`(v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CommentTag]) = _tags = v

  final protected var _text : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String]()
  def text : scala.collection.mutable.ArrayBuffer[java.lang.String] = _text
  final private[sir] def Internal_text = _text
  def `text_=`(text : scala.collection.mutable.ArrayBuffer[java.lang.String]) : scala.Unit = { _text = text }
  final private[sir] def `Internal_text_=`(v : scala.collection.mutable.ArrayBuffer[java.lang.String]) = _text = v

  override def prettyString : String = s"Comment(#$skillID, tags: ${tags}, text: ${text})"

  override def getTypeName : String = "comment"

  override def toString = "Comment#"+skillID
}

object Comment {
  def unapply(self : Comment) = Some(self.tags, self.text)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: Comment])
      extends Comment(_skillID) with UnknownObject[Comment] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}
