/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.mutable.ListBuffer

trait FileParserBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-file_parser.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Internal.File_Parser is

   package Byte_Reader renames ${packagePrefix.capitalize}.Internal.Byte_Reader;

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
   begin
      for I in 1 .. Count loop
         Read_Type_Declaration;
      end loop;

      Read_Field_Data;
   end Read_Type_Block;

   procedure Read_Type_Declaration is
      Type_Name : String := State.Get_String (Byte_Reader.Read_v64);
      Instance_Count : Long;
      Field_Count : Long;

      Skill_Unsupported_File_Format : exception;
   begin
      if not State.Has_Type (Type_Name) then
         declare
            Type_Super : Long := Byte_Reader.Read_v64;

            New_Type_Fields : Fields_Vector.Vector;
            New_Type_Storage_Pool : Storage_Pool_Vector.Vector;
            New_Type : Type_Information := new Type_Declaration'
               (Type_Name'Length, Type_Name, Type_Super, New_Type_Fields, New_Type_Storage_Pool);
         begin
            State.Put_Type (New_Type);
         end;

         Instance_Count := Byte_Reader.Read_v64;
         Skip_Restrictions;
      else
         Instance_Count := Byte_Reader.Read_v64;
      end if;

      Field_Count := Byte_Reader.Read_v64;

      if 0 = Instance_Count then
         raise Skill_Unsupported_File_Format;
      else
         declare
            Known_Fields : Long := State.Known_Fields (Type_Name);
            Last_Offset : Long := 0;
            Start_Index : Natural := State.Storage_Size (Type_Name) + 1;
            End_Index : Natural := Start_Index + Natural (Instance_Count) - 1;
         begin
            Create_Instances (Type_Name, Instance_Count);

            for I in 1 .. Field_Count loop
               if Field_Count > Known_Fields then
                  Read_Field_Declaration (Type_Name);
               end if;

               declare
                  Offset : Long := Byte_Reader.Read_v64;
                  Data_Length : Long := Offset - Last_Offset;
                  Field : Field_Information := State.Get_Field (Type_Name, I);
                  Chunk : Data_Chunk (Type_Name'Length, Field.Name'Length) :=
                     (Type_Name'Length, Field.Name'Length, Type_Name, Start_Index, End_Index, Field.Name, Field.F_Type, Data_Length);
               begin
                  Last_Offset := Offset;
                  Data_Chunks.Append (Chunk);
               end;
            end loop;
         end;
      end if;
   end Read_Type_Declaration;

   procedure Read_Field_Declaration (Type_Name : String) is
   begin
      Skip_Restrictions;

      declare
         Field_Type : Short_Short_Integer := Byte_Reader.Read_i8;
         Field_Name : String := State.Get_String (Byte_Reader.Read_v64);

         New_Field : Field_Information := new Field_Declaration'(Field_Name'Length, Field_Name, Field_Type);
      begin
         State.Put_Field (Type_Name, New_Field);
      end;
   end Read_Field_Declaration;

   procedure Read_Field_Data is
   begin
      Data_Chunks.Iterate (Data_Chunk_Vector_Iterator'Access);
      Data_Chunks.Clear;
   end Read_Field_Data;

   procedure Create_Instances (Type_Name : String; Instance_Count : Long) is
   begin
${
    var output = "";
	for (declaration ← IR) {
	  val name = declaration.getName
	  val skillName = declaration.getSkillName

	  var fields = ListBuffer[String]()
	  val iterator = declaration.getAllFields.iterator;
	  while (iterator.hasNext) {
	    val field = iterator.next
	    fields += s"""                  ${field.getSkillName} => ${defaultValue(field)}"""
	  }

	  output += s"""      if "${skillName}" = Type_Name then
         for I in 1 .. Instance_Count loop
            declare
               Object : ${name}_Instance := (
${fields.mkString(s""",\r\n""")}
               );
            begin
               State.Put_Instance (Type_Name, Object);
            end;
         end loop;
      end if;\r\n"""
	}
	output.stripSuffix(s"""\r\n""")
   }
   end Create_Instances;

   procedure Data_Chunk_Vector_Iterator (Iterator : Data_Chunk_Vector.Cursor) is
      Chunk : Data_Chunk := Data_Chunk_Vector.Element (Iterator);
      Skip_Bytes : Boolean := True;
   begin
${
    var output = "";
	for (declaration ← IR) {
	  val name = declaration.getName
	  val skillName = declaration.getSkillName

	  var fields = ListBuffer[String]()
	  val iterator = declaration.getAllFields.iterator;
	  while (iterator.hasNext) {
	    val field = iterator.next
	    output += s"""      if "${skillName}" = Chunk.Type_Name and then "${field.getSkillName}" = Chunk.Field_Name then
         for I in Chunk.Start_Index .. Chunk.End_Index loop
            declare
               Object : ${name}_Instance := ${name}_Instance (State.Get_Instance (Chunk.Type_Name, I));
            begin
               Object.${field.getSkillName} := ${mapTypeForFieldParser(field.getType)};
               State.Replace_Instance (Chunk.Type_Name, I, Object);
            end;
         end loop;
         Skip_Bytes := False;
      end if;
"""
	  }
	}
	output
   }
      if True = Skip_Bytes then
         Byte_Reader.Skip_Bytes (Chunk.Data_Length);
      end if;
   end Data_Chunk_Vector_Iterator;

   procedure Skip_Restrictions is
      X : Long := Byte_Reader.Read_v64;
   begin
      null;
   end Skip_Restrictions;

end ${packagePrefix.capitalize}.Internal.File_Parser;
""")

    out.close()
  }
}
