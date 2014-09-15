/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.doxygen

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
 * @author Timm Felden, Dennis Przytarski
 */
trait GeneralOutputMaker extends Generator {

  override def getLanguageName = "doxygen";

  private[doxygen] def header : String

  // remove special stuff for now
  final def setIR(IR : List[Declaration]) = this.IR = IR.to
  var IR : List[Declaration] = _

  /**
   * Creates the correct PrintWriter for the argument file.
   */
  override protected def open(path : String) = {
    val f = new File(s"$outPath$packagePath/$path")
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(f), "UTF-8")))
    rval.write(header)
    rval
  }

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
  protected def escaped(target : Name) : String = escaped(target.ada)

  private lazy val packagePath = if (packagePrefix.length > 0) {
    packagePrefix.replace(".", "/")
  } else {
    ""
  }
}
