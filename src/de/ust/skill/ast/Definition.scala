package de.ust.skill.ast

class Definition(
  val description: Any, mod: (Boolean, Boolean),
  val name: String, val parent: Option[String], val body: Any) extends Node {

}