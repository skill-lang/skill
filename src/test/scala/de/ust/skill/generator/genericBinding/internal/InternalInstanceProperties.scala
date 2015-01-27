/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 27.01.2015                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

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
