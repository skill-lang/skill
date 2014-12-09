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
   * Rename package prefix; we may change the implementation in the future.
   */
  protected def prefix = packagePrefix

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

  /**
   * Creates instance constructor arguments excluding the state
   */
  protected def makeConstructorArguments(t : UserType) : String

  /**
   * flag that controls omission of runtime safety checks
   */
  protected var unsafe = false

  /**
   * provides a default name for the argument type
   */
  protected def name(t : Type) : String = if (null == t) "skill_type"
  else escaped(t.getName.cStyle)
  /**
   * provides a default name for the argument field
   */
  protected def name(f : Field) : String = escaped(f.getName.cStyle)

  /**
   * provides field access implementation
   */
  protected def access(f : Field, instance : String = "instance") : String = s"((${prefix}${name(f.getDeclaredIn)})instance)->${name(f)}"

  /**
   * creates a type cast on user types
   * @note if t is null, then skill_type will be used
   */
  protected def cast(t : Type = null) : String = if (null == t) s"(${prefix}skill_type)"
  else s"(${prefix}${name(t)})"
}
