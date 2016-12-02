/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 01.12.2016                               *
 * |___/_|\_\_|_|____|    by: feldentm                                        *
\*                                                                            */
package de.ust.skill.sir

import de.ust.skill.common.scala.SkillID
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.UnknownObject

sealed class BuildInformation (_skillID : SkillID) extends SkillObject(_skillID) {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  private[sir] def this(_skillID : SkillID, language : java.lang.String, options : scala.collection.mutable.ArrayBuffer[java.lang.String], output : _root_.de.ust.skill.sir.FilePath) {
    this(_skillID)
    _language = language
    _options = options
    _output = output
  }

  final protected var _language : java.lang.String = null
  /**
   *  the name of the language to be used. It is explicitly discouraged to use all languages. Create different build
   *  informations for every language instead, as the output directory should be changed.
   */
  def language : java.lang.String = _language
  final private[sir] def Internal_language = _language
  /**
   *  the name of the language to be used. It is explicitly discouraged to use all languages. Create different build
   *  informations for every language instead, as the output directory should be changed.
   */
  def `language_=`(language : java.lang.String) : scala.Unit = { _language = language }
  final private[sir] def `Internal_language_=`(v : java.lang.String) = _language = v

  final protected var _options : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String]()
  /**
   *  options are processed as if they were entered in the command line interface
   *  @note  options consisting of multiple strings will be stored in multiple strings in this form
   */
  def options : scala.collection.mutable.ArrayBuffer[java.lang.String] = _options
  final private[sir] def Internal_options = _options
  /**
   *  options are processed as if they were entered in the command line interface
   *  @note  options consisting of multiple strings will be stored in multiple strings in this form
   */
  def `options_=`(options : scala.collection.mutable.ArrayBuffer[java.lang.String]) : scala.Unit = { _options = options }
  final private[sir] def `Internal_options_=`(v : scala.collection.mutable.ArrayBuffer[java.lang.String]) = _options = v

  final protected var _output : _root_.de.ust.skill.sir.FilePath = null
  /**
   *  the output directory passed to the target generator
   */
  def output : _root_.de.ust.skill.sir.FilePath = _output
  final private[sir] def Internal_output = _output
  /**
   *  the output directory passed to the target generator
   */
  def `output_=`(output : _root_.de.ust.skill.sir.FilePath) : scala.Unit = { _output = output }
  final private[sir] def `Internal_output_=`(v : _root_.de.ust.skill.sir.FilePath) = _output = v

  override def prettyString : String = s"BuildInformation(#$skillID, language: ${language}, options: ${options}, output: ${output})"

  override def getTypeName : String = "buildinformation"

  override def toString = "BuildInformation#"+skillID
}

object BuildInformation {
  def unapply(self : BuildInformation) = Some(self.language, self.options, self.output)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: BuildInformation])
      extends BuildInformation(_skillID) with UnknownObject[BuildInformation] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}
