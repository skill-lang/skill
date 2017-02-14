/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign.typing

import de.ust.skill.ir.Type

class TargetTypeExists(targetType : Type) extends TypeRule {

  def getTargetType : Type = targetType

  override def toString() : String = s"∃ ${targetType.getName}"
}
