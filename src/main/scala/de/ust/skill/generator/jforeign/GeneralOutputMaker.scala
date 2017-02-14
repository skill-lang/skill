/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.jforeign

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ListBuffer

import de.ust.skill.generator.common.Generator
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.Type
import de.ust.skill.ir.TypeContext
import de.ust.skill.ir.UserType
import de.ust.skill.jforeign.ReflectionContext
import javassist.NotFoundException

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden
 */
trait GeneralOutputMaker extends Generator {

  // remove special stuff
  final def setForeignTC(tc : TypeContext) {
    this.types = tc
    val flat = tc.removeTypedefs.removeEnums
    this.IR = flat.getUsertypes.to
    this.interfaces = flat.getInterfaces.to
    // set large specification mode; leave some spare parameters
    largeSpecificationMode = IR.size > 200
  }
  final def setTC(tc : TypeContext) { types = tc }

  var types : TypeContext = _
  var IR : List[UserType] = _
  var interfaces : List[InterfaceType] = _
  var rc : ReflectionContext = _

  def setReflectionContext(rc : ReflectionContext) : Unit = { this.rc = rc }

  /**
   * This flag is set iff the specification is too large to be passed as parameter list
   */
  var largeSpecificationMode = false

  override def getLanguageName : String = "javaforeign";

  // options
  /**
   * if set to true, the generated binding will reveal the values of skill IDs.
   */
  protected var revealSkillID = false;

  val ArrayTypeName = "java.util.List"
  val VarArrayTypeName = "java.util.List"
  val ListTypeName = "java.util.List"
  val SetTypeName = "java.util.Set"
  val MapTypeName = "java.util.Map"

  /**
   * Assume the existence of a translation function for types.
   */
  protected def mapType(t : Type, boxed : Boolean = false) : String

  protected def mapType(f : Field, boxed : Boolean) : String

  /**
   * creates argument list of a constructor call, not including potential skillID or braces
   */
  protected def makeConstructorArguments(t : UserType) : String
  /**
   * creates argument list of a constructor call, including a trailing comma for insertion into an argument list
   */
  protected def appendConstructorArguments(t : UserType, prependTypes : Boolean = true) : String

  /**
   * Translation of a type to its representation in the source code
   */
  protected def name(t : Type) : String = escaped(t.getName.capital)
  /**
   * Translation of a field to its representation in the source code
   */
  protected def name(f : Field) : String = escaped(f.getName.camel)
  /**
   * Translates a possible non-empty package name into a package prefix.
   */
  protected def userPackagePrefix(t : Type) : String = if (t.getName.getPackagePath.size > 0)
    t.getName.getPackagePath + "." else ""
  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix() : String
  protected def packageName = packagePrefix.substring(0, packagePrefix.length - 1)

  /**
   * this string may contain a "@SuppressWarnings("all")\n", in order to suppress warnings in generated code;
   * the option can be enabled by "-O@java:SuppressWarnings=true"
   */
  protected var suppressWarnings = "";

  protected var mappingFile : String = null;

  protected val foreignSources : ListBuffer[String] = ListBuffer()

  protected var genSpecPath : Option[String] = None

  def getMappingFile() : String = mappingFile

  def getForeignSources() : List[String] = foreignSources.toList

  def getGenSpecPath : Option[String] = genSpecPath

  def getterOrFieldAccess(t : Type, f : Field) : String = if (t.isInstanceOf[GroundType]) {
    s"get${escaped(f.getName.capital())}"
  } else {
    val javaType = rc.map(t);
    try {
      javaType.getDeclaredMethod(s"get${f.getName.capital}")
      s"get${f.getName.capital()}()"
    } catch {
      case e : NotFoundException ⇒ s"${f.getName}"
    }
  }

  def setterOrFieldAccess(t : Type, f : Field) : String = if (t.isInstanceOf[GroundType]) {
    s"set${escaped(f.getName.capital())}"
  } else {
    val javaType = rc.map(t);
    try {
      javaType.getDeclaredMethod(s"set${f.getName.capital}") // TODO: fix this
      s"set${f.getName.capital()}"
    } catch {
      case e : NotFoundException ⇒ s"${f.getName} = "
    }
  }
}
