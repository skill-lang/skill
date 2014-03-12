/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import java.io.File
import java.util.Date
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.ada.api._
import de.ust.skill.generator.ada.internal._
import de.ust.skill.ir._
import de.ust.skill.parser.Parser
import scala.collection.mutable.MutableList

/**
 * Entry point of the ada generator.
 */
object Main {
  private def printHelp: Unit = println("""
usage:
  [options] skillPath outPath

Opitions:
  -p packageName      set a package name used by all emitted code.
  -h1|h2|h3 content   overrides the content of the respective header line
  -u userName         set a user name
  -date date          set a custom date
""")

  /**
   * Takes an argument skill file name and generates a ada binding.
   */
  def main(args: Array[String]): Unit = {
    var m = new Main

    //processing command line arguments
    if (2 > args.length) {
      printHelp
    } else {

      m.setOptions(args.slice(0, args.length - 2)).ensuring(m._packagePrefix != "", "You have to specify a non-empty package name!")
      val skillPath = args(args.length - 2)
      m.outPath = args(args.length - 1)

      //parse argument code
      m.IR = Parser.process(new File(skillPath)).toList

      // create output using maker chain
      m.make;
    }
  }
}

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * A generator turns a set of skill declarations into a ada interface providing means of manipulating skill files
 * containing instances of the respective definitions.
 *
 * @author Timm Felden
 */
class Main extends FakeMain
	with PackageApiSpecMaker
	with PackageBodyMaker
	with PackageInternalSpecMaker
    with PackageSpecMaker
    with SkillBodyMaker
    with SkillSpecMaker
    with ByteReaderBodyMaker
    with ByteReaderSpecMaker
    with ByteWriterBodyMaker
    with ByteWriterSpecMaker
    with FileReaderBodyMaker
    with FileReaderSpecMaker
    with FileWriterBodyMaker
    with FileWriterSpecMaker
    with StateMakerBodyMaker
    with StateMakerSpecMaker {

  var outPath: String = null
  var IR: List[Declaration] = null

  override protected def mapTypeToId(t: Type, _f: Field): String = t match {
    case t: GroundType ⇒
      if (_f.isConstant()) {
        t.getName() match {
          case "i8"         ⇒ 0.toString
          case "i16"        ⇒ 1.toString
          case "i32"        ⇒ 2.toString
          case "i64"        ⇒ 3.toString
          case "v64"        ⇒ 4.toString
        }
      } else {
        t.getName() match {
          case "annotation" ⇒ 5.toString
          case "bool"       ⇒ 6.toString
          case "i8"         ⇒ 7.toString
          case "i16"        ⇒ 8.toString
          case "i32"        ⇒ 9.toString
          case "i64"        ⇒ 10.toString
          case "v64"        ⇒ 11.toString
          case "f32"        ⇒ 12.toString
          case "f64"        ⇒ 13.toString
          case "string"     ⇒ 14.toString
        }
    }

    case t: ConstantLengthArrayType ⇒ 15.toString
    case t: VariableLengthArrayType ⇒ 17.toString
    case t: ListType                ⇒ 18.toString
    case t: SetType                 ⇒ 19.toString
    case t: MapType					⇒ 20.toString

    case t: Declaration				⇒ s"""Long (State.Get_Type ("${t.getSkillName}").id)"""
  }

  /**
   * Translates types into ada type names.
   */
  override protected def mapType(t : Type, _d: Declaration, _f: Field): String = t match {
    case t: GroundType ⇒ t.getName() match {
      case "annotation" ⇒ "Skill_Type_Access"

      case "bool"       ⇒ "Boolean"

      case "i8"         ⇒ "i8"
      case "i16"        ⇒ "i16"
      case "i32"        ⇒ "i32"
      case "i64"        ⇒ "i64"
      case "v64"        ⇒ "v64"

      case "f32"        ⇒ "Float"
      case "f64"        ⇒ "Double"

      case "string"     ⇒ "SU.Unbounded_String"
    }

    case t: ConstantLengthArrayType ⇒ s"${_d.getSkillName.capitalize}_${_f.getSkillName.capitalize}_Array"
    case t: VariableLengthArrayType ⇒ s"${_d.getSkillName.capitalize}_${_f.getSkillName.capitalize}_Vector.Vector"
    case t: ListType ⇒ s"${_d.getSkillName.capitalize}_${_f.getSkillName.capitalize}_List.List"
    case t: SetType ⇒ s"${_d.getSkillName.capitalize}_${_f.getSkillName.capitalize}_Set.Set"
    case t: MapType ⇒ s"${_d.getSkillName.capitalize}_${_f.getSkillName.capitalize}_Map.Map"

    case t: Declaration ⇒ s"${t.getName()}_Type_Access"
  }

  protected def mapFileReader(d: Declaration, f: Field): String = {
    def inner(t: Type, _d: Declaration, _f: Field): String = {
      t match {
        case t: GroundType ⇒ t.getName() match {
          case "annotation" ⇒
            s"Read_Annotation (Input_Stream)"
          case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" ⇒
            s"Byte_Reader.Read_${mapType(t, _d, _f)} (Input_Stream)"
          case "string" ⇒
            s"Read_Unbounded_String (Input_Stream)"
        }
        case t: Declaration ⇒
          s"""Read_${t.getName}_Type (Input_Stream)"""
      }
    }

    f.getType match {
      case t: GroundType ⇒ t.getName() match {
        case "annotation" ⇒
      	  s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               Object.${f.getSkillName} := ${inner(f.getType, d, f)};"""

        case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" ⇒
          if (f.isConstant) {
            s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               if Object.Get_${f.getSkillName.capitalize} /= ${mapType(f.getType, d, f)} (Field_Declaration.Constant_Value) then
                  raise Skill_Parse_Error;
               end if;"""
          } else {
            s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               Object.${f.getSkillName} := ${inner(f.getType, d, f)};"""
          }

        case "string" ⇒
      	  s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               Object.${f.getSkillName} := ${inner(f.getType, d, f)};"""
      }

      case t: ConstantLengthArrayType ⇒
        s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               for I in 1 .. ${t.getLength} loop
                  Object.${f.getSkillName} (I) := ${inner(t.getBaseType, d, f)};
               end loop;"""

      case t: VariableLengthArrayType ⇒
        s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
               X : Natural := Natural (Byte_Reader.Read_v64 (Input_Stream));
            begin
               for I in 1 .. X loop
                  Object.${f.getSkillName}.Append (${inner(t.getBaseType, d, f)});
               end loop;"""

      case t: ListType ⇒
        s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
               X : Natural := Natural (Byte_Reader.Read_v64 (Input_Stream));
            begin
               for I in 1 .. X loop
                  Object.${f.getSkillName}.Append (${inner(t.getBaseType, d, f)});
               end loop;"""

      case t: SetType ⇒
        s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
               X : Natural := Natural (Byte_Reader.Read_v64 (Input_Stream));
            begin
               for I in 1 .. X loop
                  Object.${f.getSkillName}.Insert (${inner(t.getBaseType, d, f)});
               end loop;"""

     case t: MapType ⇒
       s"""begin
               null;"""

      case t: Declaration ⇒
        s"""   Object : ${t.getName}_Type_Access := ${t.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               Object.${f.getSkillName} := ${inner(f.getType, d, f)};"""
    }
  }

  protected def mapFileWriter(d: Declaration, f: Field): String = {
    def inner(t: Type, _d: Declaration, _f: Field, value: String): String = {
      t match {
        case t: GroundType ⇒ t.getName() match {
          case "annotation" ⇒
            s"Write_Annotation (Stream, ${value})"
          case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" ⇒
            s"Byte_Writer.Write_${mapType(t, _d, _f)} (Stream, ${value})"
          case "string" ⇒
            s"Write_Unbounded_String (Stream, ${value})"
        }
        case t: Declaration ⇒
          s"""Write_${t.getName}_Type (Stream, ${value})"""
      }
    }

    f.getType match {
      case t: GroundType ⇒ t.getName() match {
        case "annotation" ⇒
      	  s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               ${inner(f.getType, d, f, s"Object.${f.getSkillName}")};"""

        case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" ⇒
          s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               ${inner(f.getType, d, f, s"Object.${f.getSkillName}")};"""

        case "string" ⇒
      	  s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
      	    begin
               ${inner(f.getType, d, f, s"Object.${f.getSkillName}")};"""
      }

      case t: ConstantLengthArrayType ⇒
        s"""   Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               for I in Object.${f.getSkillName}'Range loop
                  ${inner(t.getBaseType, d, f, s"Object.${f.getSkillName} (I)")};
               end loop;"""

      case t: VariableLengthArrayType ⇒
        s"""   use ${mapType(f.getType, d, f).stripSuffix(".Vector")};

               Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
               X : ${mapType(f.getType, d, f)} := Object.${f.getSkillName};

               procedure Iterate (Position : Cursor) is
               begin
                  ${inner(t.getBaseType, d, f, s"${mapType(f.getType, d, f).stripSuffix(".Vector")}.Element (Position)")};
               end Iterate;
            begin
               Byte_Writer.Write_v64 (Stream, Long (X.Length));
               X.Iterate (Iterate'Access);"""

      case t: ListType ⇒
        s"""   use ${mapType(f.getType, d, f).stripSuffix(".List")};

               Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
               X : ${mapType(f.getType, d, f)} := Object.${f.getSkillName};

               procedure Iterate (Position : Cursor) is
               begin
                  ${inner(t.getBaseType, d, f, s"${mapType(f.getType, d, f).stripSuffix(".List")}.Element (Position)")};
               end Iterate;
            begin
               Byte_Writer.Write_v64 (Stream, Long (X.Length));
               X.Iterate (Iterate'Access);"""

      case t: SetType ⇒
        s"""   use ${mapType(f.getType, d, f).stripSuffix(".Set")};

               Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (State.Get_Object (Type_Name, I));
               X : ${mapType(f.getType, d, f)} := Object.${f.getSkillName};

               procedure Iterate (Position : Cursor) is
               begin
                  ${inner(t.getBaseType, d, f, s"${mapType(f.getType, d, f).stripSuffix(".Set")}.Element (Position)")};
               end Iterate;
            begin
               Byte_Writer.Write_v64 (Stream, Long (X.Length));
               X.Iterate (Iterate'Access);"""

      case t: MapType ⇒
        s"""begin
               null;"""

      case t: Declaration ⇒
        s"""begin
               null;"""
    }
  }

  protected def getSuperTypes(d: Declaration): MutableList[String] = {
    if (null == d.getSuperType) MutableList[String]()
    else getSuperTypes (d.getSuperType) += d.getSuperType.getSkillName
  }

  /**
   * provides the package prefix
   */
  override protected def packagePrefix(): String = _packagePrefix
  private var _packagePrefix = ""

  override private[ada] def header: String = _header
  private var _header = ""

  private def setOptions(args: Array[String]) {
    var index = 0
    var headerLine1: Option[String] = None
    var headerLine2: Option[String] = None
    var headerLine3: Option[String] = None
    var userName: Option[String] = None
    var date: Option[String] = None

    while (index < args.length) args(index) match {
      case "-p"    ⇒ _packagePrefix = args(index + 1); index += 2;
      case "-u"    ⇒ userName = Some(args(index + 1)); index += 2;
      case "-date" ⇒ date = Some(args(index + 1)); index += 2;
      case "-h1"   ⇒ headerLine1 = Some(args(index + 1)); index += 2;
      case "-h2"   ⇒ headerLine2 = Some(args(index + 1)); index += 2;
      case "-h3"   ⇒ headerLine3 = Some(args(index + 1)); index += 2;

      case unknown ⇒ sys.error(s"unkown Argument: $unknown")
    }

    // create header from options
    val headerLineLength = 51
    headerLine1 = Some((headerLine1 match {
      case Some(s) ⇒ s
      case None    ⇒ "Your SKilL Ada Binding"
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    headerLine2 = Some((headerLine2 match {
      case Some(s) ⇒ s
      case None ⇒ "generated: "+(date match {
        case Some(s) ⇒ s
        case None    ⇒ (new java.text.SimpleDateFormat("dd.MM.yyyy")).format(new Date)
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))
    headerLine3 = Some((headerLine3 match {
      case Some(s) ⇒ s
      case None ⇒ "by: "+(userName match {
        case Some(s) ⇒ s
        case None    ⇒ System.getProperty("user.name")
      })
    }).padTo(headerLineLength, " ").mkString.substring(0, headerLineLength))

    _header = s"""--  ___ _  ___ _ _                                                            --
-- / __| |/ (_) | |       ${headerLine1.get} --
-- \\__ \\ ' <| | | |__     ${headerLine2.get} --
-- |___/_|\\_\\_|_|____|    ${headerLine3.get} --
--                                                                            --
"""
  }

  override protected def defaultValue(t: Type, _d: Declaration, _f: Field) = t match {
    case t: GroundType ⇒ t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0f"
      case "bool"                               ⇒ "False"
      case "string"                             ⇒ s"""SU.To_Unbounded_String("")"""
      case _                                    ⇒ "null"
    }

    case t: ConstantLengthArrayType ⇒ s"(others => ${defaultValue(t.getBaseType, _d, _f)})"
    case t: VariableLengthArrayType ⇒ s"New_${mapType(t, _d, _f).stripSuffix(".Vector")}"
    case t: ListType ⇒ s"New_${mapType(t, _d, _f).stripSuffix(".List")}"
    case t: SetType ⇒ s"New_${mapType(t, _d, _f).stripSuffix(".Set")}"
    case t: MapType ⇒ s"New_${mapType(t, _d, _f).stripSuffix(".Map")}"

    case _ ⇒ "null"
  }

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  protected def escaped(target: String): String = target match {
    //keywords get a suffix "_", because that way at least auto-completion will work as expected
    case "abstract" | "case" | "catch" | "class" | "def" | "do" | "else" | "extends" | "false" | "final" | "finally" |
      "for" | "forSome" | "if" | "implicit" | "import" | "lazy" | "match" | "new" | "null" | "object" | "override" |
      "package" | "private" | "protected" | "return" | "sealed" | "super" | "this" | "throw" | "trait" | "true" |
      "try" | "type" | "var" | "while" | "with" | "yield" | "val" ⇒ target+"_"

    //the string is fine anyway
    case _ ⇒ target
  }
}
