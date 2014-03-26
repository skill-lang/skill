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

   procedure Append (State : access Skill_State; File_Name : String) is
      package File_Writer renames Internal.File_Writer;
   begin
      if Append = State.State or else Read = State.State or else Write = State.State then
         File_Writer.Append (State, File_Name);
         State.State := Append;
      else
         raise Skill_State_Error;
      end if;
   end Append;

   procedure Create (State : access Skill_State) is
      package State_Maker renames Internal.State_Maker;
   begin
      if Unused = State.State then
         State_Maker.Create (State);
         State.State := Create;
      else
         raise Skill_State_Error;
      end if;
   end Create;

   procedure Read (State : access Skill_State; File_Name : String) is
      package File_Reader renames Internal.File_Reader;
      package State_Maker renames Internal.State_Maker;
   begin
      if Unused = State.State then
         File_Reader.Read (State, File_Name);
         State_Maker.Create (State);
         State.State := Read;
      else
         raise Skill_State_Error;
      end if;
   end Read;

   procedure Write (State : access Skill_State; File_Name : String) is
      package File_Writer renames Internal.File_Writer;
   begin
      if Append = State.State or else Create = State.State or else Read = State.State or else Write = State.State then
         File_Writer.Write (State, File_Name);
         State.State := Write;
      else
         raise Skill_State_Error;
      end if;
   end Write;
${
  def printFields(d : Declaration): String = {
    var output = s"""'(\r\n         skill_id => Natural (${if (null == d.getBaseType) escaped(d.getName) else escaped(d.getBaseType.getName)}_Type_Declaration.Storage_Pool.Length) + 1"""
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

  def printSuperTypes(d: Declaration): String = {
    var output = "";
    val superTypes = getSuperTypes(d).toList.reverse
    superTypes.foreach({ t =>
      output += s"""\r\n      ${escaped(t.getName)}_Type_Declaration.Storage_Pool.Append (Skill_Type_Access (New_Object));"""
    })
    output
  }

  var output = ""
  for (d ← IR) {
    output += s"""
   function New_${escaped(d.getName)} (State : access Skill_State${printParameters(d)}) return ${escaped(d.getName)}_Type_Access is
      ${escaped(d.getName)}_Type_Declaration : Type_Information := State.Types.Element ("${d.getSkillName}");${
  var output = "" 
  val superTypes = getSuperTypes(d).toList.reverse
  superTypes.foreach({ t =>
    output += s"""\r\n      ${escaped(t.getName)}_Type_Declaration : Type_Information := State.Types.Element ("${t.getSkillName}");"""
  })
  output
}
      New_Object : ${escaped(d.getName)}_Type_Access := new ${escaped(d.getName)}_Type${printFields(d)};
   begin
      ${escaped(d.getName)}_Type_Declaration.Storage_Pool.Append (Skill_Type_Access (New_Object));${printSuperTypes(d)}
      return New_Object;
   end New_${escaped(d.getName)};

   procedure New_${escaped(d.getName)} (State : access Skill_State${printParameters(d)}) is
      New_Object : ${escaped(d.getName)}_Type_Access := New_${escaped(d.getName)} (State${printSimpleParameters(d)});
   begin
      null;
   end New_${escaped(d.getName)};

   function ${escaped(d.getName)}s_Size (State : access Skill_State) return Natural is
      (Natural (State.Types.Element ("${d.getSkillName}").Storage_Pool.Length));

   function Get_${escaped(d.getName)} (State : access Skill_State; Index : Natural) return ${escaped(d.getName)}_Type_Access is
      (${escaped(d.getName)}_Type_Access (State.Types.Element ("${d.getSkillName}").Storage_Pool.Element (Index)));

   function Get_${escaped(d.getName)}s (State : access Skill_State) return ${escaped(d.getName)}_Type_Accesses is
      use Storage_Pool_Vector;

      Type_Declaration : Type_Information := State.Types.Element ("${d.getSkillName}");
      Length : Natural := Natural (Type_Declaration.Storage_Pool.Length);
      rval : ${escaped(d.getName)}_Type_Accesses := new ${escaped(d.getName)}_Type_Array (1 .. Length);

      procedure Iterate (Position : Cursor) is
      begin
         rval (To_Index (Position)) := ${escaped(d.getName)}_Type_Access (Element (Position));
      end Iterate;
      pragma Inline (Iterate);
   begin
      Type_Declaration.Storage_Pool.Iterate (Iterate'Access);
      return rval;
   end Get_${escaped(d.getName)}s;\r\n"""
  }
  output
}
end ${packagePrefix.capitalize}.Api.Skill;
""")

    out.close()
  }
}
