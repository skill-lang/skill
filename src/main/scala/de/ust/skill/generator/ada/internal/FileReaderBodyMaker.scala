/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._
import de.ust.skill.ir.Declaration
import scala.collection.mutable.MutableList

trait FileReaderBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-file_reader.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Internal.File_Reader is

   State : access Skill_State;

   procedure Read (pState : access Skill_State; File_Name : String) is
      Input_File : ASS_IO.File_Type;
   begin
      State := pState;

      ASS_IO.Open (Input_File, ASS_IO.In_File, File_Name);
      Byte_Reader.Initialize (ASS_IO.Stream (Input_File));

      while (not ASS_IO.End_Of_File (Input_File)) loop
         Read_String_Block;
         Read_Type_Block;
         Update_Base_Pool_Start_Index;
      end loop;

      ASS_IO.Close (Input_File);
   end Read;

   procedure Read_String_Block is
      Count : Long := Byte_Reader.Read_v64;
      String_Lengths : array (1 .. Count) of Integer;
      Last_End : Integer := 0;
   begin
      --  read ends and calculate lengths
      for I in String_Lengths'Range loop
         declare
            String_End : Integer := Byte_Reader.Read_i32;
            String_Length : Integer := String_End - Last_End;
         begin
            String_Lengths (I) := String_End - Last_End;
            Last_End := String_End;
         end;
      end loop;

      --  read strings
      for I in String_Lengths'Range loop
         declare
            String_Length : Integer := String_Lengths (I);
            Next_String : String := Byte_Reader.Read_String (String_Length);
         begin
            State.Put_String (Next_String);
         end;
      end loop;
   end Read_String_Block;

   procedure Read_Type_Block is
      Count : Long := Byte_Reader.Read_v64;
      Last_End : Long := 0;
   begin
      for I in 1 .. Count loop
         Read_Type_Declaration (Last_End);
      end loop;

      Read_Field_Data;
   end Read_Type_Block;

   procedure Read_Type_Declaration (Last_End : in out Long) is
      Type_Name : String := State.Get_String (Byte_Reader.Read_v64);
      Instance_Count : Natural;
      Field_Count : Long;

      Skill_Unsupported_File_Format : exception;
   begin
      if not State.Has_Type (Type_Name) then
         declare
            Super_Name : Long := Byte_Reader.Read_v64;

            New_Type_Fields : Fields_Vector.Vector;
            New_Type_Storage_Pool : Storage_Pool_Vector.Vector;
            New_Type : Type_Information := new Type_Declaration'(
               Size => Type_Name'Length,
               Name => Type_Name,
               Super_Name => Super_Name,
               bpsi => 1,
               lbpsi => 1,
               Fields => New_Type_Fields,
               Storage_Pool => New_Type_Storage_Pool
            );
         begin
            State.Put_Type (New_Type);
         end;

         if 0 /= State.Get_Type (Type_Name).Super_Name then
            State.Get_Type (Type_Name).lbpsi := Positive (Byte_Reader.Read_v64);
         end if;

         Instance_Count := Natural (Byte_Reader.Read_v64);
         Skip_Restrictions;
      else
         if 0 /= State.Get_Type (Type_Name).Super_Name then
            State.Get_Type (Type_Name).lbpsi := Positive (Byte_Reader.Read_v64);
         end if;

         Instance_Count := Natural (Byte_Reader.Read_v64);
      end if;

      Field_Count := Byte_Reader.Read_v64;

      declare
         Known_Fields : Long := State.Known_Fields (Type_Name);
         Start_Index : Natural;
         End_Index : Natural;
      begin
         if 0 = Instance_Count then
            Start_Index := 1;
            End_Index := State.Storage_Pool_Size (Type_Name);
         else
            Start_Index := State.Storage_Pool_Size (Type_Name) + 1;
            End_Index := Start_Index + Instance_Count - 1;
            Create_Objects (Type_Name, Instance_Count);
         end if;

         for I in 1 .. Field_Count loop
            if Field_Count > Known_Fields or 0 = Instance_Count then
               Read_Field_Declaration (Type_Name);
            end if;

            declare
               Field_End : Long := Byte_Reader.Read_v64;
               Data_Length : Long := Field_End - Last_End;
               Field : Field_Information := State.Get_Field (Type_Name, I);
               Chunk : Data_Chunk (Type_Name'Length, Field.Name'Length) := (
                  Type_Size => Type_Name'Length,
                  Field_Size => Field.Name'Length,
                  Type_Name => Type_Name,
                  Field_Name => Field.Name,
                  Start_Index => Start_Index,
                  End_Index => End_Index,
                  Data_Length => Data_Length
               );
            begin
               Last_End := Field_End;
               Data_Chunks.Append (Chunk);
            end;
         end loop;
      end;
   end Read_Type_Declaration;

   procedure Read_Field_Declaration (Type_Name : String) is
   begin
      Skip_Restrictions;

      declare
         Field_Type : Short_Short_Integer := Byte_Reader.Read_i8;
      begin
         case Field_Type is
            --  unused
            when Short_Short_Integer'First .. -1 | 16 | 21 .. 31 =>
               null;

            --  constants i8, i16, i32, i64, v64
            when 0 .. 4 =>
               null;

            --  annotation, bool, i8, i16, i32, i64, v64, f32, f64, string
            when 5 .. 14 =>
               null;

            --  array T[i]
            when 15 =>
               declare
                  X : Long := Byte_Reader.Read_v64;
                  Y : Short_Short_Integer := Byte_Reader.Read_i8;
               begin
                  null;
               end;

            --  array T[], list, set
            when 17 .. 19 =>
               declare
                  X : Short_Short_Integer := Byte_Reader.Read_i8;
               begin
                  null;
               end;

            --  map
            when 20 =>
               declare
                  X : Long := Byte_Reader.Read_v64;
               begin
                  for I in 1 .. X loop
                     declare
                        Y : Short_Short_Integer := Byte_Reader.Read_i8;
                     begin
                        null;
                     end;
                  end loop;
               end;

            --  user type
            when 32 .. Short_Short_Integer'Last =>
               null;

            when others => null;
         end case;

         declare
            Field_Name : String := State.Get_String (Byte_Reader.Read_v64);

            New_Field : Field_Information := new Field_Declaration'(
               Size => Field_Name'Length,
               Name => Field_Name,
               F_Type => Field_Type
            );
         begin
            State.Put_Field (Type_Name, New_Field);
         end;
      end;
   end Read_Field_Declaration;

   procedure Read_Field_Data is
   begin
      Data_Chunks.Iterate (Data_Chunk_Vector_Iterator'Access);
      Data_Chunks.Clear;
   end Read_Field_Data;

   procedure Update_Base_Pool_Start_Index is
   begin
${
  var output = "";
  for (d ← IR) {
    output += s"""      if State.Has_Type ("${d.getSkillName}") then
         State.Get_Type ("${d.getSkillName}").bpsi := State.Storage_Pool_Size ("${d.getSkillName}") + 1;
      end if;\r\n"""
  }
  output.stripSuffix("\r\n")
}
   end Update_Base_Pool_Start_Index;

   procedure Create_Objects (Type_Name : String; Instance_Count : Natural) is
   begin
${
  def printSuperTypes(d: Declaration): String = {
    var output = "";
    val superTypes = getSuperTypes(d).toList.reverse
    superTypes.foreach({ t =>
      output += s"""\r\n\r\n               declare
                  Sub_Type : Type_Information := State.Get_Type ("${d.getSkillName}");
                  Super_Type : Type_Information := State.Get_Type ("${t}");
                  Position : Natural := (Sub_Type.lbpsi - Super_Type.lbpsi) + Super_Type.bpsi + I - 1;
               begin
                  Super_Type.Storage_Pool.Replace_Element (Position, Object);
               end;"""
    })
    output
  }

  def printDefaultValues(d: Declaration): String = {
    var output = ""
    var hasFields = false;
    val fields = d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored })
    output += fields.map({ f ⇒
      val value = defaultValue(f)
      if ("null" != value) {
          hasFields = true;
    	  s"""                  ${f.getSkillName} => ${value}"""
      }
    }).mkString("'(\r\n", ",\r\n", "\r\n               )")
    if (hasFields) output else ""
  }

  var output = "";
  for (d ← IR) {
    output += s"""      if "${d.getSkillName}" = Type_Name then
         for I in 1 .. Instance_Count loop
            declare
               Object : Skill_Type_Access := new ${d.getName}_Type${printDefaultValues(d)};
            begin
               State.Put_Object (Type_Name, Object);${printSuperTypes(d)}
            end;
         end loop;
      end if;\r\n"""
  }
  output.stripSuffix("\r\n")
}
   end Create_Objects;

   procedure Data_Chunk_Vector_Iterator (Iterator : Data_Chunk_Vector.Cursor) is
      Chunk : Data_Chunk := Data_Chunk_Vector.Element (Iterator);
      Skip_Bytes : Boolean := True;

      Skill_Parse_Error : exception;
   begin
${
  var output = "";
  for (d ← IR) {
    output += d.getFields.filter { f ⇒ !f.isAuto && !f.isConstant && !f.isIgnored }.map({ f ⇒
      s"""      if "${d.getSkillName}" = Chunk.Type_Name and then "${f.getSkillName}" = Chunk.Field_Name then
         for I in Chunk.Start_Index .. Chunk.End_Index loop
            declare
            ${mapFileParser(d, f)}
            end;
         end loop;
         Skip_Bytes := False;
      end if;\r\n"""}).mkString("")
  }
  output
}
      if True = Skip_Bytes then
         Byte_Reader.Skip_Bytes (Chunk.Data_Length);
      end if;
   end Data_Chunk_Vector_Iterator;

   procedure Skip_Restrictions is
      Zero : Long := Byte_Reader.Read_v64;
   begin
      null;
   end Skip_Restrictions;

end ${packagePrefix.capitalize}.Internal.File_Reader;
""")

    out.close()
  }
}
