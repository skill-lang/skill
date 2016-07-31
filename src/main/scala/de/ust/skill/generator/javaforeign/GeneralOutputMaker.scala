/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.javaforeign

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
import java.nio.file.Paths
import de.ust.skill.ir.InterfaceType
import javassist.CtClass
import scala.collection.mutable.HashMap
import javassist.NotFoundException
import de.ust.skill.ir.GroundType

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden
 */
trait GeneralOutputMaker extends Generator {

  // remove special stuff
  final def setTC(tc : TypeContext) = {
    this.types = tc
    val flat = tc.removeTypedefs.removeEnums
    this.IR = flat.getUsertypes.to
    this.interfaces = flat.getInterfaces.to
    // set large specification mode; leave some spare parameters
    largeSpecificationMode = IR.size > 200
  }
  var types : TypeContext = _
  var IR : List[UserType] = _
  var interfaces : List[InterfaceType] = _
  var reflectionMap: HashMap[Type, CtClass] = _

  def setReflectionMap(rm: HashMap[Type, CtClass]): Unit = { this.reflectionMap = rm }

  /**
   * This flag is set iff the specification is too large to be passed as parameter list
   */
  var largeSpecificationMode = false

  override def getLanguageName = "javaforeign";

  // options
  /**
   * if set to true, the generated binding will reveal the values of skill IDs.
   */
  protected var revealSkillID = false;

  val ArrayTypeName = "java.util.ArrayList"
  val VarArrayTypeName = "java.util.ArrayList"
  val ListTypeName = "java.util.LinkedList"
  val SetTypeName = "java.util.HashSet"
  val MapTypeName = "java.util.HashMap"

  private[javaforeign] def header : String

  /**
   * Creates the correct PrintWriter for the argument file.
   *
   * @note the used path uses maven/sbt source placement convention
   */
  override protected def open(path : String) = {
    val f = simpleOpenDirtyPathString(s"$outPath/$sourcePath/$packagePath$path")

    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(f), "UTF-8")))
    rval.write(header)
    rval
  }

  /**
   * Assume the existence of a translation function for types.
   */
  protected def mapType(t : Type, boxed : Boolean = false) : String

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
  protected def userPackagePrefix(t: Type) : String = if (t.getName.getPackagePath.size > 0)
    t.getName.getPackagePath + "." else ""
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

  /**
   * Sourcepath can be configured to change output directory of source files.
   */
  protected var sourcePath = "src/main/java"

  /**
   * this string may contain a "@SuppressWarnings("all")\n", in order to suppress warnings in generated code;
   * the option can be enabled by "-O@java:SuppressWarnings=true"
   */
  protected var suppressWarnings = "";

  def getterOrFieldAccess(t: Type, f: Field): String = if (t.isInstanceOf[GroundType]) {
    s"get${escaped(f.getName.capital())}"
  } else {
    val javaType = reflectionMap(t);
    try {
      javaType.getDeclaredMethod(s"get${f.getName.capital}()")
      s"get${f.getName.capital()}"
    } catch {
      case e: NotFoundException ⇒ s"${f.getName}"
    }
  }

  def setterOrFieldAccess(t: Type, f: Field): String = if (t.isInstanceOf[GroundType]) {
    s"set${escaped(f.getName.capital())}"
  } else {
    val javaType = reflectionMap(t);
    try {
      javaType.getDeclaredMethod(s"set${f.getName.capital}") // TODO: fix this
      s"set${f.getName.capital()}"
    } catch {
      case e: NotFoundException ⇒ s"${f.getName} = "
    }
  }
}
