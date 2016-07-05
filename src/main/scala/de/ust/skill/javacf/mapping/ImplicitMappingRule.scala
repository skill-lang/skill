package de.ust.skill.javacf.mapping

class ImplicitMappingRule(fromSkillType: String, toJavaType: String) extends MappingRule {

  override def toString(): String = s"implicit $fromSkillType -> $toJavaType;"
}