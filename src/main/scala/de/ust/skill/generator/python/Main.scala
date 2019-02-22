/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.HashMap

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
abstract class FakeMain extends de.ust.skill.generator.python.GeneralOutputMaker { def make {} }

/**
 * A generator turns a set of skill declarations into a python interface providing means of manipulating skill files
 * containing instances of the respective UserTypes.
 *
 * @author Alexander Maisch
 */
class Main extends FakeMain
    with InternalMaker
    with TypesMaker {

  lineLength = 120
  override def comment(d : Declaration) : String = d.getComment.format("    \"\"\"\n", "    ", lineLength, "    \"\"\"\n")
  override def comment(f : FieldLike) : String = f.getComment.format("# ", "    # ", lineLength, "\n")

  /**
   * Translates types into python type names.
   */
  override def mapType(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation"                         ⇒ "src.internal.SkillObject.SkillObject"

      case "bool"                               ⇒ "bool"

      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "int"

      case "f32" | "f64"                        ⇒ "float"

      case "string"                             ⇒ "str"
    }

    case t : ConstantLengthArrayType ⇒ s"$ArrayTypeName"
    case t : VariableLengthArrayType ⇒ s"$VarArrayTypeName"
    case t : ListType                ⇒ s"$ListTypeName"
    case t : SetType                 ⇒ s"$SetTypeName"
    case t : MapType                 ⇒ s"$MapTypeName"

    case t : Declaration             ⇒ packagePrefix + name(t)

    case _                           ⇒ throw new IllegalStateException(s"Unknown type $t")
  }

  /**
   * creates argument list of a constructor call, not including potential skillID or braces
   */
  override protected def makeConstructorArguments(t : UserType): String = (
    for (f ← t.getAllFields if !(f.isConstant || f.isIgnored))
      yield s"${name(f)}: ${mapType(f.getType)}=None").mkString(", ")
  override protected def appendConstructorArguments(t : UserType, prependTypes : Boolean): String = {
    val r = t.getAllFields.filterNot { f ⇒ f.isConstant || f.isIgnored }
    if (r.isEmpty) ""
    else if (prependTypes) r.map({ f ⇒ s", ${name(f)}: ${mapType(f.getType)}" }).mkString("")
    else r.map({ f ⇒ s", ${name(f)}" }).mkString("")
  }

  override def makeHeader(headerInfo : HeaderInfo) : String = headerInfo.format(this, "#", "#", "#", "#", "#", "#")

  /**
   * provides the package prefix
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    _packagePrefix = names.foldRight("")(_ + "." + _)
  }

  override def packageDependentPathPostfix : String = if (packagePrefix().length > 0) {
    packagePrefix().replace(".", "/")
  } else {
    ""
  }
  override def defaultCleanMode = "file"

  override def setOption(option : String, value : String) : Unit = option match {
    case "revealskillid"    ⇒ revealSkillID = ("true".equals(value));
    case "suppresswarnings" ⇒ suppressWarnings = if ("true".equals(value)) "@SuppressWarnings(\"all\")\n" else ""
    case unknown            ⇒ sys.error(s"unknown Argument: $unknown")
  }

  override def helpText : String = """
revealSkillID     true/false  if set to true, the generated binding will reveal SKilL IDs in the API
suppressWarnings  true/false  add a @SuppressWarnings("all") annotation to generated classes
"""

  override def customFieldManual : String = """
!import string+    A list of imports that will be added where required.
!modifier string   A modifier, that will be put in front of the variable declaration."""

  override protected def defaultValue(f : Field) = f.getType match {
    case t : GroundType ⇒ t.getSkillName match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0f"
      case "bool"                               ⇒ "false"
      case _                                    ⇒ "None"
    }

    // TODO compound types would behave more nicely if they would be initialized with empty collections instead of null

    case _ ⇒ "None"
  }

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   * Will add Z's if escaping is required.
   */
  private val escapeCache = new HashMap[String, String]()
  final def escaped(target : String) : String = escapeCache.getOrElse(target, {
    val result = target match {
      case "continue" | "for" | "assert" | "if" | "bool" | "self" | "break" | "raise" | "elif" | "else"
        | "from" | "import" | "raise" | "isinstance" | "return" | "except" | "int" | "try" | "class"
        | "finally" | "float" | "while" ⇒ "Z" + target

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
