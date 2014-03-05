/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait FileWriterSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-file_writer.ads""")

    out.write(s"""
with ${packagePrefix.capitalize}.Internal.Byte_Writer;

package ${packagePrefix.capitalize}.Internal.File_Writer is

   procedure Write (pState : access Skill_State; File_Name : String);

private

   State : access Skill_State;
   Output_Stream : ASS_IO.Stream_Access;

   type Queue_Item (Type_Size, Field_Size : Positive) is record
      Type_Name : String (1 .. Type_Size);
      Field_Name : String (1 .. Field_Size);
   end record;

   package Write_Queue_Vector is new Ada.Containers.Indefinite_Vectors (Positive, Queue_Item);
   Write_Queue : Write_Queue_Vector.Vector;

   procedure Write_String_Pool;
   procedure Write_Type_Block;
   procedure Types_Hash_Map_Iterator (Iterator : Types_Hash_Map.Cursor);
   procedure Write_Type_Declaration (Type_Declaration : Type_Information);
   procedure Write_Field_Declaration (Type_Declaration : Type_Information; Field_Declaration : Field_Information);
   function Field_Data_Size (Type_Name, Field_Name : String) return Long;
   procedure Write_Queue_Vector_Iterator (Iterator : Write_Queue_Vector.Cursor);
   procedure Write_Field_Data (Stream : ASS_IO.Stream_Access; Type_Name, Field_Name : String);

end ${packagePrefix.capitalize}.Internal.File_Writer;
""")

    out.close()
  }
}
