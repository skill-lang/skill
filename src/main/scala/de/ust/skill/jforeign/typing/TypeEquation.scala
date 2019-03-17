/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign.typing

import de.ust.skill.ir.Type

class TypeEquation(left : Type, right : Type) extends TypeRule {

  def getLeft : Type = left

  def getRight : Type = right

  override def toString() : String =
    s"${if (left != null) left.getSkillName else "null"} == ${if (right != null) right.getSkillName else "null"}"
}
