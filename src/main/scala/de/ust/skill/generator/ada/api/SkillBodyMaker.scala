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
      s"${f.getSkillName()} : ${mapType(f.getType, d, f)}"
    }).mkString("; ", "; ", "")
    if (hasFields) output else ""
  }

  def printSimpleParameters(d : Declaration): String = {
    var output = "";
    var hasFields = false
    output += d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map({ f =>
      hasFields = true
      f.getSkillName()
    }).mkString(", ", ", ", "")
    if (hasFields) output else ""
  }

  var output = "";
  for (d ← IR) {
    output += s"""
   function New_${d.getName} (State : access Skill_State${printParameters(d)}) return ${d.getName}_Type_Access is
      New_Object : ${d.getName}_Type_Access := new ${d.getName}_Type${printFields(d)};
   begin
      State.Put_Object ("${d.getSkillName}", Skill_Type_Access (New_Object));
      return New_Object;
   end New_${d.getName};

   procedure New_${d.getName} (State : access Skill_State${printParameters(d)}) is
      New_Object : ${d.getName}_Type_Access := New_${d.getName} (State${printSimpleParameters(d)});
   begin
      null;
   end New_${d.getName};

   function ${d.getName}s_Size (State : access Skill_State) return Natural is
      (State.Storage_Pool_Size ("${d.getSkillName}"));

   function Get_${d.getName} (State : access Skill_State; Index : Natural) return ${d.getName}_Type_Access is
      function Convert is new Ada.Unchecked_Conversion (Skill_Type_Access, ${d.getName}_Type_Access);
      pragma Inline (Convert);
   begin
      return Convert (State.Get_Object ("${d.getSkillName}", Index));
   end Get_${d.getName};\r\n"""
  }
  output
}
end ${packagePrefix.capitalize}.Api.Skill;
""")

    out.close()
  }
}
