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

trait APIBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api.adb""")

    out.write(s"""
with Ada.Unchecked_Conversion;
with Ada.Unchecked_Deallocation;

with Skill.Errors;
with Skill.Equals;
with Skill.Field_Types;
with Skill.Files;
with Skill.Internal.File_Parsers;
with Skill.Internal.Parts;
with Skill.Streams;
with Skill.String_Pools;
with Skill.Types;
with Skill.Types.Pools;

with ${PackagePrefix}.Internal_Skill_Names;

-- parametrization of file, read/write and pool code
package body ${PackagePrefix}.Api is

   use type Skill.Types.Pools.Pool;

   -- TODO we can make this faster using a hash map (for large type systems)
   function New_Pool
     (Type_ID : Natural;
      Name    : Skill.Types.String_Access;
      Super   : Skill.Types.Pools.Pool) return Skill.Types.Pools.Pool
   is
   begin${
      (for (t ← IR)
        yield s"""
      if Skill.Equals.Equals (Name, Internal_Skill_Names.${escaped(t.getSkillName).capitalize}_Skill_Name) then
         return ${name(t)}_Pool_P.Make (Type_ID);
      end if;
"""
      ).mkString
    }
--        If null = Super then
--              return Unknown_Base (Type_ID, Name);
--        end if;
--
--              return Super.Make_Sub_Pool (Type_ID, Name);
      return null;
   end New_Pool;

   -- build a state from intermediate information
   function Make_State
     (Path          : Skill.Types.String_Access;
      Mode          : Skill.Files.Write_Mode;
      Strings       : Skill.String_Pools.Pool;
      Types         : Skill.Files.Type_Vector;
      Types_By_Name : Skill.Files.Type_Map) return File
   is${
      (for (t ← IR)
        yield s"""
      function Convert is new Ada.Unchecked_Conversion
        (Skill.Types.Pools.Pool,
         ${name(t)}_Pool);"""
      ).mkString
    }
   begin
      -- read fields
      -- TODO implementation

      -- make state
      return new File_T'
          (Path          => Path,
           Mode          => Mode,
           Strings       => Strings,
           Types         => Types,
           Types_By_Name => Types_By_Name${
      (
        for (t ← IR) yield s""",
           ${name(t)}s          =>
             Convert
               (Types_By_Name.Element (Internal_Skill_Names.${escaped(t.getSkillName).capitalize}_Skill_Name))"""
      ).mkString
    });
   end Make_State;

   -- type instantiation functions
   function Constant_Length_Array
     (Length : Skill.Types.v64;
      Base_T : Skill.Field_Types.Field_Type)
      return Skill.Field_Types.Field_Type
   is
   begin
      return null;
   end Constant_Length_Array;
   function Variable_Length_Array
     (Base_T : Skill.Field_Types.Field_Type)
      return Skill.Field_Types.Field_Type
   is
   begin
      return null;
   end Variable_Length_Array;
   function List_Type
     (Base_T : Skill.Field_Types.Field_Type)
      return Skill.Field_Types.Field_Type
   is
   begin
      return null;
   end List_Type;
   function Set_Type
     (Base_T : Skill.Field_Types.Field_Type)
      return Skill.Field_Types.Field_Type
   is
   begin
      return null;
   end Set_Type;
   function Map_Type
     (Key_T   : Skill.Field_Types.Field_Type;
      Value_T : Skill.Field_Types.Field_Type)
      return Skill.Field_Types.Field_Type
   is
   begin
      return null;
   end Map_Type;

   function Read is new Skill.Internal.File_Parsers.Read (File_T, File);

   function Open
     (Path    : String;
      Read_M  : Skill.Files.Read_Mode  := Skill.Files.Read;
      Write_M : Skill.Files.Write_Mode := Skill.Files.Write) return File
   is
   begin
      case Read_M is

         when Skill.Files.Read =>
            return Read (Skill.Streams.Input (new String'(Path)), Write_M);

         when Skill.Files.Create =>
            raise Skill.Errors.Skill_Error with "TBD";

            --          case Create:
            --              // initialization order of type information has to match file parser
            --              // and can not be done in place
            --              StringPool strings = new StringPool(null);
      --              ArrayList<StoragePool<?, ?>> types = new ArrayList<>(1);
            --              StringType stringType = new StringType(strings);
            --              Annotation annotation = new Annotation(types);
            --
            --              // create type information
            --              AgeAccess Age = new AgeAccess(0);
            --              types.add(Age);
            --              return new SkillState(strings, types, stringType, annotation, path, actualMode.close);
            --
      end case;
   end Open;

   procedure Flush (This : access File_T) is
   begin
      null;
      -- TODO
   end Flush;

   procedure Close (This : access File_T) is
      procedure Delete is new Ada.Unchecked_Deallocation
        (String,
         Skill.Types.String_Access);

      procedure Delete (This : Skill.Types.Pools.Pool) is
      begin
         This.Dynamic.Free;
      end Delete;

      type Ft is access all File_T;

      procedure Delete is new Ada.Unchecked_Deallocation (File_T, Ft);

      Self : Ft := Ft (This);
   begin
      This.Flush;

      Delete (This.Path);
      -- TODO       This.Strings.Free;
      This.Types.Foreach (Delete'Access);
      This.Types.Free;

      Delete (Self);
   end Close;
${
      (for (t ← IR) yield s"""
   function ${name(t)}s (This : access File_T) return ${name(t)}_Pool is
   begin
      return This.${name(t)}s;
   end ${name(t)}s;
"""
      ).mkString
    }
end ${PackagePrefix}.Api;
""")

    out.close()
  }
}
