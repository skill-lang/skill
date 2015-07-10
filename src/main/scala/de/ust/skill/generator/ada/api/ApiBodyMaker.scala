/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.api

import de.ust.skill.generator.ada.GeneralOutputMaker
import de.ust.skill.ir.Declaration
import scala.collection.JavaConversions._
import de.ust.skill.ir.UserType

trait SkillBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api.adb""")

    out.write(s"""
with ${packagePrefix.capitalize}.Api.Internal.File_Writer;
with ${packagePrefix.capitalize}.Api.Internal.State_Maker;
with ${packagePrefix.capitalize}.Api.Internal.File_Reader;

package body ${packagePrefix.capitalize}.Api is

   procedure Append (State : access Skill_State) is
      package File_Writer renames Api.Internal.File_Writer;
   begin
      if Append = State.State or else Read = State.State or else Write = State.State then
         File_Writer.Append (State, State.File_Name.all);
         State.State := Append;
      else
         raise Skill_State_Error;
      end if;
   end Append;

   procedure Close (State : access Skill_State) is
      procedure Free is new Ada.Unchecked_Deallocation (String_Pool_Vector.Vector, String_Pool_Access);
      procedure Free is new Ada.Unchecked_Deallocation (Types_Hash_Map.Map, Types_Hash_Map_Access);
      procedure Free is new Ada.Unchecked_Deallocation (String, String_Access);

      procedure Iterate_Storage_Pool (Position : Storage_Pool_Vector.Cursor) is
         procedure Free is new Ada.Unchecked_Deallocation (Skill_Type'Class, Skill_Type_Access);

         Object : Skill_Type_Access := Storage_Pool_Vector.Element (Position);
      begin
         Free (Object);
      end Iterate_Storage_Pool;
      pragma Inline (Iterate_Storage_Pool);

      procedure Iterate_Field_Declaration (Position : Fields_Vector.Cursor) is
         procedure Free is new Ada.Unchecked_Deallocation (Field_Declaration, Field_Information);

         Field_Declaration : Field_Information := Fields_Vector.Element (Position);
      begin
         Free (Field_Declaration);
      end Iterate_Field_Declaration;
      pragma Inline (Iterate_Field_Declaration);

      procedure Iterate_Type_Declaration (Position : Types_Hash_Map.Cursor) is
         procedure Free is new Ada.Unchecked_Deallocation (Type_Declaration, Type_Information);

         Type_Declaration : Type_Information := Types_Hash_Map.Element (Position);
      begin
         Type_Declaration.Fields.Iterate (Iterate_Field_Declaration'Access);
         Type_Declaration.Storage_Pool.Iterate (Iterate_Storage_Pool'Access);
         Free (Type_Declaration);
      end Iterate_Type_Declaration;
      pragma Inline (Iterate_Type_Declaration);
   begin
      State.Types.Iterate (Iterate_Type_Declaration'Access);

      Free (State.File_Name);
      Free (State.String_Pool);
      Free (State.Types);

      State.State := Unused;
   end Close;

   procedure Create (State : access Skill_State) is
      package State_Maker renames Api.Internal.State_Maker;
   begin
      if Unused = State.State then
         State_Maker.Create (State);
         State.State := Create;
      else
         raise Skill_State_Error;
      end if;
   end Create;

   procedure Read (
      State     : access Skill_State;
      File_Name :        String
   ) is
      package File_Reader renames Api.Internal.File_Reader;
      package State_Maker renames Api.Internal.State_Maker;
   begin
      if Unused = State.State then
         File_Reader.Read (State, File_Name);
         State_Maker.Create (State);
         State.File_Name := new String'(File_Name);
         State.State     := Read;
      else
         raise Skill_State_Error;
      end if;
   end Read;

   procedure Write (
      State     : access Skill_State;
      File_Name :        String
   ) is
      package File_Writer renames Api.Internal.File_Writer;
   begin
      if Append = State.State or else Create = State.State or else Read = State.State or else Write = State.State then
         File_Writer.Write (State, File_Name);
         State.File_Name := new String'(File_Name);
         State.State     := Write;
      else
         raise Skill_State_Error;
      end if;
   end Write;
${
      /**
       * Provides the fields of a given type as a comma-separated list used as record attributes.
       */
      def printFields(d : UserType) : String = {
        var output = s"""'(\r\n         skill_id => Natural (${
          if (null == d.getBaseType) name(d)
          else name(d.getBaseType)
        }_Type_Declaration.Storage_Pool.Length) + 1"""
        output += d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map({ f ⇒
          s",\r\n         ${escapedLonely(f.getSkillName)} => ${escapedLonely(f.getSkillName)}"
        }).mkString("")
        output += "\r\n      )";
        output
      }

      /**
       * Provides the fields of a given type as a comma-separated list used as parameters.
       */
      def printSimpleParameters(d : UserType) : String = {
        var output = "";
        var hasFields = false
        output += d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored }).map({ f ⇒
          hasFields = true
          escapedLonely(f.getSkillName())
        }).mkString(", ", ", ", "")
        if (hasFields) output else ""
      }

      /**
       * Pushes the new object also into the storage pools of the super types.
       */
      def printSuperTypes(d : UserType) : String = {
        var output = "";
        val superTypes = getSuperTypes(d).toList.reverse
        superTypes.foreach({ t ⇒
          output += s"""\r\n      ${name(t)}_Type_Declaration.Storage_Pool.Append (Skill_Type_Access (New_Object));"""
        })
        output
      }

      var output = ""
      /**
       * Provides the API functions and procedures for all types.
       */
      for (d ← IR) {
        val nameD = name(d)
        output += s"""
   function New_${nameD} (State : access Skill_State${printParameters(d)}) return ${nameD}_Type_Access is
      ${nameD}_Type_Declaration : Type_Information := State.Types.Element (${nameD}_Type_Skillname);${
          var output = ""
          val superTypes = getSuperTypes(d).toList.reverse
          superTypes.foreach({ t ⇒
            output += s"""\r\n      ${t.getName.ada}_Type_Declaration : Type_Information := State.Types.Element (${name(t)}_Type_Skillname);"""
          })
          output
        }
      New_Object : ${nameD}_Type_Access := new ${nameD}_Type${printFields(d)};
   begin
      ${nameD}_Type_Declaration.Storage_Pool.Append (Skill_Type_Access (New_Object));${printSuperTypes(d)}
      return New_Object;
   end New_${nameD};

   procedure New_${nameD} (State : access Skill_State${printParameters(d)}) is
      New_Object : ${nameD}_Type_Access := New_${nameD} (State${printSimpleParameters(d)});
   begin
      null;
   end New_${nameD};

   function ${nameD}s_Size (State : access Skill_State) return Natural is
      (Natural (State.Types.Element (${nameD}_Type_Skillname).Storage_Pool.Length));

   function Get_${nameD} (State : access Skill_State; Index : Natural) return ${nameD}_Type_Access is
      (${nameD}_Type_Access (State.Types.Element (${nameD}_Type_Skillname).Storage_Pool.Element (Index)));

   function Get_${nameD}s (State : access Skill_State) return ${nameD}_Type_Accesses is
      use Storage_Pool_Vector;

      Type_Declaration : Type_Information := State.Types.Element (${nameD}_Type_Skillname);
      Length : Natural := Natural (Type_Declaration.Storage_Pool.Length);
      rval : ${nameD}_Type_Accesses := new ${nameD}_Type_Array (1 .. Length);

      procedure Iterate (Position : Cursor) is
      begin
         rval (To_Index (Position)) := ${nameD}_Type_Access (Element (Position));
      end Iterate;
      pragma Inline (Iterate);
   begin
      Type_Declaration.Storage_Pool.Iterate (Iterate'Access);
      return rval;
   end Get_${nameD}s;\r\n"""
      }
      output
    }
end ${packagePrefix.capitalize}.Api;
""")

    out.close()
  }
}
