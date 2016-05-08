package de.ust.skill.javacf.mapping

import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._
import de.ust.skill.ir.TypeContext
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType

class TypeMapping(skillType: String, javaType: String) {

  val fieldMappings = new HashMap[String, String]

  def getSkillType: String = skillType

  def getJavaType: String = javaType

  def mapField(skill: String, java: String): Unit = fieldMappings += (skill → java)

  def deriveEquations(fromTc: TypeContext, toTc: TypeContext): List[TypeEquation] = {
    val stype = fromTc.get(skillType).asInstanceOf[UserType]
    val jtype = toTc.get(javaType).asInstanceOf[UserType]
    
    val sfields = stype.getFields.map { x => (x.getName.lower → x) }.toMap
    val jfields = jtype.getFields.map { x => (x.getName.lower → x) }.toMap

    val fieldEquations: List[TypeEquation] = fieldMappings.map(kv ⇒ {
      println(s"$kv._1 --> $kv._2")
      val sfieldtype = sfields.get(kv._1).get.getType
      val jfieldtype = jfields.get(kv._2).get.getType
      new TypeEquation(sfieldtype, jfieldtype)
    }).to
    new TypeEquation(stype, jtype) :: fieldEquations
  }

  override def toString(): String = {
    val sb = new StringBuilder
    sb.append(skillType + " -> " + javaType + "\n")
    for ((k, v) ← fieldMappings) {
      sb.append("* " + k + " -> " + v + "\n")
    }
    sb.append("\n")
    sb.toString()
  }

}
