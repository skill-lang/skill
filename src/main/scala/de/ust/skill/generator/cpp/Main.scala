/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import java.util.Date

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.HashMap

import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.View

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * A generator turns a set of skill declarations into a scala interface providing means of manipulating skill files
 * containing instances of the respective UserTypes.
 *
 * @author Timm Felden
 */
class Main extends FakeMain
    with FieldDeclarationsMaker
    //    with SerializationFunctionsMaker
    with SkillFileMaker
    with StringKeeperMaker
    //    with StateAppenderMaker
    //    with StateWriterMaker
    //    with DependenciesMaker
    with PoolsMaker
    with TypesMaker {

  lineLength = 120
  override def comment(d : Declaration) : String = d.getComment.format("/**\n", " * ", lineLength, " */\n")
  override def comment(f : Field) : String = f.getComment.format("/**\n", "   * ", lineLength, "   */\n  ")

  /**
   * Translates types into scala type names.
   */
  override protected def mapType(t : Type) : String = t match {
    case t : GroundType ⇒ t.getName.lower match {
      case "annotation" ⇒ "::skill::api::Object*"

      case "bool"       ⇒ "bool"

      case "i8"         ⇒ "int8_t"
      case "i16"        ⇒ "int16_t"
      case "i32"        ⇒ "int32_t"
      case "i64"        ⇒ "int64_t"
      case "v64"        ⇒ "int64_t"

      case "f32"        ⇒ "float"
      case "f64"        ⇒ "double"

      case "string"     ⇒ "::skill::api::String"
    }

    case t : ConstantLengthArrayType ⇒ s"${mapType(t.getBaseType())}*"
    case t : VariableLengthArrayType ⇒ s"::std::vector<${mapType(t.getBaseType())}>*"
    case t : ListType                ⇒ s"::std::vector<${mapType(t.getBaseType())}>*"
    case t : SetType                 ⇒ s"::std::set<${mapType(t.getBaseType())}>*"
    case t : MapType                 ⇒ t.getBaseTypes().map(mapType).reduceRight((k, v) ⇒ s"::std::map<$k, $v>*")

    case t : Declaration             ⇒ s"$packageName::${name(t)}*"

    case _                           ⇒ throw new IllegalStateException(s"Unknown type $t")
  }

  override protected def unbox(t : Type) : String = t match {

    case t : GroundType ⇒ t.getName.lower match {
      case "bool" ⇒ "boolean"
      case "v64"  ⇒ "i64"
      case t      ⇒ t;
    }

    case t : ConstantLengthArrayType ⇒ "array"
    case t : VariableLengthArrayType ⇒ "list"
    case t : ListType                ⇒ "list"
    case t : SetType                 ⇒ "set"
    case t : MapType                 ⇒ "map"

    case t : Declaration             ⇒ "annotation"

    case _                           ⇒ throw new IllegalStateException(s"Unknown type $t")
  }

  /**
   * creates argument list of a constructor call, not including potential skillID or braces
   */
  override protected def makeConstructorArguments(t : UserType) = (
    for (f ← t.getAllFields if !(f.isConstant || f.isIgnored))
      yield s"${escaped(f.getName.camel)} : ${mapType(f.getType())}"
  ).mkString(", ")
  override protected def appendConstructorArguments(t : UserType) = {
    val r = t.getAllFields.filterNot { f ⇒ f.isConstant || f.isIgnored }
    if (r.isEmpty) ""
    else r.map({ f ⇒ s"${escaped(f.getName.camel)} : ${mapType(f.getType())}" }).mkString(", ", ", ", "")
  }

  /**
   * Provide a nice file header:)
   */
  override private[cpp] def header : String = _header
  private lazy val _header = {
    // create header from options
    val headerLineLength = 51
    val headerLine1 = Some((headerInfo.line1 match {
      case Some(s) ⇒ s
      case None    ⇒ headerInfo.license.map("LICENSE: "+_).getOrElse("Your SKilL C++ Binding")
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

  /**
   * provides the package prefix
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    _packagePrefix = names.foldRight("")(_+"."+_)
  }

  override def setOption(option : String, value : String) = option match {
    case "revealskillid" ⇒ revealSkillID = ("true" == value)
    case unknown         ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def printHelp : Unit = println("""
Opitions (cpp):
  revealSkillID: true/false  if set to true, the generated binding will reveal SKilL IDs in the API
""")

  override protected def defaultValue(f : Field) = f.getType match {
    case t : GroundType ⇒ t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0f"
      case "bool"                               ⇒ "false"
      case _                                    ⇒ "null"
    }

    case t : UserType ⇒ "null"

    case _            ⇒ mapType(f.getType)+"()"
  }

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  private val escapeCache = new HashMap[String, String]();
  final def escaped(target : String) : String = escapeCache.getOrElse(target, {
    val result = EscapeFunction(target)
    escapeCache(target) = result
    result
  })

  protected def writeField(d : UserType, f : Field) : String = {
    val fName = escaped(f.getName.camel)
    if (f.isConstant())
      return "// constants do not write individual field data"

    f.getType match {
      case t : GroundType ⇒ t.getSkillName match {
        case "annotation" | "string" ⇒ s"for(i ← outData) ${f.getType.getSkillName}(i.$fName, dataChunk)"
        case _                       ⇒ s"for(i ← outData) dataChunk.${f.getType.getSkillName}(i.$fName)"

      }

      case t : Declaration ⇒ s"""for(i ← outData) userRef(i.$fName, dataChunk)"""

      case t : ConstantLengthArrayType ⇒ s"for(i ← outData) writeConstArray(${
        t.getBaseType() match {
          case t : Declaration ⇒ s"userRef[${mapType(t)}]"
          case b               ⇒ b.getSkillName()
        }
      })(i.$fName, dataChunk)"
      case t : VariableLengthArrayType ⇒ s"for(i ← outData) writeVarArray(${
        t.getBaseType() match {
          case t : Declaration ⇒ s"userRef[${mapType(t)}]"
          case b               ⇒ b.getSkillName()
        }
      })(i.$fName, dataChunk)"
      case t : SetType ⇒ s"for(i ← outData) writeSet(${
        t.getBaseType() match {
          case t : Declaration ⇒ s"userRef[${mapType(t)}]"
          case b               ⇒ b.getSkillName()
        }
      })(i.$fName, dataChunk)"
      case t : ListType ⇒ s"for(i ← outData) writeList(${
        t.getBaseType() match {
          case t : Declaration ⇒ s"userRef[${mapType(t)}]"
          case b               ⇒ b.getSkillName()
        }
      })(i.$fName, dataChunk)"

      case t : MapType ⇒ locally {
        s"for(i ← outData) ${
          t.getBaseTypes().map {
            case t : Declaration ⇒ s"userRef[${mapType(t)}]"
            case b               ⇒ b.getSkillName()
          }.reduceRight { (t, v) ⇒
            s"writeMap($t, $v)"
          }
        }(i.$fName, dataChunk)"
      }
    }
  }
}

object EscapeFunction {
  def apply(target : String) : String = target match {
    //keywords get a suffix "_", because that way at least auto-completion will work as expected
    case "auto" | "const" | "double" | "float" | "int" | "short" | "struct" | "unsigned" | "break" | "continue"
      | "else" | "for" | "long" | "signed" | "switch" | "void" | "case" | "default" | "enum" | "goto" | "register"
      | "sizeof" | "typedef" | "volatile" | "char" | "do" | "extern" | "if" | "return" | "static" | "union" | "while"
      | "asm" | "dynamic_cast" | "namespace" | "reinterpret_cast" | "try" | "bool" | "explicit" | "new" | "static_cast"
      | "typeid" | "catch" | "false" | "operator" | "template" | "typename" | "class" | "friend" | "private" | "this"
      | "using" | "const_cast" | "inline" | "public" | "throw" | "virtual" | "delete" | "mutable" | "protected"
      | "true" | "wchar_t" | "and" | "bitand" | "compl" | "not_eq" | "or_eq" | "xor_eq" | "and_eq" | "bitor" | "not"
      | "or" | "xor" | "cin" | "endl" | "INT_MIN" | "iomanip" | "main" | "npos" | "std" | "cout" | "include"
      | "INT_MAX" | "iostream" | "MAX_RAND" | "NULL" | "string" ⇒ s"_$target"

    case t if t.forall(c ⇒ '_' == c || Character.isLetterOrDigit(c)) ⇒ t

    case _ ⇒ target.map {
      case 'Z' ⇒ "ZZ"
      case c if '_' == c || Character.isLetterOrDigit(c) ⇒ ""+c
      case c ⇒ f"Z$c%04X"
    }.mkString
  }
}
