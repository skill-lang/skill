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

   State : access Skill_State;

   procedure Write (pState : access Skill_State; File_Name : String) is
      Output_File : ASS_IO.File_Type;
   begin
      State := pState;

      ASS_IO.Create (Output_File, ASS_IO.Out_File, File_Name);
      Byte_Writer.Initialize (ASS_IO.Stream (Output_File));

      Write_String_Pool;

      ASS_IO.Close (Output_File);
   end Write;

   procedure Write_String_Pool is
      Size : Natural := State.String_Pool_Size;
      Last_Length : Natural := 0;
   begin
      Byte_Writer.Write_v64 (Long (Size));

      for I in 1 .. Size loop
         declare
            Length : Positive := State.Get_String (I)'Length + Last_Length;
         begin
            Byte_Writer.Write_i32 (Length);
            Last_Length := Length;
         end;
      end loop;

      for I in 1 .. Size loop
         Byte_Writer.Write_String (State.Get_String (I));
      end loop;
   end Write_String_Pool;

end ${packagePrefix.capitalize}.Internal.File_Writer;
""")

    out.close()
  }
}
