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

trait FileWriterBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-file_writer.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Internal.File_Writer is

   Last_Types_End : Long := 0;

   procedure Write (pState : access Skill_State; File_Name : String) is
      Output_File : ASS_IO.File_Type;
   begin
      State := pState;

      ASS_IO.Create (Output_File, ASS_IO.Out_File, File_Name);
      Output_Stream := ASS_IO.Stream (Output_File);

      Write_String_Pool;
      Write_Type_Block;

      ASS_IO.Close (Output_File);
   end Write;

   procedure Write_String_Pool is
      Size : Natural := State.String_Pool_Size;
      Last_String_End : Natural := 0;
   begin
      Byte_Writer.Write_v64 (Output_Stream, Long (Size));

      for I in 1 .. Size loop
         declare
            String_Length : Positive := State.Get_String (I)'Length + Last_String_End;
         begin
            Byte_Writer.Write_i32 (Output_Stream, String_Length);
            Last_String_End := String_Length;
         end;
      end loop;

      for I in 1 .. Size loop
         Byte_Writer.Write_String (Output_Stream, State.Get_String (I));
      end loop;
   end Write_String_Pool;

   procedure Write_Type_Block is
      Count : Long := Long (State.Type_Size);
   begin
      Byte_Writer.Write_v64 (Output_Stream, Count);

      State.Get_Types.Iterate (Types_Hash_Map_Iterator'Access);
      Last_Types_End := 0;

      Write_Queue.Iterate (Write_Queue_Vector_Iterator'Access);
      Write_Queue.Clear;
   end Write_Type_Block;

   procedure Types_Hash_Map_Iterator (Iterator : Types_Hash_Map.Cursor) is
      Type_Declaration : Type_Information := Types_Hash_Map.Element (Iterator);
   begin
      Write_Type_Declaration (Type_Declaration);
   end;

   procedure Write_Type_Declaration (Type_Declaration : Type_Information) is
      Type_Name : String := Type_Declaration.Name;
      Field_Size : Natural := State.Field_Size (Type_Name);
   begin
      Byte_Writer.Write_v64 (Output_Stream, Long (State.Get_String_Index (Type_Name)));
      Byte_Writer.Write_v64 (Output_Stream, 0);  --  super TODO
      Byte_Writer.Write_v64 (Output_Stream, Long (State.Storage_Pool_Size (Type_Name)));
      Byte_Writer.Write_v64 (Output_Stream, 0);  --  restrictions
      Byte_Writer.Write_v64 (Output_Stream, Long (Field_Size));

      for I in 1 .. Field_Size loop
         Write_Field_Declaration (Type_Declaration, State.Get_Field (Type_Name, I));
      end loop;
   end Write_Type_Declaration;

   procedure Write_Field_Declaration (Type_Declaration : Type_Information; Field_Declaration : Field_Information) is
      Type_Name : String := Type_Declaration.Name;
      Field_Name : String := Field_Declaration.Name;
      Size : Long := Field_Data_Size (Type_Name, Field_Name);
   begin
      Byte_Writer.Write_v64 (Output_Stream, 0);  --  restrictions
      Byte_Writer.Write_v64 (Output_Stream, Field_Declaration.F_Type);  --  type TODO
      Byte_Writer.Write_v64 (Output_Stream, Long (State.Get_String_Index (Field_Name)));

      Last_Types_End := Last_Types_End + Size;
      Byte_Writer.Write_v64 (Output_Stream, Last_Types_End);

      declare
         Item : Queue_Item (Type_Name'Length, Field_Name'Length) := (
            Type_Size => Type_Name'Length,
            Field_Size => Field_Name'Length,
            Type_Name => Type_Name,
            Field_Name => Field_Name
         );
      begin
         Write_Queue.Append (Item);
      end;
   end Write_Field_Declaration;

   function Field_Data_Size (Type_Name, Field_Name : String) return Long is
      Fake_Output_File : ASS_IO.File_Type;
      rval : Long;
   begin
      ASS_IO.Create (Fake_Output_File, ASS_IO.Out_File);
      Write_Field_Data (ASS_IO.Stream (Fake_Output_File), Type_Name, Field_Name);
      rval := Long (ASS_IO.Size (Fake_Output_File));
      ASS_IO.Delete (Fake_Output_File);
      return rval;
   end Field_Data_Size;

   procedure Write_Queue_Vector_Iterator (Iterator : Write_Queue_Vector.Cursor) is
      Item : Queue_Item := Write_Queue_Vector.Element (Iterator);
   begin
      Write_Field_Data (Output_Stream, Item.Type_Name, Item.Field_Name);
   end Write_Queue_Vector_Iterator;

   procedure Write_Field_Data (Stream : ASS_IO.Stream_Access; Type_Name, Field_Name : String) is
   begin
      if "date" = Type_Name and then "date" = Field_Name then
         for I in 1 .. State.Storage_Pool_Size (Type_Name) loop
            declare
               Object : Date_Type_Access := Date_Type_Access (State.Get_Object (Type_Name, I));
            begin
               Byte_Writer.Write_v64 (Stream, Object.date);
            end;
         end loop;
      end if;
   end Write_Field_Data;

end ${packagePrefix.capitalize}.Internal.File_Writer;
""")

    out.close()
  }
}
