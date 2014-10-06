/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.api

import de.ust.skill.generator.ada.GeneralOutputMaker
import de.ust.skill.ir.Declaration
import scala.collection.JavaConversions._

trait SkillSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api.ads""")

    out.write(s"""
with Ada.Unchecked_Deallocation;

--
--  This package provides the API for the skill state.
--

package ${packagePrefix.capitalize}.Api is

   --  Appends new data into a skill file.
   procedure Append (State : access Skill_State);
   --  Closes the skill state.
   procedure Close (State : access Skill_State);
   --  Fills an empty skill state with the known types.
   procedure Create (State : access Skill_State);
   --  Reads a skill file into an emtpy skill state.
   procedure Read (
      State     : access Skill_State;
      File_Name :        String
   );
   --  Writes all data into a skill file.
   procedure Write (
      State     : access Skill_State;
      File_Name :        String
   );

  --  generated user types by the code generator
  --  please excuse that it is not pretty formatted
${
      var output = "";
      /**
       * Provides the API functions and procedures for all types.
       */
      for (d ← IR) {
        val parameters = d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map(f ⇒ s"${f.getSkillName()} : ${mapType(f.getType, d, f)}").mkString("; ", "; ", "")
        output += s"""   function New_${d.getName.ada} (State : access Skill_State${printParameters(d)}) return ${d.getName.ada}_Type_Access;\r\n"""
        output += s"""   procedure New_${d.getName.ada} (State : access Skill_State${printParameters(d)});\r\n"""
        output += s"""   function ${d.getName.ada}s_Size (State : access Skill_State) return Natural;\r\n"""
        output += s"""   function Get_${d.getName.ada} (State : access Skill_State; Index : Natural) return ${d.getName.ada}_Type_Access;\r\n"""
        output += s"""   function Get_${d.getName.ada}s (State : access Skill_State) return ${d.getName.ada}_Type_Accesses;\r\n"""
      }
      output
    }
end ${packagePrefix.capitalize}.Api;
""")

    out.close()
  }
}
