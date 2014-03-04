/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.api

import java.io.PrintWriter
import scala.collection.JavaConversions._
import de.ust.skill.generator.ada.GeneralOutputMaker

trait SkillBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-skill.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Api.Skill is

   procedure Read (State : access Skill_State; File_Name : String) is
      package File_Reader renames Internal.File_Reader;
   begin
      File_Reader.Read (State, File_Name);
   end Read;

   procedure Write (State : access Skill_State; File_Name : String) is
      package File_Writer renames Internal.File_Writer;
   begin
      File_Writer.Write (State, File_Name);
   end Write;
${
  var output = "";
  for (d ← IR) {
    val parameters = d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map(f => s"${f.getSkillName()} : ${mapType(f.getType)}").mkString("; ", "; ", "")
    val fields = d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map(f => s"         ${f.getSkillName} => ${f.getSkillName}").mkString("'(\r\n", ",\r\n", "\r\n      )")

    output += s"""
   procedure New_${d.getName} (State : access Skill_State${parameters}) is
      New_Object : ${d.getName}_Type_Access := new ${d.getName}_Type${fields};
   begin
      State.Put_Object ("${d.getSkillName}", Skill_Type_Access (New_Object));
   end New_${d.getName};

   function Get_${d.getName}s (State : access Skill_State) return ${d.getName}_Type_Accesses is
      Length : Natural := State.Storage_Pool_Size ("${d.getSkillName}");
      rval : ${d.getName}_Type_Accesses (1 .. Length);
   begin
      for I in rval'Range loop
         rval (I) := ${d.getName}_Type_Access (State.Get_Object ("${d.getSkillName}", I));
      end loop;
      return rval;
   end Get_${d.getName}s;\r\n"""
  }
  output
}
end ${packagePrefix.capitalize}.Api.Skill;
""")

    out.close()
  }
}
