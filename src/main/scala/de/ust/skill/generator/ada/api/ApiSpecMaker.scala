/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.api

import java.io.PrintWriter
import scala.collection.JavaConversions._
import de.ust.skill.generator.ada.GeneralOutputMaker
import de.ust.skill.ir.Declaration

trait SkillSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api.ads""")

    out.write(s"""
with Ada.Unchecked_Deallocation;

package ${packagePrefix.capitalize}.Api is

   procedure Append (State : access Skill_State);
   procedure Close (State : access Skill_State);
   procedure Create (State : access Skill_State);
   procedure Read (State : access Skill_State; File_Name : String);
   procedure Write (State : access Skill_State; File_Name : String);

${
  def printParameters(d : Declaration): String = {
    var hasFields = false;
    var output = "";
    output += d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map({ f =>
      hasFields = true;
      s"${f.getSkillName()} : ${mapType(f.getType, d, f)}"
    }).mkString("; ", "; ", "")
    if (hasFields) output else ""
  }

  var output = "";
  for (d ← IR) {
    val parameters = d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map(f => s"${f.getSkillName()} : ${mapType(f.getType, d, f)}").mkString("; ", "; ", "")
    output += s"""   function New_${escaped(d.getName)} (State : access Skill_State${printParameters(d)}) return ${escaped(d.getName)}_Type_Access;\r\n"""
    output += s"""   procedure New_${escaped(d.getName)} (State : access Skill_State${printParameters(d)});\r\n"""
    output += s"""   function ${escaped(d.getName)}s_Size (State : access Skill_State) return Natural;\r\n"""
    output += s"""   function Get_${escaped(d.getName)} (State : access Skill_State; Index : Natural) return ${escaped(d.getName)}_Type_Access;\r\n"""
    output += s"""   function Get_${escaped(d.getName)}s (State : access Skill_State) return ${escaped(d.getName)}_Type_Accesses;\r\n"""
  }
  output
}
end ${packagePrefix.capitalize}.Api;
""")

    out.close()
  }
}
