package de.ust.skill.javacf.mapping

import de.ust.skill.ir.Type
import de.ust.skill.javacf.typing.TypeRule
import de.ust.skill.ir.UserType
import de.ust.skill.ir.Field
import de.ust.skill.javacf.typing.TypeEquation

class FieldMappingRule(fromSkillField: String, toJavaField: String) {

  override def toString(): String = s"$fromSkillField -> $toJavaField;"

  def bind(skillFieldMap: Map[String, Field], javaFieldMap: Map[String, Field]): TypeRule = {
    val skillField = skillFieldMap.get(fromSkillField).getOrElse(throw new RuntimeException(s"Field $fromSkillField not found in skill type!"))
    val javaField = javaFieldMap.get(toJavaField).getOrElse(throw new RuntimeException(s"Field $toJavaField not found in Java type!"))
    new TypeEquation(skillField.getType, javaField.getType)
  }

}