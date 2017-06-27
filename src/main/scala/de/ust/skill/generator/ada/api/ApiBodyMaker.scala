/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
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
    val out = files.open(s"""${packagePrefix}-api.adb""")

    out.write(s"""
with Ada.Unchecked_Conversion;
with Ada.Unchecked_Deallocation;

with Skill.Errors;
with Skill.Equals;
with Skill.Field_Types;
with Skill.Field_Types.Builtin;
with Skill.Field_Types.Builtin.String_Type_P;
with Skill.Files;
with Skill.Internal.File_Parsers;
with Skill.Internal.Parts;
with Skill.Streams;
with Skill.String_Pools;
with Skill.Types;
with Skill.Types.Pools;
with Skill.Types.Pools.Unknown_Base;

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
      if Skill.Equals.Equals (Name, ${internalSkillName(t)}) then${
        if (null == t.getSuperType) """
         if null /= Super then
            raise Skill.Errors.Skill_Error
              with "Expected no super type, but found: " &
              Super.Skill_Name.all;
         end if;"""
        else s"""
         if null = Super then
            raise Skill.Errors.Skill_Error
              with "Expected super type ${t.getSuperType.getSkillName}, but found <<none>>";
         elsif not Skill.Equals.Equals (Super.Skill_Name, ${internalSkillName(t.getSuperType)}) then
            raise Skill.Errors.Skill_Error
              with "Expected super type ${t.getSuperType.getSkillName}, but found: " &
              Super.Skill_Name.all;
         end if;"""
      }

         return ${name(t)}_Pool_P.Make_Pool (Type_ID${
        if (null == t.getSuperType) ""
        else ", Super"
      });
      end if;
"""
      ).mkString
    }
      if null = Super then
         return Skill.Types.Pools.Unknown_Base.Make (Type_ID, Name);
      end if;

      return Super.Dynamic.Make_Sub_Pool (Type_ID, Name);
   end New_Pool;

   -- build a state from intermediate information
   function Make_State
     (Path          : Skill.Types.String_Access;
      Mode          : Skill.Files.Write_Mode;
      Strings       : Skill.String_Pools.Pool;
      String_Type   : Skill.Field_Types.Builtin.String_Type_P.Field_Type;
      Annotation_Type : Skill.Field_Types.Builtin.Annotation_Type_P.Field_Type;
      Types         : Skill.Types.Pools.Type_Vector;
      Types_By_Name : Skill.Types.Pools.Type_Map) return File
   is
      pragma Warnings (Off);
${
      (for (t ← IR)
        yield s"""
      function Convert is new Ada.Unchecked_Conversion
        (Skill.Types.Pools.Pool,
         ${name(t)}_Pool);"""
      ).mkString
    }

      Rval      : File;
      P         : Skill.Types.Pools.Pool;
      TBN_Local : Skill.Types.Pools.Type_Map := Types_By_Name;
   begin
      -- create missing type information
${
      (for (t ← IR)
        yield s"""
      if not TBN_Local.Contains
        (${internalSkillName(t)})
      then
         P := ${name(t)}_Pool_P.Make_Pool (32 + Natural (Types.Length)${
        if (null == t.getSuperType) ""
        else s", TBN_Local.Element (${internalSkillName(t.getSuperType)})"
      });
         Types.Append (P);
         TBN_Local.Include
         (${internalSkillName(t)}, P);
      end if;"""
      ).mkString
    }

      Rval :=
        new File_T'
          (Path          => Path,
           Mode          => Mode,
           Strings       => Strings,
           String_Type   => String_Type,
           Annotation_Type => Annotation_Type,
           Types         => Types,
           Types_By_Name => TBN_Local${
      (
        for (t ← IR) yield s""",
           ${escapedLonely(name(t)+"s")}          =>
             Convert
               (TBN_Local.Element (${internalSkillName(t)}))"""
      ).mkString
    });

      -- read fields
      Rval.Finalize_Pools;

      -- make state
      return Rval;
   end Make_State;

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
         -- initialization order of type information has to match file parser
         -- and can not be done in place

            declare
               Strings : Skill.String_Pools.Pool :=
                 Skill.String_Pools.Create (Skill.Streams.Input (null));
               Types : Skill.Types.Pools.Type_Vector :=
                 Skill.Types.Pools.P_Type_Vector.Empty_Vector;
               String_Type : Skill.Field_Types.Builtin.String_Type_P
                 .Field_Type :=
                 Skill.Field_Types.Builtin.String_Type_P.Make (Strings);
               Annotation_Type : Skill.Field_Types.Builtin.Annotation_Type_P
                 .Field_Type :=
                 Skill.Field_Types.Builtin.Annotation (Types);
            begin
               return Make_State
                   (Path            => new String'(Path),
                    Mode            => Write_M,
                    Strings         => Strings,
                    String_Type     => String_Type,
                    Annotation_Type => Annotation_Type,
                    Types           => Types,
                    Types_By_Name   => Skill.Types.Pools.P_Type_Map.Empty_Map);
            end;
      end case;
   end Open;

   procedure Free (This : access File_T) is
      procedure Delete is new Ada.Unchecked_Deallocation
        (String,
         Skill.Types.String_Access);

      procedure Delete (This : Skill.Types.Pools.Pool) is
      begin
         This.Dynamic.Free;
      end Delete;

      type Ft is access all File_T;

      procedure Delete is new Ada.Unchecked_Deallocation (File_T, Ft);
      procedure Delete is new Ada.Unchecked_Deallocation
        (Skill.Field_Types.Builtin.Annotation_Type_P.Field_Type_T,
         Skill.Field_Types.Builtin.Annotation_Type_P.Field_Type);
      procedure Delete is new Ada.Unchecked_Deallocation
        (Skill.Field_Types.Builtin.String_Type_P.Field_Type_T,
         Skill.Field_Types.Builtin.String_Type_P.Field_Type);

      Self : Ft := Ft (This);
   begin
      Delete (This.Path);
      This.Strings.Free;
      This.Types.Foreach (Delete'Access);
      This.Types.Free;
      Delete (This.String_Type);
      Delete (This.Annotation_Type);

      Delete (Self);
   end Free;

   procedure Close (This : access File_T) is
   begin
      This.Flush;
      This.Free;
   end Close;
${
      (for (t ← IR) yield s"""
   function ${escapedLonely(name(t)+"s")} (This : not null access File_T) return ${name(t)}_Pool is
   begin
      return This.${escapedLonely(name(t)+"s")};
   end ${escapedLonely(name(t)+"s")};
"""
      ).mkString
    }
end ${PackagePrefix}.Api;
""")

    out.close()
  }
}
