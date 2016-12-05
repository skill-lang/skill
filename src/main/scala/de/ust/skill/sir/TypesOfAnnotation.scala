/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 05.12.2016                               *
 * |___/_|\_\_|_|____|    by: feldentm                                        *
\*                                                                            */
package de.ust.skill.sir

import de.ust.skill.common.scala.SkillID
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.UnknownObject

/**
 *  anything that can receive annotations
 */
trait Annotations extends SkillObject {
  final protected var _hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint]()
  def hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint] = _hints
  final private[sir] def Internal_hints = _hints
  def `hints_=`(hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint]) : scala.Unit = { _hints = hints }
  final private[sir] def `Internal_hints_=`(v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint]) = _hints = v

  final protected var _restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]()
  def restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction] = _restrictions
  final private[sir] def Internal_restrictions = _restrictions
  def `restrictions_=`(restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) : scala.Unit = { _restrictions = restrictions }
  final private[sir] def `Internal_restrictions_=`(v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) = _restrictions = v

}        
