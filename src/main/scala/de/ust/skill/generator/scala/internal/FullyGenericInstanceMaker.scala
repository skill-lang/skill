/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait FullyGenericInstanceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/FullyGenericInstance.scala")
    //package
    out.write(s"""package ${packagePrefix}internal

import ${packagePrefix}api._

/**
 * @author Timm Felden
 */
final class FullyGenericInstance(name: String, var skillID: Long = 0L) extends SkillType {
  override def getSkillID = skillID
  override def setSkillID(newID: Long) = skillID = newID

  /**
   * provides a pretty representation of this
   */
  def prettyString: String = s"<fully generic $$name#$$skillID>"
  override def toString = prettyString

  /**
   * reflective setter
   */
  def set[@specialized T](acc: Access[_ <: SkillType], field: ${packagePrefix}api.FieldDeclaration, value: T) {
    acc.asInstanceOf[StoragePool[SkillType]].unknownFieldData(field.asInstanceOf[FieldDeclaration]).put(this, value)
  }

  /**
   * reflective getter
   */
  def get(acc: Access[_ <: SkillType], field: ${packagePrefix}api.FieldDeclaration): Any = {
    try {
      acc.asInstanceOf[StoragePool[SkillType]].unknownFieldData(field.asInstanceOf[FieldDeclaration])(this)
    } catch {
      ${"""case e: Exception ⇒ this+" is not in:\n"+acc.asInstanceOf[StoragePool[SkillType]].unknownFieldData(field.asInstanceOf[FieldDeclaration]).mkString("\n")"""}
    }
  }
}
""")

    //class prefix
    out.close()
  }
}
