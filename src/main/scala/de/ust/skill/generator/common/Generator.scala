/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.common

import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Type
import java.io.PrintWriter
import de.ust.skill.ir.Field
import de.ust.skill.ir.TypeContext
import java.nio.file.Paths
import java.io.File
import scala.collection.mutable.ListBuffer

/**
 * Use this to create a 5 lines header that looks similar in all languages.
 *
 * This corresponds to the -hN, -u, -license, -date options.
 *
 * @author Timm Felden
 */
case class HeaderInfo(
  var line1 : Option[String] = None,
  var line2 : Option[String] = None,
  var line3 : Option[String] = None,
  var license : Option[String] = None,
  var userName : Option[String] = None,
  var date : Option[String] = None)

/**
 * every code generator shares these properties.
 */
trait Generator {

  /**
   * returns the generators output language.
   */
  def getLanguageName : String;

  /**
   * Base path of the output for this generator.
   */
  var outPath : String = _;

  /**
   * Set the type context. This is a function to make clear that generators may in fact project a type context prior to
   * using it.
   */
  def setTC(tc : TypeContext) : Unit;

  var headerInfo : HeaderInfo = HeaderInfo();

  /**
   * Set output package/namespace/...
   * This is a list of Strings, each denoting a package.
   *
   * This correpsonds to the -p option.
   */
  def setPackage(names : List[String]) : Unit;

  /**
   * Sets an option to a new value.
   */
  def setOption(option : String, value : String) : Unit;

  /**
   * Prints help for language specific options.
   */
  def printHelp : Unit;

  /**
   * Provides a string representation of the default value of f.
   */
  protected def defaultValue(f : Field) : String;

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  def escaped(target : String) : String;

  /**
   * Escapes words, that appear without prefix or suffix.
   */
  def escapedLonely(target : String) : String = escaped(target)

  /**
   * Create a new file with a default header.
   */
  protected def open(path : String) : PrintWriter;

  /**
   * Util to fix Javas fucked up file handling that constantly fails for no reason.
   *
   * @param path a string
   * @returns an existing new file
   */
  protected def simpleOpenDirtyPathString(path : String) : File = {
    val ps = path.split('/').reverse.iterator;

    val rps = new ListBuffer[String];
    while (ps.hasNext) ps.next match {
      case ".." ⇒
        ps.next; rps.prepend(ps.next)
      case "." ⇒ rps.prepend(ps.next)
      case p   ⇒ rps.prepend(p)
    }

    val f = new File(rps.mkString("/"));

    f.getParentFile.mkdirs
    println(f.getAbsolutePath)
    f.createNewFile

    f
  }

  /**
   * maximum line length in emitted output
   */
  var lineLength = 80

  /**
   * Transform a comment of a declaration into the language's comment system
   */
  protected def comment(d : Declaration) : String;

  /**
   * Transform a comment of a field into the language's comment system
   */
  protected def comment(d : Field) : String;

  /**
   * Makes the output. Use trait stacking, i.e. traits must invoke super.make!!!
   *
   * This function is called after options have been set.
   */
  def make : Unit;
}
