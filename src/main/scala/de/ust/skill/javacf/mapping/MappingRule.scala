package de.ust.skill.javacf.mapping

import de.ust.skill.ir.TypeContext
import de.ust.skill.javacf.typing.TypeRule

abstract class MappingRule {

  def getJavaTypeName(): String;

  def bind(skill: TypeContext, java: TypeContext): List[TypeRule];

}