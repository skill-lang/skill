/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 27.01.2015                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import _root_.de.ust.skill.generator.genericBinding.api.Access

/**
 * The top of the skill type hierarchy.
 * @author Timm Felden
 */
class SkillType private[genericBinding] (protected var skillID : Long) {
  final def getSkillID = skillID
  private[internal] final def setSkillID(newID : Long) = skillID = newID

  /**
   * mark an instance as deleted
   */
  final def delete = setSkillID(0)

  /**
   * checks for a deleted mark
   */
  final def markedForDeletion = 0 == getSkillID

  /**
   * provides a pretty representation of this
   */
  def prettyString : String = s"<some fully generic type#$skillID>"

  /**
   * reflective setter
   *
   * @param field a field declaration instance as obtained from the storage pools iterator
   * @param value the new value of the field
   *
   * @note if field is not a distributed field of this type, then anything may happen
   */
  final def set[@specialized T](field : FieldDeclaration[T], value : T) {
    field.setR(this, value)
  }

  /**
   * reflective getter
   *
   * @param field a field declaration instance as obtained from the storage pools iterator
   *
   * @note if field is not a distributed field of this type, then anything may happen
   */
  final def get[@specialized T](field : FieldDeclaration[T]) : T = field.getR(this)
}

object SkillType {
  final class SubType private[genericBinding] (val τPool : StoragePool[_, _], skillID : Long) extends SkillType(skillID) with NamedType {
    override def τName = τPool.name
    override def prettyString : String = τName+"(this: "+this+")"
    override def toString = τName+"#"+skillID
  }
}

trait NamedType {
  def τName : String;
}
