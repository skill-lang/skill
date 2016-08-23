/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.haskell

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.util.Date

import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList

import de.ust.skill.generator.common.Generator
import de.ust.skill.ir._

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * ...
 *
 * @author ...
 */
final class Main extends FakeMain {

  override def make {
    (new CodeGenerator(IR.to, this)).make()
  }

  lineLength = 80
  override def comment(d : Declaration) : String = d.getComment.format("", "-- ", lineLength, "")
  override def comment(f : FieldLike) : String = f.getComment.format("", "-- ", lineLength, "")

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
    if (!names.isEmpty)
      _packagePrefix = names.map(_.toLowerCase).mkString("_");
  }

  override private[haskell] def header : String = _header
  private lazy val _header = {
    // create header from options
    val headerLineLength = 51
    val headerLine1 = Some((headerInfo.line1 match {
      case Some(s) ⇒ s
      case None    ⇒ headerInfo.license.map("LICENSE: " + _).getOrElse("Your SKilL Haskell Binding")
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    val headerLine2 = Some((headerInfo.line2 match {
      case Some(s) ⇒ s
      case None ⇒ "generated: " + (headerInfo.date match {
        case Some(s) ⇒ s
        case None    ⇒ (new java.text.SimpleDateFormat("dd.MM.yyyy")).format(new Date)
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    val headerLine3 = Some((headerInfo.line3 match {
      case Some(s) ⇒ s
      case None ⇒ "by: " + (headerInfo.userName match {
        case Some(s) ⇒ s
        case None    ⇒ System.getProperty("user.name")
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))

    s"""--  ___ _  ___ _ _                                                            
-- / __| |/ (_) | |       ${headerLine1.get}
-- \\__ \\ ' <| | | |__     ${headerLine2.get}
-- |___/_|\\_\\_|_|____|    ${headerLine3.get}
--                                                                            
"""
  }

  var outPostfix = s"/genereted/${
    if (packagePrefix.isEmpty()) ""
    else packagePrefix.replace('.', '/') + "/"
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

  override def setOption(option : String, value : String) : Unit = option.toLowerCase match {
    case "gendir" ⇒
      outPostfix = value
      if (!outPostfix.startsWith("/"))
        outPostfix = "/" + outPostfix
      if (!outPostfix.endsWith("/"))
        outPostfix = outPostfix + "/"
    case "unsafe" ⇒ unsafe = value == "true"
    case unknown  ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def printHelp : Unit = println("""
Opitions (haskell):
""")

  override def customFieldManual : String = "not supported"

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  final def escaped(target : String) : String = target match {
    case "auto" | "_Bool" | "break" | "case" | "char" | "_Complex" | "const" | "continue" | "default" | "do" | "double"
      | "else" | "enum" | "extern" | "float" | "for" | "goto" | "if" | "_Imaginary" | "inline" | "int" | "long" |
      "register" | "restrict" | "return" | "short" | "signed" | "sizeof" | "static" | "struct" | "switch" | "typedef"
      | "union" | "unsigned" | "void" | "volatile" | "while" ⇒ "ZZ_" + target

    // the string is fine anyway
    case _ ⇒ target
  }

  override protected def makeConstructorArguments(t : UserType) : String = (for (f ← t.getAllFields; if !f.isConstant())
    yield s""", ${mapType(f.getType)} _${name(f)}""").mkString

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
