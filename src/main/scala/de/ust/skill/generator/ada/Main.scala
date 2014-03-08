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
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.VariableLengthArrayType
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

  /**
   * Translates types into ada type names.
   */
  override protected def mapType(t: Type): String = t match {
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

    case t: ConstantLengthArrayType ⇒ "null"

    case t: Declaration ⇒ s"${t.getName()}_Type_Access"
  }

  override protected def mapTypeToId(t: Type): Long = t match {
    case t: GroundType ⇒ t.getName() match {
      case "annotation" ⇒ 5
      case "bool"       ⇒ 6
      case "i8"         ⇒ 7
      case "i16"        ⇒ 8
      case "i32"        ⇒ 9
      case "i64"        ⇒ 10
      case "v64"        ⇒ 11
      case "f32"        ⇒ 12
      case "f64"        ⇒ 13
      case "string"     ⇒ 14
    }

    case t: ConstantLengthArrayType ⇒ 15
    case t: VariableLengthArrayType ⇒ 17
    case t: ListType                ⇒ 18
    case t: SetType                 ⇒ 19
    case t: MapType					⇒ 20

    case t: Declaration				⇒ 32
  }

  protected def mapFileReader(t: Type, f: Field): String = f.getType match {
    case ft: GroundType ⇒ ft.getName() match {
      case "annotation" ⇒
      	s"""   Object : ${t.getName}_Type_Access := ${t.getName}_Type_Access (State.Get_Object (Type_Name, I));
               X : v64 := Byte_Reader.Read_v64 (Input_Stream);
               Y : v64 := Byte_Reader.Read_v64 (Input_Stream);
            begin
               if 0 /= X then
                  Object.${f.getSkillName} := State.Get_Object (State.Get_String (X), Positive (Y));
               end if;"""

      case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" ⇒
        if (f.isConstant) {
          s"""   Object : ${t.getName}_Type_Access := ${t.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               if Object.Get_${f.getSkillName.capitalize} /= ${mapType(f.getType)} (Field_Declaration.Constant_Value) then
                  raise Skill_Parse_Error;
               end if;"""
        } else {
          s"""   Object : ${t.getName}_Type_Access := ${t.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               Object.${f.getSkillName} := Byte_Reader.Read_${mapType(f.getType)} (Input_Stream);"""
        }

      case "string" ⇒
      	s"""   Object : ${t.getName}_Type_Access := ${t.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               Object.${f.getSkillName} := SU.To_Unbounded_String (State.Get_String (Byte_Reader.Read_v64 (Input_Stream)));"""
    }

    case t: ConstantLengthArrayType ⇒
      s"""   begin
               null;"""

    case t: Declaration ⇒
      s"""   Object : ${t.getName}_Type_Access := ${t.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               Object.${f.getSkillName} := ${t.getName}_Type_Access (State.Get_Object ("${
                val superTypes = getSuperTypes(t).toList;
                if (superTypes.length > 0) superTypes(0); else t.getSkillName
              }", Positive (Byte_Reader.Read_v64 (Input_Stream))));"""
  }

  protected def mapFileWriter(t: Type, f: Field): String = f.getType match {
    case ft: GroundType ⇒ ft.getName() match {
      case "annotation" ⇒
      	s"""   Object : ${t.getName}_Type_Access := ${t.getName}_Type_Access (State.Get_Object (Type_Name, I));
               Type_Name : String := Get_Annotation_Type (Object.${f.getSkillName});
            begin
               if 0 = Type_Name'Length then
                  Byte_Writer.Write_v64 (Stream, 0);
                  Byte_Writer.Write_v64 (Stream, 0);
               else
                  Byte_Writer.Write_v64 (Stream, Long (State.Get_String_Index (Type_Name)));
                  Byte_Writer.Write_v64 (Stream, Long (Object.${f.getSkillName}.skill_id));
               end if;"""

      case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" ⇒
        s"""   Object : ${t.getName}_Type_Access := ${t.getName}_Type_Access (State.Get_Object (Type_Name, I));
            begin
               Byte_Writer.Write_${mapType(f.getType)} (Stream, Object.${f.getSkillName});"""

      case "string" ⇒
      	s"""   Object : ${t.getName}_Type_Access := ${t.getName}_Type_Access (State.Get_Object (Type_Name, I));
      	    begin
               Byte_Writer.Write_v64 (Stream, Long (State.Get_String_Index (SU.To_String (Object.${f.getSkillName}))));"""
    }

    case t: ConstantLengthArrayType ⇒
      s"""begin
               null;"""

    case t: Declaration ⇒
      s"""begin
               null;"""
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

  override protected def defaultValue(f: Field) = f.getType() match {
    case t: GroundType ⇒ t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0f"
      case "bool"                               ⇒ "False"
      case "string"                             ⇒ s"""SU.To_Unbounded_String("")"""
      case _                                    ⇒ "null"
    }

    // TODO compound types would behave more nicely if they would be initialized with empty collections instead of null

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
