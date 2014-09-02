/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import de.ust.skill.generator.ada.api._
import de.ust.skill.generator.ada.api.internal.PackageInternalSpecMaker
import de.ust.skill.generator.ada.internal._
import de.ust.skill.ir._
import de.ust.skill.parser.Parser
import java.io.File
import java.util.Date
import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList
import de.ust.skill.generator.common.Generator

/**
 * Entry point of the Ada generator.
 */
object Main {
  private def printHelp : Unit = println("""
usage:
  [options] skillPath outPath

Opitions:
  -p packageName      set a package name used by all emitted code.
  -h1|h2|h3 content   overrides the content of the respective header line
  -u userName         set a user name
  -date date          set a custom date
""")

  /**
   * Takes an argument skill file name and generates an Ada binding.
   */
  def main(args : Array[String]) : Unit = {
    var m = new Main

    //processing command line arguments
    if (2 > args.length) {
      printHelp
    } else {

      m.setOptions(args.slice(0, args.length - 2)).ensuring(m._packagePrefix != "", "You have to specify a non-empty package name!")
      val skillPath = args(args.length - 2)
      m.outPath = args(args.length - 1)

      //parse argument code
      m.setIR(Parser.process(new File(skillPath)).toList)

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
 * A generator turns a set of skill declarations into an Ada interface providing means of manipulating skill files
 * containing instances of the respective UserTypes.
 *
 * @author Timm Felden, Dennis Przytarski
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

  /**
   * Translates the types into the skill type id's.
   */
  override protected def mapTypeToId(t : Type, f : Field) : String = t match {
    case t : GroundType ⇒
      if (f.isConstant()) {
        t.getName.lower match {
          case "i8"  ⇒ 0.toString
          case "i16" ⇒ 1.toString
          case "i32" ⇒ 2.toString
          case "i64" ⇒ 3.toString
          case "v64" ⇒ 4.toString
        }
      } else {
        t.getName.lower match {
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

    case t : ConstantLengthArrayType ⇒ 15.toString
    case t : VariableLengthArrayType ⇒ 17.toString
    case t : ListType                ⇒ 18.toString
    case t : SetType                 ⇒ 19.toString
    case t : MapType                 ⇒ 20.toString

    case t : Declaration             ⇒ s"""Long (Types.Element ("${t.getSkillName}").id)"""
  }

  /**
   * Translates the types into Ada types.
   */
  override protected def mapType(t : Type, d : UserType, f : Field) : String = t match {
    case t : GroundType ⇒ t.getName.lower match {
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

    case t : ConstantLengthArrayType ⇒ s"${d.getSkillName.capitalize}_${f.getSkillName.capitalize}_Array"
    case t : VariableLengthArrayType ⇒ s"${d.getSkillName.capitalize}_${f.getSkillName.capitalize}_Vector.Vector"
    case t : ListType                ⇒ s"${d.getSkillName.capitalize}_${f.getSkillName.capitalize}_List.List"
    case t : SetType                 ⇒ s"${d.getSkillName.capitalize}_${f.getSkillName.capitalize}_Set.Set"
    case t : MapType                 ⇒ s"${d.getSkillName.capitalize}_${f.getSkillName.capitalize}_Map.Map"

    case t : Declaration             ⇒ s"${escaped(t.getName.ada)}_Type_Access"
  }

  /**
   * Generates the read functions into the procedure Read_Queue_Vector_Iterator in package File_Reader.
   */
  protected def mapFileReader(d : UserType, f : Field) : String = {
    /**
     * The basis type of the field that will be read.
     */
    def inner(t : Type, _d : UserType, _f : Field) : String = {
      t match {
        case t : GroundType ⇒ t.getName.lower match {
          case "annotation" ⇒
            s"Read_Annotation (Input_Stream)"
          case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
            s"Byte_Reader.Read_${mapType(t, _d, _f)} (Input_Stream)"
          case "string" ⇒
            s"Read_String (Input_Stream)"
        }
        case t : Declaration ⇒
          s"""Read_${escaped(t.getName.lower)}_Type (Input_Stream)"""
      }
    }

    f.getType() match {
      case t : GroundType ⇒ t.getName.lower match {
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

      case t : ConstantLengthArrayType ⇒
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

      case t : VariableLengthArrayType ⇒
        s"""begin
               for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                  Object.${f.getSkillName}.Append (${inner(t.getBaseType, d, f)});
               end loop;"""

      case t : ListType ⇒
        s"""begin
               for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                  Object.${f.getSkillName}.Append (${inner(t.getBaseType, d, f)});
               end loop;"""

      case t : SetType ⇒
        s"""begin
               for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                  Object.${f.getSkillName}.Insert (${inner(t.getBaseType, d, f)});
               end loop;"""

      case t : MapType ⇒
        s"""${
          var output = ""
          val types = t.getBaseTypes().reverse
          types.slice(0, types.length - 1).zipWithIndex.foreach({
            case (t, i) ⇒
              val x = {
                if (0 == i)
                  s"""declare
                        Key   : ${mapType(types.get(i + 1), d, f)} := ${inner(types.get(i + 1), d, f)};
                        Value : ${mapType(types.get(i), d, f)} := ${inner(types.get(i), d, f)};
                     begin
                        Map.Insert (Key, Value);
                     end;"""
                else
                  s"""declare
                        Key : ${mapType(types.get(i + 1), d, f)} := ${inner(types.get(i + 1), d, f)};
                     begin
                        Map.Insert (Key, Read_Map_${types.length - i});
                     end;"""
              }
              output += s"""               function Read_Map_${types.length - (i + 1)} return ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length - (i + 1)}.Map is
                  Map : ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length - (i + 1)}.Map;
               begin
                  for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                     ${x}
                  end loop;
                  return Map;
               end Read_Map_${types.length - (i + 1)};
               pragma Inline (Read_Map_${types.length - (i + 1)});\r\n\r\n"""
          })
          output.stripLineEnd.stripLineEnd
        }
            begin
               Object.f := Read_Map_1;"""

      case t : Declaration ⇒
        s"""begin
               Object.${f.getSkillName} := ${inner(f.getType, d, f)};"""
    }
  }

  /**
   * Generates the write functions into the procedure Write_Field_Data in package File_Writer.
   */
  protected def mapFileWriter(d : UserType, f : Field) : String = {
    /**
     * The basis type of the field that will be written.
     */
    def inner(t : Type, _d : UserType, _f : Field, value : String) : String = {
      t match {
        case t : GroundType ⇒ t.getName.lower match {
          case "annotation" ⇒
            s"Write_Annotation (Stream, ${value})"
          case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
            s"Byte_Writer.Write_${mapType(t, _d, _f)} (Stream, ${value})"
          case "string" ⇒
            s"Write_String (Stream, ${value})"
        }
        case t : Declaration ⇒
          s"""Write_${escaped(t.getName.ada)}_Type (Stream, ${value})"""
      }
    }

    f.getType match {
      case t : GroundType ⇒ t.getName.lower match {
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

      case t : ConstantLengthArrayType ⇒
        s"""begin
               for I in Object.${f.getSkillName}'Range loop
                  ${inner(t.getBaseType, d, f, s"Object.${f.getSkillName} (I)")};
               end loop;"""

      case t : VariableLengthArrayType ⇒
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

      case t : ListType ⇒
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

      case t : SetType ⇒
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

      case t : MapType ⇒
        s"""${
          var output = ""
          val types = t.getBaseTypes().reverse
          types.slice(0, types.length - 1).zipWithIndex.foreach({
            case (t, i) ⇒
              val x = {
                if (0 == i)
                  s"""${inner(types.get(i + 1), d, f, "Key (Position)")};
                     ${inner(types.get(i), d, f, "Element (Position)")};"""
                else
                  s"""${inner(types.get(i + 1), d, f, "Key (Position)")};
                     Write_Map_${types.length - i} (Element (Position));"""
              }
              output += s"""   ${if (types.length - (i + 1) == types.length - 1) "" else "               "}procedure Write_Map_${types.length - (i + 1)} (Map : ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length - (i + 1)}.Map) is
                  use ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length - (i + 1)};

                  procedure Iterate (Position : Cursor) is
                  begin
                     ${x}
                  end Iterate;
                  pragma Inline (Iterate);
               begin
                  Byte_Writer.Write_v64 (Stream, Long (Map.Length));
                  Map.Iterate (Iterate'Access);
               end Write_Map_${types.length - (i + 1)};
               pragma Inline (Write_Map_${types.length - (i + 1)});\r\n\r\n"""
          })
          output.stripLineEnd.stripLineEnd
        }
            begin
               Write_Map_1 (Object.${f.getSkillName});"""

      case t : Declaration ⇒
        s"""begin
               ${inner(f.getType, d, f, s"Object.${f.getSkillName}")};"""
    }
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

    1 to IR.length foreach { _ ⇒
      for (_d ← IR) {
        // element is sub type and not collected
        if (d == _d.getSuperType && -1 == rval.indexOf(_d)) rval += _d
        // element is listed sub type and not collected
        if (-1 < rval.indexOf(_d.getSuperType) && -1 == rval.indexOf(_d)) rval += _d
      }
    }

    rval
  }

  /**
   * Gets the fields as parameters of a given type.
   */
  def printParameters(d : UserType) : String = {
    var output = "";
    var hasFields = false;
    output += d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map({ f ⇒
      hasFields = true;
      s"${f.getSkillName()} : ${mapType(f.getType, d, f)}"
    }).mkString("; ", "; ", "")
    if (hasFields) output else ""
  }

  /**
   * Provides the package prefix.
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    _packagePrefix = names.foldRight("")(_+"."+_)
  }

  override private[ada] def header : String = _header
  private lazy val _header = {
    // create header from options
    val headerLineLength = 51
    val headerLine1 = Some((headerInfo.line1 match {
      case Some(s) ⇒ s
      case None    ⇒ headerInfo.license.map("LICENSE: "+_).getOrElse("Your SKilL Scala Binding")
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

    s"""--  ___ _  ___ _ _                                                            --
-- / __| |/ (_) | |       ${headerLine1.get} --
-- \\__ \\ ' <| | | |__     ${headerLine2.get} --
-- |___/_|\\_\\_|_|____|    ${headerLine3.get} --
--                                                                            --
"""
  }

  override def setOptions(args : Array[String]) {
    var index = 0

    while (index < args.length) args(index) match {
      case unknown ⇒ sys.error(s"unkown Argument: $unknown")
    }
  }

  override protected def defaultValue(t : Type, d : UserType, f : Field) = t match {
    case t : GroundType ⇒ t.getSkillName() match {
      case "i8" | "i16" | "i32" | "i64" | "v64" ⇒ "0"
      case "f32" | "f64"                        ⇒ "0.0"
      case "bool"                               ⇒ "False"
      case _                                    ⇒ "null"
    }

    case t : ConstantLengthArrayType ⇒ s"(others => ${defaultValue(t.getBaseType, d, f)})"
    case t : VariableLengthArrayType ⇒ s"New_${mapType(t, d, f).stripSuffix(".Vector")}"
    case t : ListType                ⇒ s"New_${mapType(t, d, f).stripSuffix(".List")}"
    case t : SetType                 ⇒ s"New_${mapType(t, d, f).stripSuffix(".Set")}"
    case t : MapType                 ⇒ s"New_${mapType(t, d, f).stripSuffix(".Map")}"

    case _                           ⇒ "null"
  }

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  protected def escaped(pTarget : String) : String = {
    val target = pTarget.replaceAll("([A-Z])", "_$0").stripPrefix("_")

    target match {
      // keywords get a suffix "_2"
      case "abort" | "else" | "new" | "return" | "abs" | "elsif" | "not" | "reverse" | "abstract" | "end" | "null" |
        "accept" | "entry" | "select" | "access" | "exception" | "of" | "separate" | "aliased" | "exit" | "or" |
        "some" | "all" | "others" | "subtype" | "and" | "for" | "out" | "synchronized" | "array" | "function" |
        "overriding" | "at" | "tagged" | "generic" | "package" | "task" | "begin" | "goto" | "pragma" | "terminate" |
        "body" | "private" | "then" | "if" | "procedure" | "type" | "case" | "in" | "protected" | "constant" |
        "interface" | "until" | "is" | "raise" | "use" | "declare" | "range" | "delay" | "limited" | "record" |
        "when" | "delta" | "loop" | "rem" | "while" | "digits" | "renames" | "with" | "do" | "mod" | "requeue" |
        "xor" ⇒ return target+"_2"

      // the string is fine anyway
      case _ ⇒ return target
    }
  }
}
