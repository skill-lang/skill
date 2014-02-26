/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.api

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait SkillSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-skill.ads""")

    out.write(s"""
with ${packagePrefix.capitalize}.Internal.File_Parser;

package ${packagePrefix.capitalize}.Api.Skill is

${
    var output = "";
	for (declaration ← IR) {
	  val name = declaration.getName
	  output += s"""   type %s_Type_Accesses is array (Natural range <>) of %s_Type_Access;\r\n""".format(name, name)
	}
	output
   }
   procedure Read (State : access Skill_State; File_Name : String);

${
    var output = "";
	for (declaration ← IR) {
	  val name = declaration.getName
	  output += s"""   function Get_%ss (State : access Skill_State) return %s_Type_Accesses;\r\n""".format(name, name)
	}
	output
   }
end ${packagePrefix.capitalize}.Api.Skill;
""")

    out.close()
  }
}
