package de.ust.skill.parser

import java.io.File
import de.ust.skill.ir.Comment

// TODO merge decls of Description

abstract class SIDLDefinition(val name: Name)

case class AddedField(
  val comment: Comment,
  _name: Name,
  val fields: List[AbstractField],
  val file: File
  ) extends SIDLDefinition(_name)

case class SIDLUserType(
  val declaredIn : File,
  val description : Description,
  _name : Name,
  val subTypes : List[Name],
  ) extends SIDLDefinition(_name)

case class SIDLEnum(
  val declaredIn : File,
  val comment : Comment,
  _name : Name,
  val instances : List[Name],
  ) extends SIDLDefinition(_name)

case class SIDLInterface(
  val declaredIn : File,
  val comment : Comment,
  _name : Name,
  val subTypes : List[Name],
  ) extends SIDLDefinition(_name)

case class SIDLTypedef(typedef : Typedef) extends SIDLDefinition(typedef.name)