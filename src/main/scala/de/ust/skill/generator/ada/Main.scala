/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import java.io.File
import java.util.Date
import scala.collection.JavaConversions._
import de.ust.skill.generator.ada.api._
import de.ust.skill.generator.ada.internal._
import de.ust.skill.ir._
import de.ust.skill.parser.Parser
import scala.collection.mutable.MutableList
import scala.util.matching.Regex
import de.ust.skill.generator.ada.api.internal.PackageInternalSpecMaker

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

    case t: Declaration				⇒ s"""Long (Types.Element ("${t.getSkillName}").id)"""
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

      case "f32"        ⇒ "f32"
      case "f64"        ⇒ "f64"

      case "string"     ⇒ "String_Access"
    }

    case t: ConstantLengthArrayType ⇒ s"${_d.getSkillName.capitalize}_${_f.getSkillName.capitalize}_Array"
    case t: VariableLengthArrayType ⇒ s"${_d.getSkillName.capitalize}_${_f.getSkillName.capitalize}_Vector.Vector"
    case t: ListType ⇒ s"${_d.getSkillName.capitalize}_${_f.getSkillName.capitalize}_List.List"
    case t: SetType ⇒ s"${_d.getSkillName.capitalize}_${_f.getSkillName.capitalize}_Set.Set"
    case t: MapType ⇒ s"${_d.getSkillName.capitalize}_${_f.getSkillName.capitalize}_Map.Map"

    case t: Declaration ⇒ s"${escaped(t.getName())}_Type_Access"
  }

  protected def mapFileReader(d: Declaration, f: Field): String = {
    def inner(t: Type, _d: Declaration, _f: Field): String = {
      t match {
        case t: GroundType ⇒ t.getName() match {
          case "annotation" ⇒
            s"Read_Annotation (Input_Stream)"
          case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
            s"Byte_Reader.Read_${mapType(t, _d, _f)} (Input_Stream)"
          case "string" ⇒
            s"Read_String (Input_Stream)"
        }
        case t: Declaration ⇒
          s"""Read_${escaped(t.getName)}_Type (Input_Stream)"""
      }
    }

    f.getType match {
      case t: GroundType ⇒ t.getName() match {
        case "annotation" ⇒
      	  s"""begin
               Object.${f.getSkillName} := ${inner(f.getType, d, f)};"""

        case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
          if (f.isConstant) {
            s"""   Skill_Parse_Constant_Error : exception;
            begin
               if Object.Get_${f.getSkillName.capitalize} /= ${mapType(f.getType, d, f)} (Field_Declaration.Constant_Value) then
                  raise Skill_Parse_Constant_Error;
               end if;"""
          } else {
            s"""begin
               Object.${f.getSkillName} := ${inner(f.getType, d, f)};"""
          }

        case "string" ⇒
      	  s"""begin
               Object.${f.getSkillName} := ${inner(f.getType, d, f)};"""
      }

      case t: ConstantLengthArrayType ⇒
        s"""begin
               declare
                  Skill_Parse_Constant_Array_Length_Error : exception;
               begin
                  if ${t.getLength} /= Field_Declaration.Constant_Array_Length then
                     raise Skill_Parse_Constant_Array_Length_Error;
                  end if;
               end;
               for I in 1 .. ${t.getLength} loop
                  Object.${f.getSkillName} (I) := ${inner(t.getBaseType, d, f)};
               end loop;"""

      case t: VariableLengthArrayType ⇒
        s"""begin
               for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                  Object.${f.getSkillName}.Append (${inner(t.getBaseType, d, f)});
               end loop;"""

      case t: ListType ⇒
        s"""begin
               for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                  Object.${f.getSkillName}.Append (${inner(t.getBaseType, d, f)});
               end loop;"""

      case t: SetType ⇒
        s"""begin
               for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                  Object.${f.getSkillName}.Insert (${inner(t.getBaseType, d, f)});
               end loop;"""

     case t: MapType ⇒
       s"""${
  var output = ""
  val types = t.getBaseTypes().reverse
  types.slice(0, types.length-1).zipWithIndex.foreach({ case (t, i) =>
    val x = {
      if (0 == i)
        s"""declare
                        X : ${mapType(types.get(i+1), d, f)} := ${inner(types.get(i+1), d, f)};
                        Y : ${mapType(types.get(i), d, f)} := ${inner(types.get(i), d, f)};
                     begin
                        Map.Insert (X, Y);
                     end;"""
      else
        s"""declare
                        X : ${mapType(types.get(i+1), d, f)} := ${inner(types.get(i+1), d, f)};
                     begin
                        Map.Insert (X, Read_Map_${types.length-i});
                     end;"""
    }
    output += s"""               function Read_Map_${types.length-(i+1)} return ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length-(i+1)}.Map is
                  Map : ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length-(i+1)}.Map;
               begin
                  for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                     ${x}
                  end loop;
                  return Map;
               end Read_Map_${types.length-(i+1)};
               pragma Inline (Read_Map_${types.length-(i+1)});\r\n\r\n"""
  })
  output.stripLineEnd.stripLineEnd
}
            begin
               Object.f := Read_Map_1;"""

      case t: Declaration ⇒
        s"""begin
               Object.${f.getSkillName} := ${inner(f.getType, d, f)};"""
    }
  }

  protected def mapFileWriter(d: Declaration, f: Field): String = {
    def inner(t: Type, _d: Declaration, _f: Field, value: String): String = {
      t match {
        case t: GroundType ⇒ t.getName() match {
          case "annotation" ⇒
            s"Write_Annotation (Stream, ${value})"
          case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
            s"Byte_Writer.Write_${mapType(t, _d, _f)} (Stream, ${value})"
          case "string" ⇒
            s"Write_String (Stream, ${value})"
        }
        case t: Declaration ⇒
          s"""Write_${escaped(t.getName)}_Type (Stream, ${value})"""
      }
    }

    f.getType match {
      case t: GroundType ⇒ t.getName() match {
        case "annotation" ⇒
      	  s"""begin
               ${inner(f.getType, d, f, s"Object.${f.getSkillName}")};"""

        case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
          s"""begin
               ${inner(f.getType, d, f, s"Object.${f.getSkillName}")};"""

        case "string" ⇒
      	  s"""begin
               ${inner(f.getType, d, f, s"Object.${f.getSkillName}")};"""
      }

      case t: ConstantLengthArrayType ⇒
        s"""begin
               for I in Object.${f.getSkillName}'Range loop
                  ${inner(t.getBaseType, d, f, s"Object.${f.getSkillName} (I)")};
               end loop;"""

      case t: VariableLengthArrayType ⇒
        s"""   use ${mapType(f.getType, d, f).stripSuffix(".Vector")};

               Vector : ${mapType(f.getType, d, f)} := Object.${f.getSkillName};

               procedure Iterate (Position : Cursor) is
               begin
                  ${inner(t.getBaseType, d, f, s"Element (Position)")};
               end Iterate;
               pragma Inline (Iterate);
            begin
               Byte_Writer.Write_v64 (Stream, Long (Vector.Length));
               Vector.Iterate (Iterate'Access);"""

      case t: ListType ⇒
        s"""   use ${mapType(f.getType, d, f).stripSuffix(".List")};

               List : ${mapType(f.getType, d, f)} := Object.${f.getSkillName};

               procedure Iterate (Position : Cursor) is
               begin
                  ${inner(t.getBaseType, d, f, s"Element (Position)")};
               end Iterate;
               pragma Inline (Iterate);
            begin
               Byte_Writer.Write_v64 (Stream, Long (List.Length));
               List.Iterate (Iterate'Access);"""

      case t: SetType ⇒
        s"""   use ${mapType(f.getType, d, f).stripSuffix(".Set")};

               Set : ${mapType(f.getType, d, f)} := Object.${f.getSkillName};

               procedure Iterate (Position : Cursor) is
               begin
                  ${inner(t.getBaseType, d, f, s"Element (Position)")};
               end Iterate;
               pragma Inline (Iterate);
            begin
               Byte_Writer.Write_v64 (Stream, Long (Set.Length));
               Set.Iterate (Iterate'Access);"""

      case t: MapType ⇒
        s"""${
  var output = ""
  val types = t.getBaseTypes().reverse
  types.slice(0, types.length-1).zipWithIndex.foreach({ case (t, i) =>
    val x = {
      if (0 == i)
        s"""${inner(types.get(i+1), d, f, "Key (Position)")};
                     ${inner(types.get(i), d, f, "Element (Position)")};"""
      else
        s"""${inner(types.get(i+1), d, f, "Key (Position)")};
                     Write_Map_${types.length-i} (Element (Position));"""
    }
    output += s"""   ${if (types.length-(i+1) == types.length-1) "" else "               "}procedure Write_Map_${types.length-(i+1)} (Map : ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length-(i+1)}.Map) is
                  use ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length-(i+1)};

                  procedure Iterate (Position : Cursor) is
                  begin
                     ${x}
                  end Iterate;
                  pragma Inline (Iterate);
               begin
                  Byte_Writer.Write_v64 (Stream, Long (Map.Length));
                  Map.Iterate (Iterate'Access);
               end Write_Map_${types.length-(i+1)};
               pragma Inline (Write_Map_${types.length-(i+1)});\r\n\r\n"""
  })
  output.stripLineEnd.stripLineEnd
}
            begin
               Write_Map_1 (Object.${f.getSkillName});"""

      case t: Declaration ⇒
        s"""begin
               ${inner(f.getType, d, f, s"Object.${f.getSkillName}")};"""
    }
  }

  protected def getSuperTypes(d: Declaration): MutableList[Type] = {
    if (null == d.getSuperType) MutableList[Type]()
    else getSuperTypes (d.getSuperType) += d.getSuperType
  }
  
  protected def getSubTypes(d: Declaration): MutableList[Type] = {
    var rval = MutableList[Type]()

    1 to IR.length foreach { _ =>
	  for (_d ← IR) {
        if (d == _d.getSuperType && -1 == rval.indexOf(_d)) rval += _d // element ist subtyp und noch nicht erfasst
        if (-1 < rval.indexOf(_d.getSuperType) && -1 == rval.indexOf(_d)) rval += _d // element ist subtyp aus element in der liste und noch nicht erfasst
	  }
    }

    rval
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
      case "f32" | "f64"                        ⇒ "0.0"
      case "bool"                               ⇒ "False"
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
  protected def escaped(pTarget: String): String = {
    val target = pTarget.replaceAll("([A-Z])", "_$0").stripPrefix("_")

    target match {
      // keywords get a suffix "_2", because that way at least auto-completion will work as expected
      case "abstract" | "case" | "catch" | "class" | "def" | "do" | "else" | "extends" | "false" | "final" | "finally" |
        "for" | "forSome" | "if" | "implicit" | "import" | "lazy" | "match" | "new" | "null" | "object" | "override" |
        "package" | "private" | "protected" | "return" | "sealed" | "super" | "this" | "throw" | "trait" | "true" |
        "try" | "type" | "var" | "while" | "with" | "yield" | "val" ⇒ return target+"_2" + "asd"

      // the string is fine anyway
      case _ ⇒ return target
    }
  }
}
