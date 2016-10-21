package de.ust.skill.jforeign.typing

import de.ust.skill.jforeign.mapping.MappingRule
import de.ust.skill.ir.Type

class TypeMappedOnce(typ: Type) extends TypeRule {
  
  def getType() = typ
  
  override def toString() : String = s"Type ${typ.getName.getSkillName} mapped once"
}