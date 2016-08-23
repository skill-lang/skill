package de.ust.skill.javacf.mapping

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import de.ust.skill.ir.TypeContext
import de.ust.skill.javacf.typing.TypeRule
import de.ust.skill.javacf.typing.TypeEquation
import de.ust.skill.ir.UserType

class ExplicitMappingRule(fromSkillType: String, toJavaType: String, fieldMappings: List[FieldMappingRule])
    extends MappingRule {

  override def toString(): String = {
    s"map $fromSkillType -> $toJavaType {\n${fieldMappings.mkString("\n")}\n}"
  }

  override def getJavaTypeName(): String = toJavaType;

  override def bind(skill: TypeContext, java: TypeContext): List[TypeRule] = {
    val stype = skill.get(fromSkillType.toLowerCase())
    val jtype = java.get(toJavaType)

    if (stype == null)
      throw new RuntimeException(s"$fromSkillType is not defined in any skill file, invalid mapping $fromSkillType -> $toJavaType")
    if (jtype == null)
      throw new RuntimeException(s"$toJavaType not found, invalid mapping $fromSkillType -> $toJavaType")
    val fieldrules = if (stype.isInstanceOf[UserType] && jtype.isInstanceOf[UserType]) {
      val skilltype = stype.asInstanceOf[UserType];
      val javatype = jtype.asInstanceOf[UserType];

      val skillFieldMap = skilltype.getFields.toList.map { f => (f.getName.toString → f) }.toMap
      val javaFieldMap = javatype.getFields.toList.map { f => (f.getName.toString → f) }.toMap

      fieldMappings.flatMap {
        _.bind(skillFieldMap, javaFieldMap, javatype)
      }.toList
    } else List[TypeRule]()
    new TypeEquation(stype, jtype) :: fieldrules
  }
}