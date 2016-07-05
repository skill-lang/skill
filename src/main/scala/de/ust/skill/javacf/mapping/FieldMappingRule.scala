package de.ust.skill.javacf.mapping

class FieldMappingRule(fromSkillField: String, toJavaField: String) {

  override def toString(): String = s"$fromSkillField -> $toJavaField;"

}