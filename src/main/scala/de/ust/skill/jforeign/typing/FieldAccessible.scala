/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign.typing

import de.ust.skill.ir.Field
import de.ust.skill.ir.Type

class FieldAccessible(typ : Type, field : Field) extends TypeRule {

  def getType : Type = typ

  def getField : Field = field

  override def toString : String = s"${typ.getName.getFqdn}.${field.getName.getSkillName} accessible"
}
