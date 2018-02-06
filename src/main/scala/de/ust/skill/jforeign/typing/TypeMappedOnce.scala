/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign.typing

import de.ust.skill.ir.Type

class TypeMappedOnce(typ : Type) extends TypeRule {

  def getType : Type = typ

  override def toString() : String = s"Type ${typ.getName.getSkillName} mapped once"
}
