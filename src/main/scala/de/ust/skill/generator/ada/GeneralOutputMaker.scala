/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import de.ust.skill.ir._
import java.io.File
import java.io.PrintWriter
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import scala.collection.mutable.MutableList
import de.ust.skill.generator.common.Generator
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden, Dennis Przytarski
 */
trait GeneralOutputMaker extends Generator {

  override def getLanguageName = "ada";

  /**
   * configurable build mode; either "release" or "debug"
   */
  var buildMode = "release"
  var buildOS = System.getProperty("os.name").toLowerCase
  var buildARCH = "amd64"

  private[ada] def header : String

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
   * Assume the existence of a translation function for the types.
   */
  protected def mapTypeToId(t : Type, f : Field) : String
  protected def mapType(t : Type) : String

  /**
   * Assume the existence of inheritance information functions for the types.
   */
  protected def getSuperTypes(d : UserType) : MutableList[Type]
  protected def getSubTypes(d : UserType) : MutableList[Type]

  /**
   * Assume the existence of the get field parameters function.
   */
  protected def printParameters(d : UserType) : String

  /**
   * Assume a package prefix provider. Small p for files, large P for names.
   */
  protected def packagePrefix : String
  protected def PackagePrefix : String
  // the name of tha package that contains all pools
  protected def poolsPackage : String

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
  protected final def name(d : Type) = nameCache.get(d).getOrElse { val r = escapedLonely(d.getName.ada); nameCache(d) = r; r }
  protected final def name(f : Field) = escapedLonely(f.getName.ada)

  /**
   * creates references to generated skill names package
   */
  private final val skillNameCache = HashMap[String, String]()
  protected final def internalSkillName(f : Field) = skillNameCache.get(f.getSkillName).getOrElse {
    val r = s"$PackagePrefix.Internal_Skill_Names.${escaped(f.getSkillName).capitalize}_Skill_Name";
    skillNameCache(f.getSkillName) = r;
    r
  }
  protected final def internalSkillName(t : Type) = skillNameCache.get(t.getSkillName).getOrElse {
    val r = s"$PackagePrefix.Internal_Skill_Names.${escaped(t.getSkillName).capitalize}_Skill_Name";
    skillNameCache(t.getSkillName) = r;
    r
  }

  private lazy val packagePath = if (packagePrefix.length > 0) {
    packagePrefix.replace(".", "/")
  } else {
    ""
  }
}
