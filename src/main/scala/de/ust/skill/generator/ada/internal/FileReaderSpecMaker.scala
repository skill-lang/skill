/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait FileReaderSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-file_reader.ads""")

    out.write(s"""
with ${packagePrefix.capitalize}.Internal.Byte_Reader;

package ${packagePrefix.capitalize}.Internal.File_Reader is

   procedure Read (pState : access Skill_State; File_Name : String);

private

   State : access Skill_State;
   Input_Stream : ASS_IO.Stream_Access;

   type Queue_Item (Type_Size, Field_Size : Positive) is record
      Type_Name : String (1 .. Type_Size);
      Field_Name : String (1 .. Field_Size);
      Start_Index : Natural;
      End_Index : Natural;
      Data_Length : Long;
   end record;

   package Read_Queue_Vector is new Ada.Containers.Indefinite_Vectors (Positive, Queue_Item);
   Read_Queue : Read_Queue_Vector.Vector;

   procedure Read_String_Block;
   procedure Read_Type_Block;
   procedure Read_Type_Declaration (Last_End : in out Long);
   procedure Read_Field_Declaration (Type_Name : String);
   procedure Read_Field_Data;
   procedure Update_Base_Pool_Start_Index;

   procedure Create_Objects (Type_Name : String; Instance_Count : Natural);
   procedure Read_Queue_Vector_Iterator (Iterator : Read_Queue_Vector.Cursor);
   procedure Skip_Restrictions;

end ${packagePrefix.capitalize}.Internal.File_Reader;
""")

    out.close()
  }
}
