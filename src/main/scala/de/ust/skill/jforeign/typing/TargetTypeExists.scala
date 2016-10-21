package de.ust.skill.jforeign.typing

import de.ust.skill.ir.Type

class TargetTypeExists(targetType: Type) extends TypeRule {
  
  def getTargetType: Type = targetType
  
  override def toString(): String = s"∃ ${targetType.getName}"
  
  
}