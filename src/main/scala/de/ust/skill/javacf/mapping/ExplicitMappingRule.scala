package de.ust.skill.javacf.mapping

import scala.collection.mutable.ListBuffer
import de.ust.skill.ir.TypeContext
import de.ust.skill.javacf.typing.TypeRule

class ExplicitMappingRule(fromSkillType: String, toJavaType: String, fieldMappings: List[FieldMappingRule])
    extends MappingRule {

  override def toString(): String = {
    s"map $fromSkillType -> $toJavaType {\n${fieldMappings.mkString("\n")}\n}"
  }

}