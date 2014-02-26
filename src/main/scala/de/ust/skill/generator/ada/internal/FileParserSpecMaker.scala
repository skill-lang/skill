/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait FileParserSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-file_parser.ads""")

    out.write(s"""
with ${packagePrefix.capitalize}.Internal.Byte_Reader;

generic
package ${packagePrefix.capitalize}.Internal.File_Parser is

   procedure Read (pState : access Skill_State; File_Name : String);

private

   procedure Read_String_Block;
   procedure Read_Type_Block;
   procedure Read_Type_Declaration;
   procedure Read_Field_Declaration (Type_Name : String);
   procedure Read_Field_Data;

   procedure Create_Objects (Type_Name : String; Instance_Count : Long);
   procedure Data_Chunk_Vector_Iterator (Iterator : Data_Chunk_Vector.Cursor);
   procedure Skip_Restrictions;

end ${packagePrefix.capitalize}.Internal.File_Parser;
""")

    out.close()
  }
}
