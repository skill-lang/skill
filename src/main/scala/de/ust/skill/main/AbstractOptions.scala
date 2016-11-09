/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.main

import java.io.File

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import de.ust.skill.BuildInfo
import de.ust.skill.generator.common.HeaderInfo
import de.ust.skill.generator.common.KnownGenerators
import de.ust.skill.ir.TypeContext
import de.ust.skill.parser.Parser

/**
 * abstract properties, that may be accessed from any option
 */
class AbstractOptions {

  /**
   * configurable exit method called on error
   * @note the purpose of changing this is to replace it with a failure method in unit tests
   */
  var exit : String ⇒ Unit = { s ⇒ System.err.println(s); System.exit(0) }

  /**
   * print an error message and quit
   */
  def error(msg : String) : Nothing = {
    exit(msg)
    ???
  }

  def checkEscaping(language : String, args : Array[String]) : String = {
    val generator = KnownGenerators.all.map(_.newInstance).collect({
      case g if g.getLanguageName == language.toLowerCase ⇒ g
    }).head
    args.map { s ⇒ s != generator.escapedLonely(s) }.mkString(" ")
  }

  /**
   * returns an array containing all language names reported by known generators
   */
  lazy val allGeneratorNames : Array[String] = {
    KnownGenerators.all.map(_.newInstance.getLanguageName).to
  }

  /**
   * all known help texts
   */
  lazy val knownHelpTexts = KnownGenerators.all.map(_.newInstance).map { g ⇒ g.getLanguageName -> g.helpText }.toMap
}
trait WithProcess {
  def process;
}