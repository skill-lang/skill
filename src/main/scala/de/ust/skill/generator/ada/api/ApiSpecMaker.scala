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
--  This package provides the API for Skill_State
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

  --  type access functionality for each user types
  --  provides new, size and get operations.
${
      /**
       * Provides the API functions and procedures for all types.
       */
      (for (d ← IR) yield {
        val parameters = d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map(f ⇒ s"${f.getSkillName()} : ${mapType(f.getType, d, f)}").mkString("; ", "; ", "")
        s"""   function New_${name(d)} (State : access Skill_State${printParameters(d)}) return ${name(d)}_Type_Access;
   procedure New_${name(d)} (State : access Skill_State${printParameters(d)});
   function ${name(d)}s_Size (State : access Skill_State) return Natural;
   function Get_${name(d)} (State : access Skill_State; Index : Natural) return ${name(d)}_Type_Access;
   function Get_${name(d)}s (State : access Skill_State) return ${name(d)}_Type_Accesses;
"""
      }).mkString("\n")
    }
end ${packagePrefix.capitalize}.Api;
""")

    out.close()
  }
}
