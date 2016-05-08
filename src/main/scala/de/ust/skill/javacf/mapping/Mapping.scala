package de.ust.skill.javacf.mapping

import scala.collection.mutable.ListBuffer
import de.ust.skill.ir.TypeContext

class Mapping {

  val typeMappings = new ListBuffer[TypeMapping]

  def mapType(skill: String, java: String): TypeMapping = {
    val ntm = new TypeMapping(skill, java)
    typeMappings += ntm
    ntm
  }
  
  def deriveEquations(fromTc: TypeContext, toTc: TypeContext): List[TypeEquation] = 
    typeMappings.flatMap { tm => tm.deriveEquations(fromTc, toTc) }.toList
  
  override def toString(): String = typeMappings.map(_.toString()).mkString("\n")

}