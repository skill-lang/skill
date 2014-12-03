/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c

import de.ust.skill.ir._
import java.io.File
import java.io.PrintWriter
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import scala.collection.mutable.MutableList
import de.ust.skill.generator.common.Generator

import scala.collection.JavaConversions._

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden, Fabian Harth
 */
trait GeneralOutputMaker extends Generator {

  override def getLanguageName = "c";

  private[c] def header : String

  // remove special stuff for now
  final def setTC(tc : TypeContext) = this.IR = tc.removeSpecialDeclarations.getUsertypes.to
  var IR : List[UserType] = _

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
   * @note currently unused, because emitted names can not alias predefined types or keywords anyway
   */
  protected def escaped(target : Name) : String = escaped(target.ada)

  private lazy val packagePath = if (packagePrefix.length > 0) {
    packagePrefix.replace(".", "_")
  } else {
    ""
  }
}
