/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import de.ust.skill.generator.ada.GeneralOutputMaker
import de.ust.skill.ir._
import scala.collection.JavaConversions._

trait FileWriterBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-internal-file_writer.adb""")

//    out.write(s"""
//package body ${packagePrefix.capitalize}.Api.Internal.File_Writer is
//
//   Modus       : Modus_Type;
//   String_Pool : String_Pool_Access;
//   Types       : Types_Hash_Map_Access;
//
//   Last_Types_End : Long := 0;
//
//   procedure Append (
//      State     : access Skill_State;
//      File_Name :        String
//   ) is
//      Output_File : ASS_IO.File_Type;
//   begin
//      Modus := Append;
//
//      String_Pool := State.String_Pool;
//      Types       := State.Types;
//
//      ASS_IO.Open (Output_File, ASS_IO.Append_File, File_Name);
//      ASS_IO.Create (Field_Data_File, ASS_IO.Out_File);
//
//      Run (Output_File);
//
//      ASS_IO.Delete (Field_Data_File);
//      ASS_IO.Flush (Output_File);
//      ASS_IO.Close (Output_File);
//   end Append;
//
//   procedure Write (
//      State     : access Skill_State;
//      File_Name :        String
//   ) is
//      Output_File : ASS_IO.File_Type;
//   begin
//      Modus := Write;
//
//      String_Pool := State.String_Pool;
//      Types       := State.Types;
//
//      --  reset string pool, spsi and written flags
//      String_Pool.Clear;
//      declare
//         use Types_Hash_Map;
//
//         procedure Iterate (Position : Cursor) is
//            Type_Declaration : Type_Information := Element (Position);
//         begin
//            for I in 1 .. Natural (Type_Declaration.Fields.Length) loop
//               Type_Declaration.Fields.Element (I).Written := False;
//            end loop;
//
//            Type_Declaration.spsi    := 0;
//            Type_Declaration.Written := False;
//         end Iterate;
//         pragma Inline (Iterate);
//      begin
//         Types.Iterate (Iterate'Access);
//      end;
//
//      ASS_IO.Create (Output_File, ASS_IO.Out_File, File_Name);
//      ASS_IO.Create (Field_Data_File, ASS_IO.Out_File);
//
//      Run (Output_File);
//
//      ASS_IO.Delete (Field_Data_File);
//      ASS_IO.Flush (Output_File);
//      ASS_IO.Close (Output_File);
//   end Write;
//
//   procedure Run (Output_File : ASS_IO.File_Type) is
//   begin
//      Field_Data_Stream := ASS_IO.Stream (Field_Data_File);
//      Output_Stream     := ASS_IO.Stream (Output_File);
//
//      Write_String_Pool;
//      Write_Type_Block;
//      Update_Storage_Pool_Start_Index;
//
//      Byte_Writer.Finalize_Buffer (Output_Stream);
//   end Run;
//
//   function Get_String_Index (Value : String_Access) return Positive is
//      Index : Natural := String_Pool.Reverse_Find_Index (Value);
//      Skill_Unknown_String_Index : exception;
//   begin
//      if 0 = Index then
//         raise Skill_Unknown_String_Index;
//      end if;
//      return Index;
//   end Get_String_Index;
//
//   procedure Put_String (
//      Value : String_Access;
//      Safe  : Boolean := False
//   ) is
//      Append : Boolean := True;
//   begin
//      if True = Safe then
//         declare
//            Index : Natural := String_Pool.Reverse_Find_Index (Value);
//         begin
//            if 0 < Index or else null = Value then
//               Append := False;
//            end if;
//         end;
//      end if;
//
//      if True = Append then
//         String_Pool.Append (Value);
//      end if;
//   end Put_String;
//
//   procedure Prepare_String_Pool is
//   begin
//      Types.Iterate (Prepare_String_Pool_Iterator'Access);
//   end Prepare_String_Pool;
//
//   procedure Prepare_String_Pool_Iterator (Iterator : Types_Hash_Map.Cursor) is
//      Type_Declaration : Type_Information := Types_Hash_Map.Element (Iterator);
//      Type_Name        : String_Access    := Type_Declaration.Name;
//   begin
//      Put_String (Type_Name, Safe => True);
//
//      declare
//         use Fields_Vector;
//
//         procedure Iterate (Iterator : Cursor) is
//            Field_Declaration : Field_Information := Element (Iterator);
//            Field_Name        : String_Access     := Field_Declaration.Name;
//         begin
//            Put_String (Field_Name, Safe => True);
//         end Iterate;
//         pragma Inline (Iterate);
//      begin
//         Type_Declaration.Fields.Iterate (Iterate'Access);
//      end;
//
//      declare
//         procedure Iterate (Iterator : Storage_Pool_Vector.Cursor) is
//            Skill_Object : Skill_Type_Access := Storage_Pool_Vector.Element (Iterator);
//         begin
//${
//      /**
//       * Gets all data from the string fields and puts them into the string pool.
//       */
//      var output = "";
//      for (d ← IR) {
//        var hasOutput = false;
//        output += s"""            if Equals (${name(d)}_Type_Skillname, Type_Name) then
//               declare
//                  Object : ${name(d)}_Type_Access := ${name(d)}_Type_Access (Skill_Object);
//               begin
//"""
//        d.getFields.filter({ f ⇒ "string" == f.getType.getSkillName }).foreach({ f ⇒
//          hasOutput = true;
//          output += s"                  Put_String (Object.${escapedLonely(f.getSkillName)}, Safe => True);\r\n"
//        })
//        d.getFields.foreach({ f ⇒
//          f.getType match {
//            case t : ConstantLengthArrayType ⇒
//              if ("string" == t.getBaseType.getSkillName) {
//                hasOutput = true;
//                output += s"""\r\n                  for I in Object.${escapedLonely(f.getSkillName)}'Range loop
//                     Put_String (Object.${escapedLonely(f.getSkillName)} (I), Safe => True);
//                  end loop;\r\n""";
//              }
//            case t : VariableLengthArrayType ⇒
//              if ("string" == t.getBaseType.getSkillName) {
//                hasOutput = true;
//                output += s"""\r\n                  declare
//                     use ${mapType(t, d, f).stripSuffix(".Vector")};
//
//                     Vector : ${mapType(t, d, f)} renames Object.${escapedLonely(f.getSkillName)};
//
//                     procedure Iterate (Position : Cursor) is
//                     begin
//                        Put_String (Element (Position), Safe => True);
//                     end Iterate;
//                     pragma Inline (Iterate);
//                  begin
//                     Vector.Iterate (Iterate'Access);
//                  end;\r\n"""
//              }
//            case t : ListType ⇒
//              if ("string" == t.getBaseType.getSkillName) {
//                hasOutput = true;
//                output += s"""\r\n                  declare
//                     use ${mapType(t, d, f).stripSuffix(".List")};
//
//                     List : ${mapType(t, d, f)} renames Object.${escapedLonely(f.getSkillName)};
//
//                     procedure Iterate (Position : Cursor) is
//                     begin
//                        Put_String (Element (Position), Safe => True);
//                     end Iterate;
//                     pragma Inline (Iterate);
//                  begin
//                     List.Iterate (Iterate'Access);
//                  end;\r\n"""
//              }
//            case t : SetType ⇒
//              if ("string" == t.getBaseType.getSkillName) {
//                hasOutput = true;
//                output += s"""\r\n                  declare
//                     use ${mapType(t, d, f).stripSuffix(".Set")};
//
//                     Set : ${mapType(t, d, f)} renames Object.${escapedLonely(f.getSkillName)};
//
//                     procedure Iterate (Position : Cursor) is
//                     begin
//                        Put_String (Element (Position), Safe => True);
//                     end Iterate;
//                     pragma Inline (Iterate);
//                  begin
//                     Set.Iterate (Iterate'Access);
//                  end;\r\n"""
//              }
//            case t : MapType ⇒
//              val types = t.getBaseTypes().reverse
//              if (types.map({ x ⇒ x.getSkillName }).contains("string")) {
//                hasOutput = true;
//                output += s"                  declare\r\n"
//                types.slice(0, types.length - 1).zipWithIndex.foreach({
//                  case (t, i) ⇒
//                    val x = {
//                      var output = ""
//                      if (0 == i) {
//                        if ("string" == types.get(i + 1).getSkillName) output += s"Put_String (Key (Position), Safe => True);"
//                        if ("string" == types.get(i).getSkillName) {
//                          if (!output.isEmpty) output += "\r\n                     "
//                          output += s"Put_String (Element (Position), Safe => True);"
//                        }
//                      } else {
//                        if ("string" == types.get(i + 1).getSkillName) output += s"Put_String (Key (Position), Safe => True);\r\n                           "
//                        output += s"Read_Map_${types.length - i} (Element (Position));"
//                      }
//                      if (output.isEmpty) "null;" else output
//                    }
//                    output += s"""                     procedure Read_Map_${types.length - (i + 1)} (Map : ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length - (i + 1)}.Map) is
//                        use ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length - (i + 1)};
//
//                        procedure Iterate (Position : Cursor) is
//                        begin
//                           ${x}
//                        end Iterate;
//                        pragma Inline (Iterate);
//                     begin
//                        Map.Iterate (Iterate'Access);
//                     end Read_Map_${types.length - (i + 1)};
//                     pragma Inline (Read_Map_${types.length - (i + 1)});\r\n\r\n"""
//                })
//                output = output.stripLineEnd
//                output += s"""                  begin
//                     Read_Map_1 (Object.${escapedLonely(f.getSkillName)});
//                  end;\r\n"""
//              }
//            case _ ⇒ null
//          }
//        })
//        if (!hasOutput) output += s"                  null;\r\n"
//        output += s"""               end;
//            end if;\r\n"""
//      }
//      if (output.isEmpty)
//        "                  null;"
//      else
//        output.stripSuffix("\r\n")
//    }
//         end Iterate;
//         pragma Inline (Iterate);
//      begin
//         Type_Declaration.Storage_Pool.Iterate (Iterate'Access);
//      end;
//   end Prepare_String_Pool_Iterator;
//
//   procedure Write_String_Pool is
//      Last_Size : Natural := Natural (String_Pool.Length);
//   begin
//      Prepare_String_Pool;
//
//      declare
//         Current_Size    : Natural := Natural (String_Pool.Length);
//         Start_Index     : Natural := Last_Size + 1;
//         End_Index       : Natural := Current_Size;
//         Size            : Natural := Current_Size - Last_Size;
//         Last_String_End : i32     := 0;
//      begin
//         Byte_Writer.Write_v64 (Output_Stream, Long (Size));
//
//         for I in Start_Index .. End_Index loop
//            declare
//               Value         : String_Access := String_Pool.Element (I);
//               String_Length : i32           := Value'Length + Last_String_End;
//            begin
//               Byte_Writer.Write_i32 (Output_Stream, String_Length);
//               Last_String_End := String_Length;
//            end;
//         end loop;
//
//         for I in Start_Index .. End_Index loop
//            Byte_Writer.Write_String (Output_Stream, String_Pool.Element (I));
//         end loop;
//      end;
//   end Write_String_Pool;
//
//   procedure Ensure_Type_Order is
//   begin${
//      var output = ""
//      /**
//       * Ensures the type order of all base types, if necessary.
//       */
//      for (d ← IR) {
//        if (null == d.getSuperType && 0 < getSubTypes(d).length) {
//          val types = getSubTypes(d).+=:(d)
//
//          output += s"""
//      declare
//         use Storage_Pool_Vector;
//
//         Type_Declaration : Type_Information := Types.Element (${name(d)}_Type_Skillname);
//         Size             : Natural          :=
//            Natural (Type_Declaration.Storage_Pool.Length) - Type_Declaration.spsi;
//
//         type Temp_Type is array (1 .. Size) of Skill_Type_Access;
//         type Temp_Type_Access is access Temp_Type;
//         Temp  : Temp_Type_Access := new Temp_Type;
//         Index : Positive         := 1;
//         First_Object : Boolean := False;
//"""
//          types.foreach({ t ⇒
//            output += s"""\r\n         ${name(t)}_Type_Declaration : Type_Information := Types.Element (${name(d)}_Type_Skillname);"""
//          })
//          output += "\r\n      begin\r\n"
//          types.foreach({ t ⇒
//            output += s"""         First_Object := True;
//         for I in Type_Declaration.spsi + 1 .. Natural (Type_Declaration.Storage_Pool.Length) loop
//            declare
//               Object : Skill_Type_Access := Type_Declaration.Storage_Pool.Element (I);
//            begin
//               if First_Object then
//                  ${name(t)}_Type_Declaration.lbpsi := Index - 1;
//                  First_Object := False;
//               end if;
//               if ${name(t)}_Type_Skillname = Get_Object_Type (Object) then
//                  Temp (Index) := Object;
//                  Index := Index + 1;
//               end if;
//            end;
//         end loop;\r\n"""
//          })
//          output += "\r\n"
//          types.foreach({ t ⇒
//            output += s"""         declare
//            Next_Type_Declaration : Type_Information := ${name(t)}_Type_Declaration;
//            Start_Index : Natural := Next_Type_Declaration.lbpsi + 1;
//            End_Index : Integer :=
//               Start_Index + Natural (Next_Type_Declaration.Storage_Pool.Length) - Next_Type_Declaration.spsi - 1;
//         begin
//            for I in Start_Index .. End_Index loop${if (d == t) s"\r\n               Temp (I).skill_id := Type_Declaration.spsi + I;" else ""}
//               declare
//                  Index : Natural := Next_Type_Declaration.spsi - Start_Index + I + 1;
//               begin
//                  Next_Type_Declaration.Storage_Pool.Replace_Element (Index, Temp (I));
//               end;
//            end loop;
//         end;\r\n"""
//          })
//          output += "      end;"
//          output
//        }
//      }
//      output.stripLineEnd.stripLineEnd
//    }
//      null;
//   end Ensure_Type_Order;
//
//   function Is_Type_Instantiated (Type_Declaration : Type_Information) return Boolean is
//      use Fields_Vector;
//
//      Known_Unwritten_Fields_Count : Natural := 0;
//      New_Instances_Count          : Natural :=
//         Natural (Type_Declaration.Storage_Pool.Length) - Type_Declaration.spsi;
//
//      procedure Iterate (Position : Cursor) is
//         Field_Declaration : Field_Information := Element (Position);
//      begin
//         if Field_Declaration.Known and then not Field_Declaration.Written then
//            Known_Unwritten_Fields_Count := Known_Unwritten_Fields_Count + 1;
//         end if;
//      end Iterate;
//      pragma Inline (Iterate);
//   begin
//      if Type_Declaration.Known then
//         Type_Declaration.Fields.Iterate (Iterate'Access);
//
//         --  write modus, new fields or new instances
//         return Write = Modus or else 0 < Known_Unwritten_Fields_Count or else 0 < New_Instances_Count;
//      else
//         return False;
//      end if;
//   end Is_Type_Instantiated;
//
//   function Count_Instantiated_Types return Long is
//      use Types_Hash_Map;
//
//      Return_Value : Long := 0;
//
//      procedure Iterate (Iterator : Cursor) is
//         Type_Declaration : Type_Information := Types_Hash_Map.Element (Iterator);
//      begin
//         if Is_Type_Instantiated (Type_Declaration) then
//            Return_Value := Return_Value + 1;
//         end if;
//      end Iterate;
//      pragma Inline (Iterate);
//   begin
//      Types.Iterate (Iterate'Access);
//      return Return_Value;
//   end Count_Instantiated_Types;
//
//   procedure Write_Type_Block is
//   begin
//      Ensure_Type_Order;
//
//      Byte_Writer.Write_v64 (Output_Stream, Count_Instantiated_Types);
//${
//      /**
//       * write types in type order (as guaranteed by IR)
//       */
//      (for (t ← IR)
//        yield s"""
//      Write_Type_Declaration (Types.Element (${name(t)}_Type_Skillname));"""
//      ).mkString
//    }
//
//      Last_Types_End := 0;
//
//      Copy_Field_Data;
//   end Write_Type_Block;
//
//   function Count_Known_Fields
//     (Type_Declaration : Type_Information) return Long
//   is
//      use Fields_Vector;
//
//      Return_Value : Long := 0;
//
//      procedure Iterate (Iterator : Cursor) is
//         Field_Declaration : Field_Information := Fields_Vector.Element (Iterator);
//      begin
//         if Field_Declaration.Known then
//            Return_Value := Return_Value + 1;
//         end if;
//      end Iterate;
//      pragma Inline (Iterate);
//   begin
//      Type_Declaration.Fields.Iterate (Iterate'Access);
//      return Return_Value;
//   end Count_Known_Fields;
//
//   function Count_Known_Unwritten_Fields
//     (Type_Declaration : Type_Information) return Long
//   is
//      use Fields_Vector;
//
//      Return_Value : Long := 0;
//
//      procedure Iterate (Iterator : Cursor) is
//         Field_Declaration : Field_Information := Fields_Vector.Element (Iterator);
//      begin
//         if Field_Declaration.Known and then not Field_Declaration.Written then
//            Return_Value := Return_Value + 1;
//         end if;
//      end Iterate;
//      pragma Inline (Iterate);
//   begin
//      Type_Declaration.Fields.Iterate (Iterate'Access);
//      return Return_Value;
//   end Count_Known_Unwritten_Fields;
//
//   procedure Write_Type_Declaration (Type_Declaration : Type_Information) is
//      Type_Name       : String_Access    := Type_Declaration.Name;
//      Super_Type      : Type_Information := Type_Declaration.Super_Type;
//      Field_Count     : Natural := Natural (Type_Declaration.Fields.Length);
//      Instances_Count : Natural :=
//        Natural (Type_Declaration.Storage_Pool.Length) - Type_Declaration.spsi;
//   begin
//      -- write instantiated types only
//      if not Is_Type_Instantiated (Type_Declaration) then
//         return;
//      end if;
//
//      Byte_Writer.Write_v64
//        (Output_Stream,
//         Long (Get_String_Index (Type_Name)));
//      Byte_Writer.Write_v64 (Output_Stream, Long (Instances_Count));
//
//      if not Type_Declaration.Written then
//         Byte_Writer.Write_v64 (Output_Stream, 0);  --  restrictions
//
//         if null /= Super_Type then
//            Byte_Writer.Write_v64 (Output_Stream, Long (Super_Type.id));
//         else
//            Byte_Writer.Write_i8 (Output_Stream, 0);
//         end if;
//      end if;
//
//      if null /= Super_Type then
//         Byte_Writer.Write_v64 (Output_Stream, Long (Type_Declaration.lbpsi));
//      end if;
//
//      if Write = Modus then
//         Byte_Writer.Write_v64
//           (Output_Stream,
//            Count_Known_Fields (Type_Declaration));
//
//         --  write known fields
//         for I in 1 .. Field_Count loop
//            declare
//               Field_Declaration : Field_Information :=
//                 Type_Declaration.Fields.Element (Positive (I));
//            begin
//               if Field_Declaration.Known then
//                  Write_Field_Declaration
//                    (Type_Declaration,
//                     Field_Declaration);
//               end if;
//            end;
//         end loop;
//      end if;
//
//      if Append = Modus then
//         if 0 = Instances_Count then
//            Byte_Writer.Write_v64
//              (Output_Stream,
//               Count_Known_Unwritten_Fields (Type_Declaration));
//         else
//            Byte_Writer.Write_v64
//              (Output_Stream,
//               Count_Known_Fields (Type_Declaration));
//
//            --  write known written fields
//            for I in 1 .. Field_Count loop
//               declare
//                  Field_Declaration : Field_Information :=
//                    Type_Declaration.Fields.Element (Positive (I));
//               begin
//                  if Field_Declaration.Known and Field_Declaration.Written then
//                     Write_Field_Declaration
//                       (Type_Declaration,
//                        Field_Declaration);
//                  end if;
//               end;
//            end loop;
//         end if;
//
//         --  write known unwritten fields
//         for I in 1 .. Field_Count loop
//            declare
//               Field_Declaration : Field_Information :=
//                 Type_Declaration.Fields.Element (Positive (I));
//            begin
//               if Field_Declaration.Known and
//                 not Field_Declaration.Written
//               then
//                  Write_Field_Declaration
//                    (Type_Declaration,
//                     Field_Declaration);
//               end if;
//            end;
//         end loop;
//      end if;
//
//      Type_Declaration.Written := True;
//   end Write_Type_Declaration;
//
//   procedure Write_Field_Declaration
//     (Type_Declaration  : Type_Information;
//      Field_Declaration : Field_Information)
//   is
//      Type_Name  : String_Access := Type_Declaration.Name;
//      Field_Name : String_Access := Field_Declaration.Name;
//      Field_Type : Long          := Field_Declaration.F_Type;
//      Size : Long := Field_Data_Size (Type_Declaration, Field_Declaration);
//
//      --  see comment in file date.ads
//      Base_Types : Base_Types_Vector.Vector := Field_Declaration.Base_Types;
//   begin
//      Byte_Writer.Write_v64 (Output_Stream, Field_Declaration.id);
//
//      if not Field_Declaration.Written then
//         Byte_Writer.Write_v64
//           (Output_Stream,
//            Long (Get_String_Index (Field_Name)));
//         Byte_Writer.Write_v64 (Output_Stream, Long (Field_Type));
//
//         case Field_Type is
//            --  const i8, i16, i32, i64, v64
//            when 0 =>
//               Byte_Writer.Write_i8
//                 (Output_Stream,
//                  i8 (Field_Declaration.Constant_Value));
//            when 1 =>
//               Byte_Writer.Write_i16
//                 (Output_Stream,
//                  Short (Field_Declaration.Constant_Value));
//            when 2 =>
//               Byte_Writer.Write_i32
//                 (Output_Stream,
//                  i32 (Field_Declaration.Constant_Value));
//            when 3 =>
//               Byte_Writer.Write_i64
//                 (Output_Stream,
//                  Field_Declaration.Constant_Value);
//            when 4 =>
//               Byte_Writer.Write_v64
//                 (Output_Stream,
//                  Field_Declaration.Constant_Value);
//
//            --  array T[i]
//            when 15 =>
//               Byte_Writer.Write_v64
//                 (Output_Stream,
//                  Long (Field_Declaration.Constant_Array_Length));
//               Byte_Writer.Write_v64
//                 (Output_Stream,
//                  Field_Declaration.Base_Types.First_Element);
//
//            --  array T[], list, set
//            when 17 .. 19 =>
//               Byte_Writer.Write_v64
//                 (Output_Stream,
//                  Field_Declaration.Base_Types.First_Element);
//
//            --  map
//            when 20 =>
//               declare
//                  use Base_Types_Vector;
//
//                  Base_Types_Length : Positive := Positive (Base_Types.Length);
//
//                  procedure Iterate (Position : Cursor) is
//                     Base_Type_Id : Long     := Element (Position);
//                     Index        : Positive := Positive (To_Index (Position));
//                  begin
//                     Byte_Writer.Write_v64 (Output_Stream, Base_Type_Id);
//
//                     if Base_Types_Length > Index + 1 then
//                        Byte_Writer.Write_v64 (Output_Stream, 20);
//                     end if;
//                  end Iterate;
//                  pragma Inline (Iterate);
//               begin
//                  Base_Types.Iterate (Iterate'Access);
//               end;
//
//            when others =>
//               null;
//         end case;
//
//         Byte_Writer.Write_v64 (Output_Stream, 0);  --  restrictions
//      end if;
//
//      Last_Types_End := Last_Types_End + Size;
//      Byte_Writer.Write_v64 (Output_Stream, Last_Types_End);
//
//      Field_Declaration.Written := True;
//   end Write_Field_Declaration;
//
//   function Field_Data_Size
//     (Type_Declaration  : Type_Information;
//      Field_Declaration : Field_Information) return Long
//   is
//      Current_Index : Long := Long (ASS_IO.Index (Field_Data_File));
//      Return_Value  : Long;
//   begin
//      Byte_Writer.Finalize_Buffer (Output_Stream);
//      Write_Field_Data
//        (Field_Data_Stream,
//         Type_Declaration,
//         Field_Declaration);
//      Byte_Writer.Finalize_Buffer (Field_Data_Stream);
//      Return_Value := Long (ASS_IO.Index (Field_Data_File)) - Current_Index;
//      return Return_Value;
//   end Field_Data_Size;
//
//   procedure Write_Field_Data
//     (Stream            : ASS_IO.Stream_Access;
//      Type_Declaration  : Type_Information;
//      Field_Declaration : Field_Information)
//   is
//      Type_Name    : String_Access             := Type_Declaration.Name;
//      Field_Name   : String_Access             := Field_Declaration.Name;
//      Start_Index  : Positive                  := 1;
//      Storage_Pool : Storage_Pool_Array_Access :=
//        new Storage_Pool_Array
//        (1 .. Natural (Type_Declaration.Storage_Pool.Length));
//
//      procedure Iterate (Position : Storage_Pool_Vector.Cursor) is
//         Index : Positive := Storage_Pool_Vector.To_Index (Position);
//      begin
//         Storage_Pool (Index) := Storage_Pool_Vector.Element (Position);
//      end Iterate;
//      pragma Inline (Iterate);
//   begin
//      Type_Declaration.Storage_Pool.Iterate (Iterate'Access);
//
//      if Field_Declaration.Written then
//         Start_Index := Type_Declaration.spsi + 1;
//      end if;
//${
//      /**
//       * Writes the field data of all fields.
//       */
//      (for (
//        t ← IR;
//        f ← t.getFields if (!f.isAuto && !f.isConstant && !f.isIgnored)
//      ) yield s"""
//      if Equals (${name(t)}_Type_Skillname, Type_Name)
//        and then Equals (${name(t)}_Type_${name(f)}_Field_Skillname, Field_Name)
//      then
//         for I in Start_Index .. Natural (Type_Declaration.Storage_Pool.Length)
//         loop
//            declare
//               Object : ${name(t)}_Type_Access :=
//                 ${name(t)}_Type_Access (Storage_Pool (I));
//            ${mapFileWriter(t, f)}
//            end;
//         end loop;
//      end if;
//""").mkString
//    }
//      Free (Storage_Pool);
//   end Write_Field_Data;
//
//   procedure Copy_Field_Data is
//   begin
//      ASS_IO.Reset (Field_Data_File, ASS_IO.In_File);
//      Byte_Reader.Reset_Buffer;
//      for I in
//        Long (ASS_IO.Index (Field_Data_File)) ..
//            Long (ASS_IO.Size (Field_Data_File))
//      loop
//         Byte_Writer.Write_i8
//           (Output_Stream,
//            Byte_Reader.Read_i8 (Field_Data_Stream));
//      end loop;
//   end Copy_Field_Data;
//
//   procedure Write_Annotation
//     (Stream : ASS_IO.Stream_Access;
//      Object : Skill_Type_Access)
//   is
//      Type_Name : String_Access := Get_Object_Type (Object);
//
//      function Get_Base_Type
//        (Type_Declaration : Type_Information) return String_Access
//      is
//         T : Type_Information := Type_Declaration;
//      begin
//         while (null /= T) loop
//            T := T.Super_Type;
//         end loop;
//
//         return Type_Declaration.Name;
//      end Get_Base_Type;
//   begin
//      if null = Type_Name then
//         Byte_Writer.Write_v64 (Stream, 0);
//         Byte_Writer.Write_v64 (Stream, 0);
//      else
//         Byte_Writer.Write_v64
//           (Stream,
//            Long
//              (Get_String_Index (Get_Base_Type (Types.Element (Type_Name)))));
//         Byte_Writer.Write_v64 (Stream, Long (Object.skill_id));
//      end if;
//   end Write_Annotation;
//
//   procedure Write_String (
//      Stream : ASS_IO.Stream_Access;
//      Value  : String_Access
//   ) is
//   begin
//      Byte_Writer.Write_v64 (Stream, Long (Get_String_Index (Value)));
//   end Write_String;
//
//${
//      var output = "";
//      /**
//       * Writes the skill id of a given object.
//       */
//      for (d ← IR) {
//        output += s"""   procedure Write_${name(d)}_Type (
//      Stream : ASS_IO.Stream_Access;
//      Object : ${name(d)}_Type_Access
//   ) is
//   begin
//      if null = Object then
//         Byte_Writer.Write_v64 (Stream, 0);
//      else
//         Byte_Writer.Write_v64 (Stream, Long (Object.skill_id));
//      end if;
//   end Write_${name(d)}_Type;\r\n\r\n"""
//      }
//      output.stripSuffix("\r\n")
//    }
//   function Get_Object_Type (Object : Skill_Type_Access) return String_Access is
//      use Ada.Tags;
//   begin
//      if null = Object then
//         return null;
//      end if;
//${
//      /**
//       * Gets the type of a given object.
//       */
//      (for (t ← IR) yield s"""
//      if ${name(t)}_Type'Tag = Object'Tag then
//         return ${name(t)}_Type_Skillname;
//      end if;
//""").mkString
//    }
//      return null;
//   end Get_Object_Type;
//
//   procedure Update_Storage_Pool_Start_Index is
//   begin${
//      /**
//       * Corrects the SPSI (storage pool start index) of all types.
//       */
//      val r = (for (t ← IR) yield s"""
//      if Types.Contains (${name(t)}_Type_Skillname) then
//         Types.Element (${name(t)}_Type_Skillname).spsi := Natural (Types.Element (${name(t)}_Type_Skillname).Storage_Pool.Length);
//      end if;
//""").mkString
//
//      if (r.isEmpty) "\n      null;" else r
//    }
//   end Update_Storage_Pool_Start_Index;
//
//end ${packagePrefix.capitalize}.Api.Internal.File_Writer;
//""")

    out.close()
  }

//  /**
//   * Generates the write functions into the procedure Write_Field_Data in package File_Writer.
//   */
//  protected def mapFileWriter(d : UserType, f : Field) : String = {
//    /**
//     * The basis type of the field that will be written.
//     */
//    def inner(t : Type, _d : UserType, _f : Field, value : String) : String = {
//      t match {
//        case t : GroundType ⇒ t.getName.lower match {
//          case "annotation" ⇒
//            s"Write_Annotation (Stream, ${value})"
//          case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
//            s"Byte_Writer.Write_${mapType(t, _d, _f)} (Stream, ${value})"
//          case "string" ⇒
//            s"Write_String (Stream, ${value})"
//        }
//        case t : Declaration ⇒
//          s"""Write_${name(t)}_Type (Stream, ${value})"""
//      }
//    }
//
//    f.getType match {
//      case t : GroundType ⇒ t.getName.lower match {
//        case "annotation" ⇒
//          s"""begin
//               ${inner(f.getType, d, f, s"Object.${escapedLonely(f.getSkillName)}")};"""
//
//        case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
//          s"""begin
//               ${inner(f.getType, d, f, s"Object.${escapedLonely(f.getSkillName)}")};"""
//
//        case "string" ⇒
//          s"""begin
//               ${inner(f.getType, d, f, s"Object.${escapedLonely(f.getSkillName)}")};"""
//      }
//
//      case t : ConstantLengthArrayType ⇒
//        s"""begin
//               for I in Object.${escapedLonely(f.getSkillName)}'Range loop
//                  ${inner(t.getBaseType, d, f, s"Object.${escapedLonely(f.getSkillName)} (I)")};
//               end loop;"""
//
//      case t : VariableLengthArrayType ⇒
//        s"""   use ${mapType(f.getType, d, f).stripSuffix(".Vector")};
//
//               Vector : ${mapType(f.getType, d, f)} renames Object.${escapedLonely(f.getSkillName)};
//
//               procedure Iterate (Position : Cursor) is
//               begin
//                  ${inner(t.getBaseType, d, f, s"Element (Position)")};
//               end Iterate;
//               pragma Inline (Iterate);
//            begin
//               Byte_Writer.Write_v64 (Stream, Long (Vector.Length));
//               Vector.Iterate (Iterate'Access);"""
//
//      case t : ListType ⇒
//        s"""   use ${mapType(f.getType, d, f).stripSuffix(".List")};
//
//               List : ${mapType(f.getType, d, f)} renames Object.${escapedLonely(f.getSkillName)};
//
//               procedure Iterate (Position : Cursor) is
//               begin
//                  ${inner(t.getBaseType, d, f, s"Element (Position)")};
//               end Iterate;
//               pragma Inline (Iterate);
//            begin
//               Byte_Writer.Write_v64 (Stream, Long (List.Length));
//               List.Iterate (Iterate'Access);"""
//
//      case t : SetType ⇒
//        s"""   use ${mapType(f.getType, d, f).stripSuffix(".Set")};
//
//               Set : ${mapType(f.getType, d, f)} renames Object.${escapedLonely(f.getSkillName)};
//
//               procedure Iterate (Position : Cursor) is
//               begin
//                  ${inner(t.getBaseType, d, f, s"Element (Position)")};
//               end Iterate;
//               pragma Inline (Iterate);
//            begin
//               Byte_Writer.Write_v64 (Stream, Long (Set.Length));
//               Set.Iterate (Iterate'Access);"""
//
//      case t : MapType ⇒
//        s"""${
//          var output = ""
//          val types = t.getBaseTypes().reverse
//          types.slice(0, types.length - 1).zipWithIndex.foreach({
//            case (t, i) ⇒
//              val x = {
//                if (0 == i)
//                  s"""${inner(types.get(i + 1), d, f, "Key (Position)")};
//                     ${inner(types.get(i), d, f, "Element (Position)")};"""
//                else
//                  s"""${inner(types.get(i + 1), d, f, "Key (Position)")};
//                     Write_Map_${types.length - i} (Element (Position));"""
//              }
//              output += s"""   ${if (types.length - (i + 1) == types.length - 1) "" else "               "}procedure Write_Map_${types.length - (i + 1)} (Map : ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length - (i + 1)}.Map) is
//                  use ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length - (i + 1)};
//
//                  procedure Iterate (Position : Cursor) is
//                  begin
//                     ${x}
//                  end Iterate;
//                  pragma Inline (Iterate);
//               begin
//                  Byte_Writer.Write_v64 (Stream, Long (Map.Length));
//                  Map.Iterate (Iterate'Access);
//               end Write_Map_${types.length - (i + 1)};
//               pragma Inline (Write_Map_${types.length - (i + 1)});\r\n\r\n"""
//          })
//          output.stripLineEnd.stripLineEnd
//        }
//            begin
//               Write_Map_1 (Object.${escapedLonely(f.getSkillName)});"""
//
//      case t : Declaration ⇒
//        s"""begin
//               ${inner(f.getType, d, f, s"Object.${escapedLonely(f.getSkillName)}")};"""
//    }
//  }
}
