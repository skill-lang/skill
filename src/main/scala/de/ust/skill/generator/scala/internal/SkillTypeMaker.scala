/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait SkillTypeMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val packageName = if(this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.')+1) else this.packageName;
    val out = open("internal/SkillType.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import ${packagePrefix}api.Access

/**
 * The top of the skill type hierarchy.
 * @author Timm Felden
 */
abstract sealed class SkillType private[$packageName] (protected var skillID : Int) {
  @inline private[internal] final def getSkillID = skillID
  @inline private[internal] final def setSkillID(newID : Int) = skillID = newID
  protected[internal] def myPool : StoragePool[_ <: SkillType, _ <: SkillType]

  /**
   * mark an instance as deleted. After a call to delete, this instance should no longer be used.
   */
  def delete : Unit

  /**
   * checks for a deleted mark
   */
  def markedForDeletion : Boolean

  /**
   * provides a pretty representation of this
   */
  def prettyString : String = s"<some fully generic type#$$skillID>"

  /**
   * reflective setter
   */
  def set[@specialized T](acc : Access[_ <: SkillType], field : FieldDeclaration, value : T) {
    acc.asInstanceOf[StoragePool[_ <: SkillType, _ <: SkillType]].unknownFieldData(field).put(skillID, value)
  }

  /**
   * reflective getter
   */
  def get(acc : Access[_ <: SkillType], field : FieldDeclaration) : Any = {
    try {
      acc.asInstanceOf[StoragePool[_ <: SkillType, _ <: SkillType]].unknownFieldData(field)(skillID)
    } catch {
      case e : Exception ⇒ this+" is not in:\\n"+acc.asInstanceOf[StoragePool[_ <: SkillType, _ <: SkillType]].unknownFieldData(field).mkString("\\n")
    }
  }

  /**
   * equality of SkillType instances
   */
  override def equals(obj : Any) = obj match {
    case st : SkillType ⇒ skillID == st.skillID && myPool.basePool == st.myPool.basePool
    case _ ⇒ false
  }
  /**
   * hash code function
   */
  override def hashCode = skillID.hashCode ^ myPool.basePool.typeID.hashCode
}
object SkillType {
  implicit class ReferenceHelper(val x : SkillType) extends AnyVal {
    @inline private[internal] final def annotation : Long = if (x == null) 0L else (x.myPool.poolIndex.toLong << 32) | x.getSkillID
    @inline private[internal] final def reference : Int = if (x == null) 0 else x.getSkillID
  }
}

/**
 * Base class for all known skill types (accessible through state fields)
 * @author Jonathan Roth
 */
abstract class KnownSkillType private[$packageName] (_skillID : Int) extends SkillType(_skillID) {
  protected def basePool : BasePool[_ <: SkillType]
  
  final override def delete = basePool.removeByID(skillID)
  final override def markedForDeletion = basePool.isIDRemoved(skillID)

  /**
   * equality of SkillType instances
   */
  final override def equals(obj : Any) = obj match {
    case kst : KnownSkillType ⇒ skillID == kst.skillID && basePool == kst.basePool
    case st : SkillType ⇒ skillID == st.getSkillID && basePool == st.myPool.basePool
    case _ ⇒ false
  }
  /**
   * hash code function
   */
  final override def hashCode = skillID.hashCode ^ basePool.typeID.hashCode
}
/**
 * Represents unknown skill types read from a file. Only reflective field access is possible.
 * @author Jonathan Roth
 */
final class UnknownSkillType private[$packageName] (_skillID : Int, val pool : StoragePool[_ <: SkillType, _ <: SkillType]) extends SkillType(_skillID) {
  protected[internal] override def myPool = pool
  
  override def delete = pool.basePool.removeByID(skillID)
  override def markedForDeletion = pool.basePool.isIDRemoved(skillID)
}
""")

    out.close()
  }
}
