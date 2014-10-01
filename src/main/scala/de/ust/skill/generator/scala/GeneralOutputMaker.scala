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
import de.ust.skill.generator.common.Generator
import scala.collection.JavaConversions._
import de.ust.skill.ir.TypeContext
import de.ust.skill.ir.UserType

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden
 */
trait GeneralOutputMaker extends Generator {

  // remove special stuff
  final def setTC(tc : TypeContext) = this.IR = tc.removeSpecialDeclarations.getUsertypes.to
  var IR : List[UserType] = _

  override def getLanguageName = "scala";

  val ArrayTypeName = "scala.collection.mutable.ArrayBuffer"
  val VarArrayTypeName = "scala.collection.mutable.ArrayBuffer"
  val ListTypeName = "scala.collection.mutable.ListBuffer"
  val SetTypeName = "scala.collection.mutable.HashSet"
  val MapTypeName = "scala.collection.mutable.HashMap"

  private[scala] def header : String

  /**
   * Creates the correct PrintWriter for the argument file.
   *
   * @note the used path uses maven/sbt source placement convention
   */
  override protected def open(path : String) = {
    val f = new File(s"$outPath/src/main/scala/$packagePath$path")
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
   * creates argument list of a constructor call, not including potential skillID or braces
   */
  protected def makeConstructorArguments(t : UserType) : String
  /**
   * creates argument list of a constructor call, including a trailing comma for insertion into an argument list
   */
  protected def appendConstructorArguments(t : UserType) : String

  /**
   * turns a declaration and a field into a string writing that field into an outStream
   * @note the used iterator is "outData"
   * @note the used target OutStream is "dataChunk"
   */
  protected def writeField(d : UserType, f : Field) : String

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix() : String
  protected def packageName = packagePrefix.substring(0, packagePrefix.length - 1)

  private lazy val packagePath = if (packagePrefix.length > 0) {
    packagePrefix.replace(".", "/")
  } else {
    ""
  }
}
