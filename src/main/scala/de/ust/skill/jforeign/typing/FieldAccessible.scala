package de.ust.skill.jforeign.typing

import de.ust.skill.ir.Field
import de.ust.skill.ir.Type

class FieldAccessible(typ: Type, field: Field) extends TypeRule {

  def getType: Type = typ

  def getField: Field = field

  override def toString: String = s"${typ.getName.getFqdn}.${field.getName.getSkillName} accessible"

}