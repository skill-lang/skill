/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.util.Date

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.HashMap

import de.ust.skill.generator.scala.api.SkillFileMaker
import de.ust.skill.generator.scala.api.internal.FieldDeclarationMaker
import de.ust.skill.generator.scala.api.internal.FileParserMaker
import de.ust.skill.generator.scala.api.internal.PoolsMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.FieldLike
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.restriction.DefaultRestriction
import de.ust.skill.ir.restriction.FloatDefaultRestriction
import de.ust.skill.ir.restriction.FloatRangeRestriction
import de.ust.skill.ir.restriction.IntDefaultRestriction
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.NameDefaultRestriction
import de.ust.skill.ir.restriction.RangeRestriction
import de.ust.skill.ir.restriction.StringDefaultRestriction

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
    with FieldDeclarationMaker
    with FileParserMaker
    with SkillFileMaker
    with DependenciesMaker
    with PoolsMaker
    with TypesMaker {

  lineLength = 120
  override def comment(d : Declaration) : String = d.getComment.format("/**\n", " * ", lineLength, " */\n")
  override def comment(f : FieldLike) : String = f.getComment.format("/**\n", "   * ", lineLength, "   */\n  ")

  /**
   * Translates types into scala type names.
   */
  override protected def mapType(t : Type) : String = t match {
    case t : GroundType ⇒ t.getName.lower match {
      case "annotation" ⇒ "SkillObject"

      case "bool"       ⇒ "scala.Boolean"

      case "i8"         ⇒ "Byte"
      case "i16"        ⇒ "Short"
      case "i32"        ⇒ "Int"
      case "i64"        ⇒ "Long"
      case "v64"        ⇒ "Long"

      case "f32"        ⇒ "Float"
      case "f64"        ⇒ "Double"

      case "string"     ⇒ "java.lang.String"
    }

    case t : ConstantLengthArrayType ⇒ s"$ArrayTypeName[${mapType(t.getBaseType())}]"
    case t : VariableLengthArrayType ⇒ s"$VarArrayTypeName[${mapType(t.getBaseType())}]"
    case t : ListType                ⇒ s"$ListTypeName[${mapType(t.getBaseType())}]"
    case t : SetType                 ⇒ s"$SetTypeName[${mapType(t.getBaseType())}]"
    case t : MapType                 ⇒ t.getBaseTypes().map(mapType).reduceRight((k, v) ⇒ s"$MapTypeName[$k, $v]")

    case t : Declaration             ⇒ "_root_." + packagePrefix + name(t)

    case _                           ⇒ throw new IllegalStateException(s"Unknown type $t")
  }

  /**
   * creates argument list of a constructor call, not including potential skillID or braces
   */
  override protected def makeConstructorArguments(t : UserType) = (
    for (f ← t.getAllFields if !(f.isConstant || f.isIgnored))
      yield s"${escaped(f.getName.camel)} : ${mapType(f.getType())} = ${defaultValue(f)}").mkString(", ")
  override protected def appendConstructorArguments(t : UserType) = {
    val r = t.getAllFields.filterNot { f ⇒ f.isConstant || f.isIgnored }
    if (r.isEmpty) ""
    else r.map({ f ⇒ s"${escaped(f.getName.camel)} : ${mapType(f.getType())}" }).mkString(", ", ", ", "")
  }

  /**
   * Provide a nice file header:)
   */
  override private[scala] def header : String = _header
  private lazy val _header = {
    // create header from options
    val headerLineLength = 51
    val headerLine1 = Some((headerInfo.line1 match {
      case Some(s) ⇒ s
      case None    ⇒ headerInfo.license.map("LICENSE: " + _).getOrElse("Your SKilL Scala Binding")
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

  override def setOption(option : String, value : String) = option match {
    case "revealskillid" ⇒ revealSkillID = ("true" == value)
    case unknown         ⇒ sys.error(s"unkown Argument: $unknown")
  }

  override def printHelp : Unit = println("""
Opitions (scala):
  revealSkillID: true/false  if set to true, the generated binding will reveal SKilL IDs in the API
""")

  override def customFieldManual = """
!import string+    A list of imports that will be added where required.
!modifier string   A modifier, that will be put in front of the variable declaration."""

  override protected def defaultValue(f : Field) : String = {
    val fr = f.getRestrictions.collect {
      case f : DefaultRestriction ⇒ f
      case f : RangeRestriction   ⇒ f
    };
    if (!fr.isEmpty) {
      fr.find { x ⇒ x.isInstanceOf[DefaultRestriction] }.map(
        _ match {
          case r : IntDefaultRestriction ⇒ r.getValue.toString
          case r : FloatDefaultRestriction if f.getType.getSkillName.equals("f64") ⇒ r.getValue.toString
          case r : FloatDefaultRestriction ⇒ r.getValue.toString + "f"
          case r : StringDefaultRestriction ⇒ "\"" + r.getValue + "\""
          case r : NameDefaultRestriction ⇒

            f.getType.getSkillName match {
              case "bool" ⇒
                if (r.getValue.head.equals("true")) "true"
                else "false"

              case "v64" ⇒ "0"
              case _     ⇒ null
            }

          case _ ⇒ null // TODO find a way to access the state object
        }).getOrElse {
          fr.collect {
            case r : FloatRangeRestriction ⇒
              if (r.getHighDouble < 0.0 || r.getLowDouble > 0.0) {
                if (f.getType.getSkillName.equals("f64")) r.getLowDouble.toString
                else r.getLowFloat.toString + "f"
              } else "0.0f"

            case r : IntRangeRestriction ⇒
              if (r.getHigh < 0L || r.getLow > 0L) {
                if (f.getType.getSkillName.endsWith("64")) r.getLow.toString + "L"
                else math.max(r.getLow, Int.MinValue).toString
              } else "0"
          }.head
        }
    } else
      // TODO type default restrictions

      // else -> language defined default
      f.getType match {
        case t : GroundType ⇒ t.getSkillName() match {
          case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
          case "f32" | "f64"                        ⇒ "0.0f"
          case "bool"                               ⇒ "false"
          case _                                    ⇒ "null"
        }

        case t : UserType      ⇒ "null"
        case t : InterfaceType ⇒ "null"

        case _                 ⇒ mapType(f.getType) + "()"
      }
  }

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  private val escapeCache = new HashMap[String, String]();
  final def escaped(target : String) : String = escapeCache.getOrElse(target, {
    val result = target match {
      //keywords get a suffix "_", because that way at least auto-completion will work as expected
      case "abstract" | "case" | "catch" | "class" | "def" | "do" | "else" | "extends" | "false" | "final" | "finally" |
        "for" | "forSome" | "if" | "implicit" | "import" | "lazy" | "match" | "new" | "null" | "object" | "override" |
        "package" | "private" | "protected" | "return" | "sealed" | "super" | "this" | "throw" | "trait" | "true" |
        "try" | "type" | "var" | "while" | "with" | "yield" | "val" ⇒ s"`$target`"

      case t if t.forall(c ⇒ '_' == c || Character.isLetterOrDigit(c)) ⇒ t

      case _ ⇒ s"`$target`"
    }
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
