/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api

import de.ust.skill.generator.scala.GeneralOutputMaker

trait GenericTypeMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/GenericType.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}api

/**
 * Any unknown object is instantiated by this type.
 *
 * @note not yet implemented; this will get eventually provide setters getters and a way to retrieve type info for fields
 *
 * @author Timm Felden
 */
class GenericType extends SkillType {

  override def getSkillID = 0L

  override def setSkillID(id: Long) = throw new NoSuchMethodError("you can not set the SKilL ID of a generic instance")

  def prettyString: String = "<<some generic objcet>>"

}
""")

    out.close()
  }
}
