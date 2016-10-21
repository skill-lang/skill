package de.ust.skill.jforeign.typing

import de.ust.skill.ir.Field
import de.ust.skill.ir.Type

class FieldMappedOnce(typ: Type, field: Field) extends TypeRule {
  
  def getDeclaringType() = typ
  
  def getField() = field
  
  override def toString: String = s"Field ${field.getName.getSkillName} in type ${typ.getName.getSkillName} mapped once"
}