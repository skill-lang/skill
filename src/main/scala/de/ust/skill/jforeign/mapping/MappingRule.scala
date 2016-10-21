package de.ust.skill.jforeign.mapping

import de.ust.skill.ir.TypeContext
import de.ust.skill.jforeign.typing.TypeRule

abstract class MappingRule {

  def getJavaTypeName(): String;

  def bind(skill: TypeContext, java: TypeContext): List[TypeRule];

}