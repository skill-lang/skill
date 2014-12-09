/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c

import de.ust.skill.ir._
import de.ust.skill.parser.Parser
import java.io.File
import java.util.Date
import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList
import de.ust.skill.generator.common.Generator
import java.io.PrintWriter
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import java.io.FileOutputStream
import de.ust.skill.generator.c.api.ApiHeaderMaker
import de.ust.skill.generator.c.api.ApiSourceMaker
import de.ust.skill.generator.c.model.FieldInformationHeaderMaker
import de.ust.skill.generator.c.model.FieldInformationSourceMaker
import de.ust.skill.generator.c.model.SkillStateSourceMaker
import de.ust.skill.generator.c.model.SkillStateHeaderMaker
import de.ust.skill.generator.c.model.StringAccessHeaderMaker
import de.ust.skill.generator.c.model.StringAccessSourceMaker
import de.ust.skill.generator.c.model.TypeDeclarationHeaderMaker
import de.ust.skill.generator.c.model.TypeDeclarationSourceMaker
import de.ust.skill.generator.c.model.TypeEnumHeaderMaker
import de.ust.skill.generator.c.model.TypeEnumSourceMaker
import de.ust.skill.generator.c.model.StoragePoolHeaderMaker
import de.ust.skill.generator.c.model.StoragePoolSourceMaker
import de.ust.skill.generator.c.model.TypeInformationHeaderMaker
import de.ust.skill.generator.c.model.TypeInformationSourceMaker
import de.ust.skill.generator.c.model.TypesHeaderMaker
import de.ust.skill.generator.c.model.TypesSourceMaker

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * A generator turns a set of skill declarations into an Ada interface providing means of manipulating skill files
 * containing instances of the respective UserTypes.
 *
 * @author Timm Felden, Fabian Harth
 * @note the style guide for emitted C-code is K&R with 4 spaces
 */
final class Main extends FakeMain
    with ApiHeaderMaker
    with ApiSourceMaker
    with FieldInformationHeaderMaker
    with FieldInformationSourceMaker
    with MakefileMaker
    with SkillStateHeaderMaker
    with SkillStateSourceMaker
    with StoragePoolHeaderMaker
    with StoragePoolSourceMaker
    with StringAccessHeaderMaker
    with StringAccessSourceMaker
    with TypeDeclarationHeaderMaker
    with TypeDeclarationSourceMaker
    with TypeEnumHeaderMaker
    with TypeEnumSourceMaker
    with TypeInformationHeaderMaker
    with TypeInformationSourceMaker
    with TypesHeaderMaker
    with TypesSourceMaker {

  override def comment(d : Declaration) = d.getComment.format("", "//! ", 80, "")
  override def comment(f : Field) = f.getComment.format("", "//! ", 80, "")

  /**
   * Translates the types into C99 types.
   */
  override protected def mapType(t : Type) : String = t match {
    case t : GroundType ⇒ t.getName.lower match {
      case "annotation" ⇒ "skill_type"

      case "bool"       ⇒ "bool"

      case "i8"         ⇒ "int8_t"
      case "i16"        ⇒ "int16_t"
      case "i32"        ⇒ "int32_t"
      case "i64"        ⇒ "int64_t"
      case "v64"        ⇒ "int64_t"

      case "f32"        ⇒ "float"
      case "f64"        ⇒ "double"

      case "string"     ⇒ "char*"
    }

    case t : ConstantLengthArrayType ⇒ "GArray*"
    case t : VariableLengthArrayType ⇒ "GArray*"
    case t : ListType                ⇒ "GList*"
    case t : SetType                 ⇒ "GHashTable*"
    case t : MapType                 ⇒ "GHashTable*"

    case t : Declaration             ⇒ s"${prefix}${t.getName.cStyle}"
  }

  /**
   * Gets all super types of a given type.
   */
  protected def getSuperTypes(d : UserType) : MutableList[Type] = {
    if (null == d.getSuperType) MutableList[Type]()
    else getSuperTypes(d.getSuperType) += d.getSuperType
  }

  /**
   * Gets all sub types of a given type.
   */
  protected def getSubTypes(d : UserType) : MutableList[Type] = {
    var rval = MutableList[Type]()

    IR.reverse.foreach { _d ⇒
      if (d == _d.getSuperType && -1 == rval.indexOf(_d)) {
        rval += _d
        rval ++= getSubTypes(_d)
      }
    }

    rval.distinct
  }

  /**
   * Gets the fields as parameters of a given type.
   */
  def printParameters(d : UserType) : String = {
    var output = "";
    var hasFields = false;
    output += d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map({ f ⇒
      hasFields = true;
      s"${f.getSkillName()} : ${mapType(f.getType)}"
    }).mkString("; ", "; ", "")
    if (hasFields) output else ""
  }

  /**
   * Provides the package prefix.
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    if (names.isEmpty)
      return ;

    if (names.size > 1)
      System.err.println("The Ada package system does not support nested packages with the expected meaning, dropping prefixes...");

    _packagePrefix = names.last
  }

  override private[c] def header : String = _header
  private lazy val _header = {
    // create header from options
    val headerLineLength = 51
    val headerLine1 = Some((headerInfo.line1 match {
      case Some(s) ⇒ s
      case None    ⇒ headerInfo.license.map("LICENSE: "+_).getOrElse("Your SKilL C Binding")
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    val headerLine2 = Some((headerInfo.line2 match {
      case Some(s) ⇒ s
      case None ⇒ "generated: "+(headerInfo.date match {
        case Some(s) ⇒ s
        case None    ⇒ (new java.text.SimpleDateFormat("dd.MM.yyyy")).format(new Date)
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    val headerLine3 = Some((headerInfo.line3 match {
      case Some(s) ⇒ s
      case None ⇒ "by: "+(headerInfo.userName match {
        case Some(s) ⇒ s
        case None    ⇒ System.getProperty("user.name")
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))

    s"""/*  ___ _  ___ _ _                                                            *\\
 * / __| |/ (_) | |       ${headerLine1.get} *
 * \\__ \\ ' <| | | |__     ${headerLine2.get} *
 * |___/_|\\_\\_|_|____|    ${headerLine3.get} *
\\*                                                                            */
"""
  }

  var outPostfix = s"/genereted/${
    if (packagePrefix.isEmpty()) ""
    else packagePrefix.replace('.', '/')+"/"
  }"

  /**
   * Creates the correct PrintWriter for the argument file.
   */
  override protected def open(path : String) = {
    val f = new File(outPath + outPostfix + path)
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(f), "UTF-8")))
    rval.write(header)
    rval
  }

  /**
   * Open file, but do not write a header (required by make files)
   */
  override protected def openRaw(path : String) = {
    val f = new File(outPath + outPostfix + path)
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(f), "UTF-8")))
    rval
  }

  override def setOption(option : String, value : String) = option.toLowerCase match {
    case "gendir" ⇒
      outPostfix = value
      if (!outPostfix.startsWith("/"))
        outPostfix = "/"+outPostfix
      if (!outPostfix.endsWith("/"))
        outPostfix = outPostfix+"/"
    case "unsafe" ⇒ unsafe = value == "true"
    case unknown  ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def printHelp : Unit = println("""
Opitions (C):
  genDir                 replace default sub-directory for generated sources
  unsafe                 remove all generated runtime type checks, if set to "true"
""")

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  protected def escaped(target : String) : String = target match {
    case "auto" | "_Bool" | "break" | "case" | "char" | "_Complex" | "const" | "continue" | "default" | "do" | "double"
      | "else" | "enum" | "extern" | "float" | "for" | "goto" | "if" | "_Imaginary" | "inline" | "int" | "long" |
      "register" | "restrict" | "return" | "short" | "signed" | "sizeof" | "static" | "struct" | "switch" | "typedef"
      | "union" | "unsigned" | "void" | "volatile" | "while" ⇒ return target.toUpperCase

    // the string is fine anyway
    case _ ⇒ return target
  }

  override protected def makeConstructorArguments(t : UserType) : String = (for (f ← t.getAllFields)
    yield s""", ${mapType(f.getType)} ${f.getName.cStyle}""").mkString

  override protected def defaultValue(f : Field) = f.getType match {
    case t : GroundType ⇒ t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0f"
      case "bool"                               ⇒ "false"
      case _                                    ⇒ "null"
    }

    // TODO compound types would behave more nicely if they would be initialized with empty collections instead of null
    // @note some collections use 0 as empty (e.g. list)

    case _ ⇒ "0"
  }
}
