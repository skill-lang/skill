package de.ust.skill.javacf.mapping

class UnboundMappingRule(javaType: String, fieldNames: List[String]) extends MappingRule {

  override def toString(): String = s"new $javaType {\n${fieldNames.mkString(";\n")}\n}"

}