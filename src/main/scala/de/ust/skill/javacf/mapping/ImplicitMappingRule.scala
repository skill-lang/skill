package de.ust.skill.javacf.mapping

class ImplicitMappingRule(fromSkillType: String, toJavaType: String, total: Boolean) extends MappingRule {

  override def toString(): String = s"${if (total) "total" else "implicit"} $fromSkillType -> $toJavaType;"
}
