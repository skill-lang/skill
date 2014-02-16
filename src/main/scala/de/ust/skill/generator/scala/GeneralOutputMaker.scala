/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.io.File
import java.io.PrintWriter
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Type
import de.ust.skill.ir.Field
import java.util.Date
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden
 */
trait GeneralOutputMaker {

  val ArrayTypeName = "scala.collection.mutable.ArrayBuffer"
  val VarArrayTypeName = "scala.collection.mutable.ArrayBuffer"
  val ListTypeName = "scala.collection.mutable.ListBuffer"
  val SetTypeName = "scala.collection.mutable.HashSet"
  val MapTypeName = "scala.collection.mutable.HashMap"

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

  private[scala] def header: String

  /**
   * Creates the correct PrintWriter for the argument file.
   */
  protected def open(path: String) = {
    val f = new File(s"$outPath$packagePath$path")
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
  protected def mapType(t: Type): String

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix(): String

  /**
   * Provides a string representation of the default value of f.
   */
  protected def defaultValue(f: Field): String

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
