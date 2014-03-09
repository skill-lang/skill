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

   type Queue_Item is
      record
         Type_Declaration : Type_Information;
         Field_Declaration : Field_Information;
      end record;

   package Write_Queue_Vector is new Ada.Containers.Indefinite_Vectors (Positive, Queue_Item);
   Write_Queue : Write_Queue_Vector.Vector;

   procedure Prepare_String_Pool;
   procedure Prepare_String_Pool_Types_Iterator (Iterator : Types_Hash_Map.Cursor);
   procedure Prepare_String_Pool_Fields_Iterator (Iterator : Fields_Vector.Cursor);
   procedure Prepare_String_Pool_Storage_Pool_Iterator (Iterator : Storage_Pool_Vector.Cursor);

   procedure Write_String_Pool;
   function Count_Instantiated_Types return Long;
   procedure Write_Type_Block;
   procedure Types_Hash_Map_Iterator (Iterator : Types_Hash_Map.Cursor);
   procedure Write_Type_Declaration (Type_Declaration : Type_Information);
   procedure Write_Field_Declaration (Type_Declaration : Type_Information; Field_Declaration : Field_Information);
   function Field_Data_Size (Type_Declaration : Type_Information; Field_Declaration : Field_Information) return Long;
   procedure Write_Queue_Vector_Iterator (Iterator : Write_Queue_Vector.Cursor);
   procedure Write_Field_Data
      (Stream : ASS_IO.Stream_Access; Type_Declaration : Type_Information; Field_Declaration : Field_Information);

   procedure Write_Annotation (Stream : ASS_IO.Stream_Access; Object : Skill_Type_Access);
   function Get_Object_Type (Object : Skill_Type_Access) return String;

end ${packagePrefix.capitalize}.Internal.File_Writer;
""")

    out.close()
  }
}
