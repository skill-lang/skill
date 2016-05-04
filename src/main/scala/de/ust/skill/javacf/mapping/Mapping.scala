package de.ust.skill.javacf.mapping

import scala.collection.mutable.ListBuffer

class Mapping {

  val typeMappings = new ListBuffer[TypeMapping]

  def mapType(skill: String, java: String): TypeMapping = {
    val ntm = new TypeMapping(skill, java)
    typeMappings += ntm
    ntm
  }
  
  
  override def toString(): String = typeMappings.map(_.toString()).mkString("\n")

}