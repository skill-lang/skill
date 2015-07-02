/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada2

import de.ust.skill.generator.common.Generator
import de.ust.skill.ir._
import java.io.File
import java.io.PrintWriter
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import scala.collection.mutable.MutableList
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden, Dennis Przytarski
 */
trait GeneralOutputMaker extends Generator {

  override def getLanguageName = "ada2";

  private[ada2] def header : String

  // remove special stuff for now
  final def setTC(tc : TypeContext) = this.IR = tc.removeSpecialDeclarations.removeViews.getUsertypes.to
  var IR : List[UserType] = _

  /**
   * Creates the correct PrintWriter for the argument file.
   */
  override protected def open(path : String) = {
    val f = new File(s"$outPath/src/$packagePath/$path")
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(f), "UTF-8")))
    rval.write(header)
    rval
  }

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix : String

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   * @note currently unused, because emitted names can not alias predefined types or keywords anyway
   */
  protected def escaped(target : Name) : String = escaped(target.ada)

  /**
   * Escape lonely words
   */
  protected def escapedLonely(target : String) : String;

  private final val nameCache = HashMap[Type, String]()
  protected final def name(d : Type) = nameCache.get(d).getOrElse { val r = escaped(d.getName.ada); nameCache(d) = r; r }
  protected final def name(f : Field) = escaped(f.getName.ada)

  private lazy val packagePath = if (packagePrefix.length > 0) {
    packagePrefix.replace(".", "/")
  } else {
    ""
  }

}
