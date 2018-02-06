/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign.mapping

import de.ust.skill.ir.TypeContext
import de.ust.skill.jforeign.typing.TypeRule

class ImplicitMappingRule(fromSkillType : String, toJavaType : String, total : Boolean) extends MappingRule {

  override def toString() : String = s"${if (total) "implicit!" else "implicit"} $fromSkillType -> $toJavaType;"

  override def getJavaTypeName() : String = toJavaType;

  def bind(skill : TypeContext, java : TypeContext) : List[TypeRule] = {
    throw new RuntimeException("implicit mapping not implemented.")
  }
}
