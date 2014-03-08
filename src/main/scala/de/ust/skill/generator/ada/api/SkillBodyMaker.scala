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

trait SkillBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-skill.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Api.Skill is

   procedure Create (State : access Skill_State) is
      package State_Maker renames Internal.State_Maker;

      Skill_State_Already_Consumed : exception;
   begin
      if False = State.Is_Consumed then
         State_Maker.Create (State);
         State.Consume;
      else
         raise Skill_State_Already_Consumed;
      end if;
   end Create;

   procedure Read (State : access Skill_State; File_Name : String) is
      package File_Reader renames Internal.File_Reader;
      package State_Maker renames Internal.State_Maker;

      Skill_State_Already_Consumed : exception;
   begin
      if False = State.Is_Consumed then
         File_Reader.Read (State, File_Name);
         State_Maker.Create (State);
         State.Consume;
      else
         raise Skill_State_Already_Consumed;
      end if;
   end Read;

   procedure Write (State : access Skill_State; File_Name : String) is
      package File_Writer renames Internal.File_Writer;

      Skill_State_Not_Consumed : exception;
   begin
      if True = State.Is_Consumed then
         File_Writer.Write (State, File_Name);
      else
         raise Skill_State_Not_Consumed;
      end if;
   end Write;
${
  def printFields(d : Declaration): String = {
    var output = s"""'(\r\n         skill_id => State.Storage_Pool_Size ("${d.getSkillName}") + 1"""
    output += d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map({ f =>
      s",\r\n         ${f.getSkillName} => ${f.getSkillName}"
    }).mkString("")
    output += "\r\n      )";
    output
  }

  def printParameters(d : Declaration): String = {
    var output = "";
    var hasFields = false
    output += d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map({ f =>
      hasFields = true
      s"${f.getSkillName()} : ${mapType(f.getType)}"
    }).mkString("; ", "; ", "")
    if (hasFields) output else ""
  }

  var output = "";
  for (d ← IR) {
    output += s"""
   procedure New_${d.getName} (State : access Skill_State${printParameters(d)}) is
      New_Object : ${d.getName}_Type_Access := new ${d.getName}_Type${printFields(d)};
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
