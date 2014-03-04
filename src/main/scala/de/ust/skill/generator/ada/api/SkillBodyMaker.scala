/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.api

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait SkillBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-skill.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Api.Skill is

   procedure Read (State : access Skill_State; File_Name : String) is
      package File_Reader is new ${packagePrefix.capitalize}.Internal.File_Reader;
   begin
      File_Reader.Read (State, File_Name);
   end Read;

   procedure Write (State : access Skill_State; File_Name : String) is
      package File_Writer is new ${packagePrefix.capitalize}.Internal.File_Writer;
   begin
      File_Writer.Write (State, File_Name);
   end Write;

${
    var output = "";
	for (declaration ← IR) {
	  val name = declaration.getName
	  val skillName = declaration.getSkillName
	  output += s"""   function Get_${name}s (State : access Skill_State) return ${name}_Type_Accesses is
      Length : Natural := State.Storage_Pool_Size ("${skillName}");
      rval : ${name}_Type_Accesses (1 .. Length);
   begin
      for I in rval'Range loop
         rval (I) := ${name}_Type_Access (State.Get_Object ("${skillName}", I));
      end loop;
      return rval;
   end Get_${name}s;\r\n\r\n"""
	}
	output.stripSuffix("\r\n")
   }
end ${packagePrefix.capitalize}.Api.Skill;
""")

    out.close()
  }
}
