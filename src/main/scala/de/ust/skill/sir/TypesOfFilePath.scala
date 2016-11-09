/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 09.11.2016                               *
 * |___/_|\_\_|_|____|    by: feldentm                                        *
\*                                                                            */
package de.ust.skill.sir

import de.ust.skill.common.scala.SkillID
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.UnknownObject

/**
 *  a path that can be used in the description of a build process
 */
sealed class FilePath (_skillID : SkillID) extends SkillObject(_skillID) {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  private[sir] def this(_skillID : SkillID, isAbsolut : scala.Boolean, parts : scala.collection.mutable.ArrayBuffer[java.lang.String]) {
    this(_skillID)
    _isAbsolut = isAbsolut
    _parts = parts
  }

  final protected var _isAbsolut : scala.Boolean = false
  /**
   *  true, iff starting from file system root
   */
  def isAbsolut : scala.Boolean = _isAbsolut
  final private[sir] def Internal_isAbsolut = _isAbsolut
  /**
   *  true, iff starting from file system root
   */
  def `isAbsolut_=`(isAbsolut : scala.Boolean) : scala.Unit = { _isAbsolut = isAbsolut }
  final private[sir] def `Internal_isAbsolut_=`(v : scala.Boolean) = _isAbsolut = v

  final protected var _parts : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String]()
  /**
   *  parts of the path that will be assembled into a usable path
   */
  def parts : scala.collection.mutable.ArrayBuffer[java.lang.String] = _parts
  final private[sir] def Internal_parts = _parts
  /**
   *  parts of the path that will be assembled into a usable path
   */
  def `parts_=`(parts : scala.collection.mutable.ArrayBuffer[java.lang.String]) : scala.Unit = { _parts = parts }
  final private[sir] def `Internal_parts_=`(v : scala.collection.mutable.ArrayBuffer[java.lang.String]) = _parts = v

  override def prettyString : String = s"FilePath(#$skillID, isabsolut: ${isAbsolut}, parts: ${parts})"

  override def getTypeName : String = "filepath"

  override def toString = "FilePath#"+skillID
}

object FilePath {
  def unapply(self : FilePath) = Some(self.isAbsolut, self.parts)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: FilePath])
      extends FilePath(_skillID) with UnknownObject[FilePath] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}
