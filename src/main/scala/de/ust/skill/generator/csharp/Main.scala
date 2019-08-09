/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.HashMap

import de.ust.skill.generator.csharp.api.SkillFileMaker
import de.ust.skill.generator.csharp.api.VisitorsMaker
import de.ust.skill.generator.csharp.internal.AccessMaker
import de.ust.skill.generator.csharp.internal.FieldDeclarationMaker
import de.ust.skill.generator.csharp.internal.FileParserMaker
import de.ust.skill.generator.csharp.internal.StateMaker
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
import de.ust.skill.main.HeaderInfo

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * A generator turns a set of skill declarations into a Java interface providing means of manipulating skill files
 * containing instances of the respective UserTypes.
 *
 * @author Simon Glaub, Timm Felden
 */
class Main extends FakeMain
    with DependenciesMaker
    with InterfacesMaker
    with InternalMaker
    with SkillFileMaker
    with TypesMaker
    with VisitorsMaker {

  lineLength = 120
  override def comment(d : Declaration) : String = d.getComment.format("/// <summary>\n", "    /// ", lineLength, "    /// </summary>\n    ")
  override def comment(f : FieldLike) : String = f.getComment.format("/// <summary>\n", "        /// ", lineLength, "        /// </summary>\n        ")

  /**
   * Translates types into scala type names.
   */
  override def mapType(t : Type, boxed : Boolean) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ "de.ust.skill.common.csharp.@internal.SkillObject"

      case "bool"       ⇒ if (boxed) "System.Boolean" else "bool"

      case "i8"         ⇒ if (boxed) "System.SByte" else "sbyte"
      case "i16"        ⇒ if (boxed) "System.Int16" else "short"
      case "i32"        ⇒ if (boxed) "System.Int32" else "int"
      case "i64"        ⇒ if (boxed) "System.Int64" else "long"
      case "v64"        ⇒ if (boxed) "System.Int64" else "long"

      case "f32"        ⇒ if (boxed) "System.Single" else "float"
      case "f64"        ⇒ if (boxed) "System.Double" else "double"

      case "string"     ⇒ if (boxed) "System.String" else "string"
    }

    case t : ConstantLengthArrayType ⇒ s"$ArrayTypeName"
    case t : VariableLengthArrayType ⇒ s"$VarArrayTypeName"
    case t : ListType                ⇒ s"$ListTypeName<${mapType(t.getBaseType(), true)}>"
    case t : SetType                 ⇒ s"$SetTypeName<${mapType(t.getBaseType(), true)}>"
    case t : MapType                 ⇒ t.getBaseTypes().map(mapType(_, true)).reduceRight((k, v) ⇒ s"$MapTypeName<$k, $v>")

    case t : Declaration             ⇒ packagePrefix + name(t)

    case _                           ⇒ throw new IllegalStateException(s"Unknown type $t")
  }

  override protected def mapVariantType(t : Type) : String = t match {

    case t : ConstantLengthArrayType ⇒ s"$ArrayTypeName"
    case t : VariableLengthArrayType ⇒ s"$VarArrayTypeName"
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

  override def makeHeader(headerInfo : HeaderInfo) : String = headerInfo.format(this, "/*", "*\\", " *", "* ", "\\*", "*/")

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
  override def defaultCleanMode = "file";

  override def setOption(option : String, value : String) : Unit = option match {
    case "revealskillid"    ⇒ revealSkillID = ("true".equals(value));
    case "suppresswarnings" ⇒ suppressWarnings = if ("true".equals(value)) "@SuppressWarnings(\"all\")\n" else ""
    case unknown            ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def helpText : String = """
revealSkillID     true/false  if set to true, the generated binding will reveal SKilL IDs in the API
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
      case "abstract" | "continue" | "for" | "new" | "switch" | "default" | "if" | "Double" | "int" | "Int32" | "UInt32"
        | "Boolean" | "do" | "goto" | "private" | "this" | "break" | "double" | "protected" | "throw" | "Byte" | "void"
        | "byte" | "else" | "public" | "case" | "enum" | "Enum" | "return"  | "char" | "Char" | "short" | "Int16" | "in"
        | "UInt16" | "catch" | "extends" | "try" | "interface" | "static" | "class" | "long" | "Int64" | "UInt64" | "out"
        | "finally" | "volatile" | "const" | "float" | "Single" | "while" | "String" | "string" | "lock" | "namespace"
        | "ref" | "internal" | "bool" | "System" | "operator" | "sbyte" | "Sbyte" | "event" | "as" | "base" | "checked"
        | "decimal" | "delegate" | "explicit" | "extern" | "fixed" | "foreach" | "implicit" | "is" | "false" | "true"
        | "object" | "override" | "readonly" | "sealed" | "stackalloc" | "struct" | "uint" | "null" | "ushort" | "using"
        | "typeof" | "ulong" | "unchecked" | "unsafe" | "virtual" ⇒ "Z" + target

      case _ ⇒ target.map {
        case '_'                                  ⇒ "__"
        case ':'                                  ⇒ "_"
        case 'Z'                                  ⇒ "ZZ"
        case c if Character.isLetterOrDigit(c)    ⇒ c.toString
        case c                                    ⇒ "Z" + c.toHexString
      }.reduce(_ + _)
    }
    escapeCache(target) = result
    result
  })

}
