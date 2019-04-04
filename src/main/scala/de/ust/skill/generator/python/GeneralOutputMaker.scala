/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.common.Generator
import de.ust.skill.ir.FieldLike
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.Type
import de.ust.skill.ir.TypeContext
import de.ust.skill.ir.UserType

import scala.collection.mutable

/**
 * The parent class for all output makers.
 *
 * @author Alexander Maisch
 */
trait GeneralOutputMaker extends Generator {

  // remove special stuff
  final def setTC(tc : TypeContext) {
    this.types = tc
    val flat = tc.removeTypedefs().removeEnums()
    this.IR = flat.getUsertypes.to
    this.interfaces = flat.getInterfaces.to
    // set large specification mode; leave some spare parameters
    largeSpecificationMode = IR.size > 200
  }
  var types : TypeContext = _
  var IR : List[UserType] = _
  var interfaces : List[InterfaceType] = _

  /**
   * This flag is set iff the specification is too large to be passed as parameter list
   */
  var largeSpecificationMode = false

  override def getLanguageName : String = "python"

  // options
  /**
   * if set to true, the generated binding will reveal the values of skill IDs.
   */
  protected var revealSkillID = false

  val ArrayTypeName = "list"
  val VarArrayTypeName = "list"
  val ListTypeName = "list"
  val SetTypeName = "set"
  val MapTypeName = "dict"

  /**
   * Assume the existence of a translation function for types.
   */
  protected def mapType(t : Type) : String

  /**
   * creates argument list of a constructor call, not including potential skillID or braces
   */
  protected def makeConstructorArguments(t : UserType) : String
  /**
   * creates argument list of the constructor, including a trailing comma for insertion into an argument list
   */
  protected def appendConstructorArguments(t : UserType, prependTypes : Boolean = true) : String

  /**
    * creates argument list of a constructor call, including a trailing comma for insertion into an argument list
    */
  protected def appendInitializationArguments(t : UserType, prependTypes : Boolean = true) : String

  /**
   * Translation of a type to its representation in the source code
   */
  protected def name(t : Type) : String = escaped(t.getName.capital)
  /**
   * Translation of a field to its representation in the source code
   */
  protected def name(f : FieldLike) : String = escaped(f.getName.camel)

  /**
   * id's given to fields
   */
  protected val poolNameStore : mutable.HashMap[String, Int] = new mutable.HashMap()
  /**
   * The name of T's storage pool
   */
  protected def access(t : Type) : String = this.synchronized {
    "P" + poolNameStore.getOrElseUpdate(t.getSkillName, poolNameStore.size).toString
  }

  /**
   * id's given to fields
   */
  protected val fieldNameStore : mutable.HashMap[(String, String), Int] = new mutable.HashMap()
  /**
   * Class name of the representation of a known field
   */
  protected def knownField(f : FieldLike) : String = this.synchronized {
    "F" + fieldNameStore.getOrElseUpdate((f.getDeclaredIn.getName.getSkillName, f.getSkillName), fieldNameStore.size).toString
  }

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix() : String
  protected def packageName: String = packagePrefix().substring(0, packagePrefix().length - 1)

  /**
   * this string may contain a "@SuppressWarnings("all")\n", in order to suppress warnings in generated code;
   * the option can be enabled by "-O@java:SuppressWarnings=true"
   */
  protected var suppressWarnings = ""
}
