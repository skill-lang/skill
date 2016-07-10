package de.ust.skill.javacf.mapping

class UnboundMappingRule(javaType: String, fieldNames: List[String], total: Boolean) extends MappingRule {

  override def toString(): String = if (total)
    s"new! $javaType;" else s"new $javaType {\n${fieldNames.mkString(";\n")}\n}"

}