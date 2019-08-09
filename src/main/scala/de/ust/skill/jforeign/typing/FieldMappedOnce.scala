/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign.typing

import de.ust.skill.ir.Field
import de.ust.skill.ir.Type

class FieldMappedOnce(typ : Type, field : Field) extends TypeRule {

  def getDeclaringType : Type = typ

  def getField : Field = field

  override def toString : String = s"Field ${field.getName.getSkillName} in type ${typ.getName.getSkillName} mapped once"
}
