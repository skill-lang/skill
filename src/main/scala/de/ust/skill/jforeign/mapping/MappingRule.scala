/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign.mapping

import de.ust.skill.ir.TypeContext
import de.ust.skill.jforeign.typing.TypeRule

abstract class MappingRule {

  def getJavaTypeName() : String

  def bind(skill : TypeContext, java : TypeContext) : List[TypeRule]
}
