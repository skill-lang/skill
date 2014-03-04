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

generic
package ${packagePrefix.capitalize}.Internal.File_Writer is

   procedure Write (pState : access Skill_State; File_Name : String);

private

   procedure Write_String_Pool;

end ${packagePrefix.capitalize}.Internal.File_Writer;
""")

    out.close()
  }
}
