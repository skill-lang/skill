package de.ust.skill.javacf.mapping

import scala.collection.JavaConversions._
import de.ust.skill.ir.TypeContext
import de.ust.skill.javacf.typing.TypeRule
import de.ust.skill.ir.UserType
import de.ust.skill.javacf.typing.TargetTypeExists
import de.ust.skill.javacf.typing.FieldAccessible

class UnboundMappingRule(javaTypeName: String, fieldNames: List[String], total: Boolean) extends MappingRule {

  override def toString(): String = if (total)
    s"new! $javaTypeName;" else s"new $javaTypeName {\n${fieldNames.mkString(";\n")}\n}"

  override def getJavaTypeName(): String = javaTypeName;

  def bind(skill: TypeContext, java: TypeContext): List[TypeRule] = {
    if (!total) throw new RuntimeException("Not implemented: non-total unbound mappings");

    val javaType = java.get(javaTypeName)
    List(new TargetTypeExists(javaType)) ++ javaType.asInstanceOf[UserType].getFields.map { f => new FieldAccessible(javaType, f) }.toList
  }
}