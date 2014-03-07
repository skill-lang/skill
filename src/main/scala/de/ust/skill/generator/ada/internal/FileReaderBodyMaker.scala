/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import scala.collection.JavaConversions._
import de.ust.skill.ir.Declaration
import de.ust.skill.generator.ada.GeneralOutputMaker

trait FileReaderBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-file_reader.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Internal.File_Reader is

   procedure Read (pState : access Skill_State; File_Name : String) is
      Input_File : ASS_IO.File_Type;
   begin
      State := pState;

      ASS_IO.Open (Input_File, ASS_IO.In_File, File_Name);
      Input_Stream := ASS_IO.Stream (Input_File);

      while (not ASS_IO.End_Of_File (Input_File)) loop
         Read_String_Block;
         Read_Type_Block;
         Update_Base_Pool_Start_Index;
      end loop;

      ASS_IO.Close (Input_File);
   end Read;

   procedure Read_String_Block is
      Count : Long := Byte_Reader.Read_v64 (Input_Stream);
      String_Lengths : array (1 .. Count) of Integer;
      Last_End : Integer := 0;
   begin
      --  read ends and calculate lengths
      for I in String_Lengths'Range loop
         declare
            String_End : Integer := Byte_Reader.Read_i32 (Input_Stream);
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
            Next_String : String := Byte_Reader.Read_String (Input_Stream, String_Length);
         begin
            State.Put_String (Next_String);
         end;
      end loop;
   end Read_String_Block;

   procedure Read_Type_Block is
      Count : Long := Byte_Reader.Read_v64 (Input_Stream);
      Last_End : Long := 0;
   begin
      for I in 1 .. Count loop
         Read_Type_Declaration (Last_End);
      end loop;

      Read_Field_Data;
   end Read_Type_Block;

   procedure Read_Type_Declaration (Last_End : in out Long) is
      Type_Name : String := State.Get_String (Byte_Reader.Read_v64 (Input_Stream));
      Instance_Count : Natural;
      Field_Count : Long;
   begin
      if not State.Has_Type (Type_Name) then
         declare
            Super_Name_Index : Long := Byte_Reader.Read_v64 (Input_Stream);
            Super_Name : SU.Unbounded_String := SU.To_Unbounded_String("");
         begin
            if Super_Name_Index > 0 then
               Super_Name := SU.To_Unbounded_String (State.Get_String (Super_Name_Index));
            end if;

            declare
               New_Type_Fields : Fields_Vector.Vector;
               New_Type_Storage_Pool : Storage_Pool_Vector.Vector;
               New_Type : Type_Information := new Type_Declaration'(
                  Type_Size => Type_Name'Length,
                  Super_Size => SU.To_String (Super_Name)'Length,
                  Name => Type_Name,
                  Super_Name => SU.To_String (Super_Name),
                  bpsi => 1,
                  lbpsi => 1,
                  Fields => New_Type_Fields,
                  Storage_Pool => New_Type_Storage_Pool
               );
            begin
               State.Put_Type (New_Type);
            end;
         end;

         if 0 /= State.Get_Type (Type_Name).Super_Name'Length then
            State.Get_Type (Type_Name).lbpsi := Positive (Byte_Reader.Read_v64 (Input_Stream));
         end if;

         Instance_Count := Natural (Byte_Reader.Read_v64 (Input_Stream));
         Skip_Restrictions;
      else
         if 0 /= State.Get_Type (Type_Name).Super_Name'Length then
            State.Get_Type (Type_Name).lbpsi := Positive (Byte_Reader.Read_v64 (Input_Stream));
         end if;

         Instance_Count := Natural (Byte_Reader.Read_v64 (Input_Stream));
      end if;

      Field_Count := Byte_Reader.Read_v64 (Input_Stream);

      declare
         Field_Index : Long;
         Known_Fields : Long := Long (State.Field_Size (Type_Name));
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

            Field_Index := I;
            if 0 = Instance_Count then
               Field_Index := Field_Index + Known_Fields;
            end if;

            declare
               Field_End : Long := Byte_Reader.Read_v64 (Input_Stream);
               Data_Length : Long := Field_End - Last_End;
               Field_Declaration : Field_Information := State.Get_Field (Type_Name, Field_Index);
               Item : Queue_Item := (
                  Type_Declaration => State.Get_Type (Type_Name),
                  Field_Declaration => Field_Declaration,
                  Start_Index => Start_Index,
                  End_Index => End_Index,
                  Data_Length => Data_Length
               );
            begin
               Last_End := Field_End;
               Read_Queue.Append (Item);
            end;
         end loop;
      end;
   end Read_Type_Declaration;

   procedure Read_Field_Declaration (Type_Name : String) is
   begin
      Skip_Restrictions;

      declare
         Constant_Value : Long := 0;
         Field_Name_Index : Long;
         Field_Type : Long := Byte_Reader.Read_v64 (Input_Stream);
      begin
         case Field_Type is
            --  const i8
            when 0 => Constant_Value := Long (Byte_Reader.Read_i8 (Input_Stream));
            --  const i16
            when 1 => Constant_Value := Long (Byte_Reader.Read_i16 (Input_Stream));
            --  const i32
            when 2 => Constant_Value := Long (Byte_Reader.Read_i32 (Input_Stream));
            --  const i64
            when 3 => Constant_Value := Byte_Reader.Read_i64 (Input_Stream);
            --  const v64
            when 4 => Constant_Value := Byte_Reader.Read_v64 (Input_Stream);

            --  array T[i]
            when 15 =>
               declare
                  X : Long := Byte_Reader.Read_v64 (Input_Stream);
                  Y : Short_Short_Integer := Byte_Reader.Read_i8 (Input_Stream);
               begin
                  null;
               end;

            --  array T[], list, set
            when 17 .. 19 =>
               declare
                  X : Short_Short_Integer := Byte_Reader.Read_i8 (Input_Stream);
               begin
                  null;
               end;

            --  map
            when 20 =>
               declare
                  X : Long := Byte_Reader.Read_v64 (Input_Stream);
               begin
                  for I in 1 .. X loop
                     declare
                        Y : Short_Short_Integer := Byte_Reader.Read_i8 (Input_Stream);
                     begin
                        null;
                     end;
                  end loop;
               end;

            when others =>
               null;
         end case;

         Field_Name_Index := Byte_Reader.Read_v64 (Input_Stream);

         declare
            Field_Name : String := State.Get_String (Field_Name_Index);

            New_Field : Field_Information := new Field_Declaration'(
               Size => Field_Name'Length,
               Name => Field_Name,
               F_Type => Field_Type,
               Constant_Value => Constant_Value
            );
         begin
            State.Put_Field (Type_Name, New_Field);
         end;
      end;
   end Read_Field_Declaration;

   procedure Read_Field_Data is
   begin
      Read_Queue.Iterate (Read_Queue_Vector_Iterator'Access);
      Read_Queue.Clear;
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

   procedure Read_Queue_Vector_Iterator (Iterator : Read_Queue_Vector.Cursor) is
      Item : Queue_Item := Read_Queue_Vector.Element (Iterator);
      Skip_Bytes : Boolean := True;

      Type_Declaration : Type_Information := Item.Type_Declaration;
      Field_Declaration : Field_Information := Item.Field_Declaration;

      Type_Name : String := Type_Declaration.Name;
      Field_Name : String := Field_Declaration.Name;

      Skill_Parse_Error : exception;
   begin
${
  var output = "";
  for (d ← IR) {
    output += d.getFields.filter({ f ⇒ !f.isAuto && !f.isIgnored }).map({ f ⇒
      s"""      if "${d.getSkillName}" = Type_Name and then "${f.getSkillName}" = Field_Name then
         for I in Item.Start_Index .. Item.End_Index loop
            declare
            ${mapFileReader(d, f)}
            end;
         end loop;
         Skip_Bytes := False;
      end if;\r\n"""}).mkString("")
  }
  output
}
      if True = Skip_Bytes then
         Byte_Reader.Skip_Bytes (Input_Stream, Item.Data_Length);
      end if;
   end Read_Queue_Vector_Iterator;

   procedure Skip_Restrictions is
      Zero : Long := Byte_Reader.Read_v64 (Input_Stream);
   begin
      null;
   end Skip_Restrictions;

end ${packagePrefix.capitalize}.Internal.File_Reader;
""")

    out.close()
  }
}
