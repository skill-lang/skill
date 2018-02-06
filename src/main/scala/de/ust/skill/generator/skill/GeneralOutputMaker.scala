/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.skill

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

import scala.collection.mutable.HashSet

import de.ust.skill.generator.common.Generator
import de.ust.skill.ir.Name
import de.ust.skill.ir.Type
import de.ust.skill.ir.TypeContext
import de.ust.skill.main.HeaderInfo

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden
 */
trait GeneralOutputMaker extends Generator {

  // droppable IR kinds
  sealed abstract class Droppable;
  object Interfaces extends Droppable;
  object Enums extends Droppable;
  object Typedefs extends Droppable;

  // by default, nothing is dropped
  var droppedKinds = HashSet[Droppable]();

  override def getLanguageName : String = "skill";

  // remove special stuff for now
  final def setTC(tc : TypeContext) { this.tc = tc }
  var tc : TypeContext = _

  // no header required here
  override def makeHeader(headerInfo : HeaderInfo) : String = ""

  /**
   * Assume the existence of a translation function for types.
   */
  protected def mapType(t : Type) : String

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix : String

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  protected def escaped(target : Name) : String = target.camel

  private lazy val packagePath = if (packagePrefix.length > 0) {
    "/" + packagePrefix.replace(".", "/")
  } else {
    ""
  }
}
