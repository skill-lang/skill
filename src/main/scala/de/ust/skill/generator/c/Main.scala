/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c

import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList

import de.ust.skill.generator.c.api.ApiHeaderMaker
import de.ust.skill.generator.c.api.ApiSourceMaker
import de.ust.skill.generator.c.io.BinaryReaderHeaderMaker
import de.ust.skill.generator.c.io.BinaryReaderSourceMaker
import de.ust.skill.generator.c.io.BinaryWriterHeaderMaker
import de.ust.skill.generator.c.io.BinaryWriterSourceMaker
import de.ust.skill.generator.c.io.ReaderHeaderMaker
import de.ust.skill.generator.c.io.ReaderSourceMaker
import de.ust.skill.generator.c.io.WriterHeaderMaker
import de.ust.skill.generator.c.io.WriterSourceMaker
import de.ust.skill.generator.c.model.FieldInformationHeaderMaker
import de.ust.skill.generator.c.model.FieldInformationSourceMaker
import de.ust.skill.generator.c.model.SkillStateHeaderMaker
import de.ust.skill.generator.c.model.SkillStateSourceMaker
import de.ust.skill.generator.c.model.StoragePoolHeaderMaker
import de.ust.skill.generator.c.model.StoragePoolSourceMaker
import de.ust.skill.generator.c.model.StringAccessHeaderMaker
import de.ust.skill.generator.c.model.StringAccessSourceMaker
import de.ust.skill.generator.c.model.TypeDeclarationHeaderMaker
import de.ust.skill.generator.c.model.TypeDeclarationSourceMaker
import de.ust.skill.generator.c.model.TypeEnumHeaderMaker
import de.ust.skill.generator.c.model.TypeEnumSourceMaker
import de.ust.skill.generator.c.model.TypeInformationHeaderMaker
import de.ust.skill.generator.c.model.TypeInformationSourceMaker
import de.ust.skill.generator.c.model.TypesHeaderMaker
import de.ust.skill.generator.c.model.TypesSourceMaker
import de.ust.skill.ir._
import de.ust.skill.main.HeaderInfo

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
    with BinaryReaderHeaderMaker
    with BinaryReaderSourceMaker
    with BinaryWriterHeaderMaker
    with BinaryWriterSourceMaker
    with FieldInformationHeaderMaker
    with FieldInformationSourceMaker
    with MakefileMaker
    with ReaderHeaderMaker
    with ReaderSourceMaker
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
    with TypesSourceMaker
    with WriterHeaderMaker
    with WriterSourceMaker {

  lineLength = 80
  override def comment(d : Declaration) : String = d.getComment.format("", "//! ", lineLength, "")
  override def comment(f : FieldLike) : String = f.getComment.format("", "//! ", lineLength, "")
  
  override def defaultCleanMode = "file";

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

  override def makeHeader(headerInfo : HeaderInfo) : String = headerInfo.format(this, "/*", "*\\", " *", "* ", "\\*", "*/")

  override def setOption(option : String, value : String) : Unit = option.toLowerCase match {
    case "unsafe" ⇒ unsafe = value == "true"
    case unknown  ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def helpText : String = """
unsafe                 remove all generated runtime type checks, if set to "true"
"""

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
