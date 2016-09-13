package de.ust.skill.jforeign.mapping

import de.ust.skill.ir.Type
import de.ust.skill.jforeign.typing.TypeRule
import de.ust.skill.ir.UserType
import de.ust.skill.ir.Field
import de.ust.skill.jforeign.typing.TypeEquation
import de.ust.skill.jforeign.typing.FieldAccessible
import de.ust.skill.jforeign.typing.FieldMappedOnce
import de.ust.skill.jforeign.typing.FieldMappedOnce

class FieldMappingRule(fromSkillField: String, toJavaField: String) {

  override def toString(): String = s"$fromSkillField -> $toJavaField;"

  def bind(skillFieldMap: Map[String, Field], javaFieldMap: Map[String, Field], skillType: Type, javaType: Type): List[TypeRule] = {

    val skillField = skillFieldMap.get(fromSkillField).
      getOrElse(throw new RuntimeException(s"Field $fromSkillField not found in skill type!"))
    val javaField = javaFieldMap.get(toJavaField).
      getOrElse(throw new RuntimeException(s"Field $toJavaField not found in Java type!"))

    List(new TypeEquation(skillField.getType, javaField.getType),
      new FieldAccessible(javaType, javaField),
      new FieldMappedOnce(skillType, skillField),
      new FieldMappedOnce(javaType, javaField))
  }

}