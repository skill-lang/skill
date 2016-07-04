package de.ust.skill.javacf.typing

import de.ust.skill.ir.Type

class TypeEquation(left: Type, right: Type) extends TypeRule {

  def getLeft: Type = left

  def getRight: Type = right

  override def toString(): String = s"${left.getSkillName} == ${right.getSkillName}"

}