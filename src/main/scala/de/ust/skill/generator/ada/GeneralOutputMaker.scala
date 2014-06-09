/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import java.io.File
import java.io.PrintWriter
import de.ust.skill.ir._
import java.util.Date
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import scala.collection.mutable.MutableList

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden, Dennis Przytarski
 */
trait GeneralOutputMaker {

  /**
   * The base path of the output.
   */
  var outPath: String

  /**
   * The intermediate representation of the (known) output type system.
   */
  var IR: List[Declaration]

  /**
   * Makes the output; has to invoke super.make!!!
   */
  def make: Unit;

  private[ada] def header: String

  /**
   * Creates the correct PrintWriter for the argument file.
   */
  protected def open(path: String) = {
    val f = new File(s"$outPath$packagePath/$path")
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(f), "UTF-8")))
    rval.write(header)
    rval
  }

  /**
   * Assume the existence of a translation function for the types.
   */
  protected def mapTypeToId(t: Type, f: Field): String
  protected def mapType(t : Type, d: Declaration, f: Field): String

  /**
   * Assume the existence of a translation function for the fields.
   */
  protected def mapFileReader(d: Declaration, f: Field): String
  protected def mapFileWriter(d: Declaration, f: Field): String

  /**
   * Assume the existence of inheritance information functions for the types.
   */
  protected def getSuperTypes(d: Declaration): MutableList[Type]
  protected def getSubTypes(d: Declaration): MutableList[Type]

  /**
   * Assume the existence of the get field parameters function.
   */
  protected def printParameters(d : Declaration): String

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix: String

  /**
   * Provides a string representation of the default value of f.
   */
  protected def defaultValue(t: Type, d: Declaration, f: Field): String

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  protected def escaped(target: String): String

  private lazy val packagePath = if (packagePrefix.length > 0) {
    packagePrefix.replace(".", "/")
  } else {
    ""
  }
}
