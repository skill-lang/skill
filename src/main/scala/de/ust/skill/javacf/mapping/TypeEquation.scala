package de.ust.skill.javacf.mapping

import de.ust.skill.ir.Type

class TypeEquation(left: Type, right: Type) {

  def getLeft: Type = left

  def getRight: Type = right

  override def toString(): String = s"${left.getSkillName} == ${right.getSkillName}"

}