/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait InternalInstancePropertiesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/InternalInstanceProperties.scala")
    //package
    out.write(s"""package ${packagePrefix}internal""")
    out.write("""

/**
 * properties that are required on each instance, but are not exported through the interface
 *
 * @author Timm Felden
 */
trait InternalInstanceProperties {

  /**
   * mark an instance as deleted
   */
  final def delete = setSkillID(0)

  /**
   * checks for a deleted mark
   */
  final def markedForDeletion = 0 == getSkillID

  /**
   * @return the ID of the instance; if -1, no ID has been assigned yet, if 0, the object will not be written to disk
   */
  private[internal] def getSkillID: Long
  private[internal] def setSkillID(newID: Long): Unit
}
""")

    //class prefix
    out.close()
  }
}
