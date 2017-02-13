/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java

import java.util.Date

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.HashMap

import de.ust.skill.generator.java.api.SkillFileMaker
import de.ust.skill.generator.java.api.VisitorsMaker
import de.ust.skill.generator.java.internal.AccessMaker
import de.ust.skill.generator.java.internal.FieldDeclarationMaker
import de.ust.skill.generator.java.internal.FileParserMaker
import de.ust.skill.generator.java.internal.StateMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.FieldLike
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.generator.common.HeaderInfo

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * A generator turns a set of skill declarations into a Java interface providing means of manipulating skill files
 * containing instances of the respective UserTypes.
 *
 * @author Timm Felden
 */
class Main extends FakeMain
    with AccessMaker
    with DependenciesMaker
    with InterfacesMaker
    with FieldDeclarationMaker
    with FileParserMaker
    with StateMaker
    with SkillFileMaker
    with TypesMaker
    with VisitorsMaker {

  lineLength = 120
  override def comment(d : Declaration) : String = d.getComment.format("/**\n", " * ", lineLength, " */\n")
  override def comment(f : FieldLike) : String = f.getComment.format("/**\n", "     * ", lineLength, "     */\n    ")

  /**
   * Translates types into scala type names.
   */
  override protected def mapType(t : Type, boxed : Boolean) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ "de.ust.skill.common.java.internal.SkillObject"

      case "bool"       ⇒ if (boxed) "java.lang.Boolean" else "boolean"

      case "i8"         ⇒ if (boxed) "java.lang.Byte" else "byte"
      case "i16"        ⇒ if (boxed) "java.lang.Short" else "short"
      case "i32"        ⇒ if (boxed) "java.lang.Integer" else "int"
      case "i64"        ⇒ if (boxed) "java.lang.Long" else "long"
      case "v64"        ⇒ if (boxed) "java.lang.Long" else "long"

      case "f32"        ⇒ if (boxed) "java.lang.Float" else "float"
      case "f64"        ⇒ if (boxed) "java.lang.Double" else "double"

      case "string"     ⇒ "java.lang.String"
    }

    case t : ConstantLengthArrayType ⇒ s"$ArrayTypeName<${mapType(t.getBaseType(), true)}>"
    case t : VariableLengthArrayType ⇒ s"$VarArrayTypeName<${mapType(t.getBaseType(), true)}>"
    case t : ListType                ⇒ s"$ListTypeName<${mapType(t.getBaseType(), true)}>"
    case t : SetType                 ⇒ s"$SetTypeName<${mapType(t.getBaseType(), true)}>"
    case t : MapType                 ⇒ t.getBaseTypes().map(mapType(_, true)).reduceRight((k, v) ⇒ s"$MapTypeName<$k, $v>")

    case t : Declaration             ⇒ packagePrefix + name(t)

    case _                           ⇒ throw new IllegalStateException(s"Unknown type $t")
  }

  override protected def mapVariantType(t : Type) : String = t match {

    case t : ConstantLengthArrayType ⇒ s"$ArrayTypeName<?>"
    case t : VariableLengthArrayType ⇒ s"$VarArrayTypeName<?>"
    case t : ListType                ⇒ s"$ListTypeName<?>"
    case t : SetType                 ⇒ s"$SetTypeName<?>"
    case t : MapType                 ⇒ t.getBaseTypes().map(_ ⇒ "?").reduceRight((k, v) ⇒ s"$MapTypeName<$k, $v>")

    case _                           ⇒ mapType(t, true)
  }

  /**
   * creates argument list of a constructor call, not including potential skillID or braces
   */
  override protected def makeConstructorArguments(t : UserType) = (
    for (f ← t.getAllFields if !(f.isConstant || f.isIgnored))
      yield s"${mapType(f.getType())} ${name(f)}").mkString(", ")
  override protected def appendConstructorArguments(t : UserType, prependTypes : Boolean) = {
    val r = t.getAllFields.filterNot { f ⇒ f.isConstant || f.isIgnored }
    if (r.isEmpty) ""
    else if (prependTypes) r.map({ f ⇒ s", ${mapType(f.getType())} ${name(f)}" }).mkString("")
    else r.map({ f ⇒ s", ${name(f)}" }).mkString("")
  }

  override def makeHeader(headerInfo : HeaderInfo) : String = {
    // create header from options
    val headerLineLength = 51
    val headerLine1 = Some((headerInfo.line1 match {
      case Some(s) ⇒ s
      case None    ⇒ headerInfo.license.map("LICENSE: " + _).getOrElse("Your SKilL Java 8 Binding")
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

    s"""/*  ___ _  ___ _ _                                                            *\\
 * / __| |/ (_) | |       ${headerLine1.get} *
 * \\__ \\ ' <| | | |__     ${headerLine2.get} *
 * |___/_|\\_\\_|_|____|    ${headerLine3.get} *
\\*                                                                            */
"""
  }

  /**
   * provides the package prefix
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    _packagePrefix = names.foldRight("")(_ + "." + _)
  }

  override def packageDependentPathPostfix = if (packagePrefix.length > 0) {
    packagePrefix.replace(".", "/")
  } else {
    ""
  }
  
  override def setOption(option : String, value : String) : Unit = option match {
    case "revealskillid"    ⇒ revealSkillID = ("true".equals(value));
    case "visitors"         ⇒ createVisitors = ("true".equals(value));
    case "suppresswarnings" ⇒ suppressWarnings = if ("true".equals(value)) "@SuppressWarnings(\"all\")\n" else ""
    case unknown            ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def helpText : String = """
revealSkillID     true/false  if set to true, the generated binding will reveal SKilL IDs in the API
visitors          true/false  if set to true, the a visitor for each base type will be generated
suppressWarnings  true/false  add a @SuppressWarnings("all") annotation to generated classes
"""

  override def customFieldManual : String = """
!import string+    A list of imports that will be added where required.
!modifier string   A modifier, that will be put in front of the variable declaration."""

  override protected def defaultValue(f : Field) = f.getType match {
    case t : GroundType ⇒ t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0f"
      case "bool"                               ⇒ "false"
      case _                                    ⇒ "null"
    }

    // TODO compound types would behave more nicely if they would be initialized with empty collections instead of null

    case _ ⇒ "null"
  }

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   * Will add Z's if escaping is required.
   */
  private val escapeCache = new HashMap[String, String]();
  final def escaped(target : String) : String = escapeCache.getOrElse(target, {
    val result = target match {
      case "abstract" | "continue" | "for" | "new" | "switch" | "assert" | "default" | "if" | "package" | "synchronized"
        | "boolean" | "do" | "goto" | "private" | "this" | "break" | "double" | "implements" | "protected" | "throw"
        | "byte" | "else" | "import" | "public" | "throws" | "case" | "enum" | "instanceof" | "return" | "transient"
        | "catch" | "extends" | "int" | "short" | "try" | "char" | "final" | "interface" | "static" | "void" | "class"
        | "finally" | "long" | "strictfp" | "volatile" | "const" | "float" | "native" | "super" | "while" ⇒ "Z" + target

      case _ ⇒ target.map {
        case ':'                                    ⇒ "$"
        case 'Z'                                    ⇒ "ZZ"
        case c if Character.isJavaIdentifierPart(c) ⇒ c.toString
        case c                                      ⇒ "Z" + c.toHexString
      }.reduce(_ + _)
    }
    escapeCache(target) = result
    result
  })

}
