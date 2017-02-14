/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign.mapping

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir.Field
import de.ust.skill.ir.TypeContext
import de.ust.skill.ir.UserType
import de.ust.skill.jforeign.typing.FieldAccessible
import de.ust.skill.jforeign.typing.FieldMappedOnce
import de.ust.skill.jforeign.typing.TargetTypeExists
import de.ust.skill.jforeign.typing.TypeRule

class UnboundMappingRule(javaTypeName : String, fieldNames : List[String], total : Boolean) extends MappingRule {

  override def toString() : String = if (total)
    s"new! $javaTypeName;" else s"new $javaTypeName {\n${fieldNames.mkString(";\n")}\n}"

  override def getJavaTypeName() : String = javaTypeName;

  def bind(skill : TypeContext, java : TypeContext) : List[TypeRule] = {
    val javaType = java.get(javaTypeName)
    val allFields = javaType.asInstanceOf[UserType].getFields
    val mappedFields : List[Field] = if (total) allFields.toList else {
      val fields : Set[String] = fieldNames.toSet
      allFields.filter { fn ⇒ fields.contains(fn.getSkillName) }.toList
    }
    List(new TargetTypeExists(javaType)) ++
      mappedFields.map { f ⇒ new FieldAccessible(javaType, f) }.toList ++
      mappedFields.map { f ⇒ new FieldMappedOnce(javaType, f) }.toList
  }
}
