package de.ust.skill.javacf.mapping

import scala.collection.mutable.HashMap

class TypeMapping(skillType: String, javaType: String) {

  val fieldMappings = new HashMap[String, String]

  def getSkillType: String = skillType

  def getJavaType: String = javaType

  def mapField(skill: String, java: String): Unit = fieldMappings += (skill → java)

  override def toString(): String = {
    val sb = new StringBuilder
    sb.append(skillType + " -> " + javaType + "\n")
    for ((k,v) ← fieldMappings) {
      sb.append("* " + k + " -> " + v + "\n")
    }
    sb.append("\n")
    sb.toString()
  }
 
}
