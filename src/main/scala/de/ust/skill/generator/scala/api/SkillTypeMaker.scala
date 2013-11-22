/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api

import de.ust.skill.generator.scala.GeneralOutputMaker

trait SkillTypeMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/SkillType.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}api

/**
 * The top of the skill type hierarchy.
 * @author Timm Felden
 */
trait SkillType {

  /**
   * @return the ID of the instance; if -1, no ID has been assigned yet, if 0, the object will not be written to disk
   */
  def getID: Long

  /**
   * provides a pretty representation of this
   */
  def prettyString: String

}
""")

    out.close()
  }
}
