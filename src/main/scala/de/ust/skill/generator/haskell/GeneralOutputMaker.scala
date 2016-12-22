/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.haskell

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.common.Generator
import de.ust.skill.ir.ContainerType
import de.ust.skill.ir.Field
import de.ust.skill.ir.Name
import de.ust.skill.ir.Type
import de.ust.skill.ir.TypeContext
import de.ust.skill.ir.UserType

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden, Fabian Harth
 */
trait GeneralOutputMaker extends Generator {

  override def getLanguageName : String = "haskell";

  override def clean { println("clean not supported by haskell") }

  private[haskell] def header : String

  // remove special stuff for now
  final def setTC(tc : TypeContext) { this.IR = tc.removeSpecialDeclarations.getUsertypes.to }
  var IR : List[UserType] = _

  /**
   * Assume the existence of a translation function for types.
   * Comes in two flavors depending on whether references can
   * be followed at the target location.
   */
  protected def mapType(t : Type, followReferences : Boolean) : String
  
  /**
   * create a data constructor for the boxed form of a value
   */
  protected def BoxedDataConstructor(t : Type) : String

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix : String

  /**
   * Rename package prefix; we may change the implementation in the future.
   */
  protected def prefix = if (packagePrefix.isEmpty()) "" else packagePrefix + "_"

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   * @note currently unused, because emitted names can not alias predefined types or keywords anyway
   */
  protected def escaped(target : Name) : String = escaped(target.ada)

  protected def packagePath = if (packagePrefix.length > 0) {
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
  else if (t.isInstanceOf[ContainerType]) "???"
  else escaped(t.getName.capital)
  /**
   * provides a default name for the argument field
   */
  protected def name(f : Field) : String = escaped(f.getName.capital)

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
