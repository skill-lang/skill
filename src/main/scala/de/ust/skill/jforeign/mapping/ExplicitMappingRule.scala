package de.ust.skill.jforeign.mapping

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import de.ust.skill.ir.TypeContext
import de.ust.skill.jforeign.typing.TypeRule
import de.ust.skill.jforeign.typing.TypeEquation
import de.ust.skill.ir.UserType
import de.ust.skill.jforeign.typing.TypeMappedOnce

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

    jtype.getName.setInternalName(fromSkillType)

    val fieldrules = if (stype.isInstanceOf[UserType] && jtype.isInstanceOf[UserType]) {
      val skilltype = stype.asInstanceOf[UserType];
      val javatype = jtype.asInstanceOf[UserType];

      val skillFieldMap = skilltype.getFields.toList.map { f => (f.getName.toString → f) }.toMap
      val javaFieldMap = javatype.getFields.toList.map { f => (f.getName.toString → f) }.toMap

      fieldMappings.flatMap {
        _.bind(skillFieldMap, javaFieldMap, skilltype, javatype)
      }.toList
    } else List[TypeRule]()

    val parentEquality = (stype, jtype) match {
      case (s: UserType, j: UserType) ⇒ Some(new TypeEquation(s.getSuperType, j.getSuperType))
      case _ ⇒ None
    }

    new TypeMappedOnce(stype) :: new TypeMappedOnce(jtype) :: new TypeEquation(stype, jtype) :: fieldrules ++ parentEquality
  }
}