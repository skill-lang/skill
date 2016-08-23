package de.ust.skill.javacf.mapping

import de.ust.skill.ir.Type
import de.ust.skill.javacf.typing.TypeRule
import de.ust.skill.ir.UserType
import de.ust.skill.ir.Field
import de.ust.skill.javacf.typing.TypeEquation
import de.ust.skill.javacf.typing.FieldAccessible

class FieldMappingRule(fromSkillField: String, toJavaField: String) {

  override def toString(): String = s"$fromSkillField -> $toJavaField;"

  def bind(skillFieldMap: Map[String, Field], javaFieldMap: Map[String, Field], javaType: Type): List[TypeRule] = {
    val skillField = skillFieldMap.get(fromSkillField).getOrElse(throw new RuntimeException(s"Field $fromSkillField not found in skill type!"))
    val javaField = javaFieldMap.get(toJavaField).getOrElse(throw new RuntimeException(s"Field $toJavaField not found in Java type!"))
    List(new TypeEquation(skillField.getType, javaField.getType), new FieldAccessible(javaType, javaField))
  }

}