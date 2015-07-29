/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.api.internal

import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._

trait PoolsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    makeSpec
    if (!IR.isEmpty)
      makeBody
  }

  private final def makeSpec {

    val out = open(s"""skill-types-pools-${packagePrefix.replace('-', '_')}_pools.ads""")

    out.write(s"""
with Skill.Files;
with Skill.Internal.File_Parsers;
with Skill.Types.Pools;
with Skill.Types;
with Skill.Types.Vectors;

with $PackagePrefix;

-- instantiated pool packages
-- GNAT Bug workaround; should be "new Base(...)" instead
package Skill.Types.Pools.${PackagePrefix.replace('.', '_')}_Pools is
${
      (for (t ← IR) yield {
        val Name = name(t)
        val Type = PackagePrefix+"."+Name
        s"""
   package ${Name}_P is

      type Pool_T is new Base_Pool_T with private;
      type Pool is access Pool_T;

      -- API methods
      function Get (This : access Pool_T; ID : Skill_ID_T) return $Type;

      ----------------------
      -- internal methods --
      ----------------------

      -- constructor invoked by new_pool
      function Make (Type_Id : Natural) return Pools.Pool;
      -- destructor invoked by close
      procedure Free (This : access Pool_T);

      overriding
      function Add_Field
        (This : access Pool_T;
         ID   : Natural;
         T    : Field_Types.Field_Type;
         Name : String_Access) return Skill.Field_Declarations.Field_Declaration;

      overriding function Insert_Instance
        (This : access Pool_T;
         ID   : Skill_ID_T) return Boolean;

      overriding function Static_Size (This : access Pool_T) return Natural;

      -- applies F for each element in this
--        procedure Foreach
--          (This : access Pool_T;
--           F    : access procedure (I : $Name));

   private

      package A1 is new Vectors (Natural, $Type);
      subtype Instance_Vector is A1.Vector;

      type Pool_T is new Base_Pool_T with record
         Static_Data : Instance_Vector;
         New_Objects : Instance_Vector;
      end record;
   end ${Name}_P;
"""
      }).mkString
    }
end Skill.Types.Pools.${PackagePrefix.replace('.', '_')}_Pools;
""")

    out.close()
  }

  private final def makeBody {

    val out = open(s"""skill-types-pools-${packagePrefix.replace('-', '_')}_pools.adb""")

    out.write(s"""
with Ada.Unchecked_Conversion;
with Ada.Unchecked_Deallocation;

with Skill.Equals;
with Skill.Errors;
with Skill.Field_Types;
with Skill.Files;
with Skill.Internal.File_Parsers;
with Skill.Internal.Parts;
with Skill.Streams;
with Skill.String_Pools;
with Skill.Types.Pools;
with Skill.Types;
with Skill.Types.Vectors;

with $PackagePrefix.Api;
with $PackagePrefix.Internal_Skill_Names;${
      (for (t ← IR; f ← t.getFields) yield s"""
with $PackagePrefix.Known_Field_${escaped(t.getName.ada())}_${escaped(f.getName.ada())};""").mkString
    }

-- instantiated pool packages
-- GNAT Bug workaround; should be "new Base(...)" instead
package body Skill.Types.Pools.${PackagePrefix.replace('.', '_')}_Pools is
${
      (for (t ← IR) yield {
        val Name = name(t)
        val Type = PackagePrefix+"."+Name
        s"""
   package body ${Name}_P is

      -- API methods
      function Get (This : access Pool_T; ID : Skill_ID_T) return $Type is
      begin
         if 0 = ID then
            return null;
         else
            return ${PackagePrefix}.To_$Name (This.Data (ID));
         end if;
      end Get;

      ----------------------
      -- internal methods --
      ----------------------

      -- constructor invoked by new_pool
      function Make (Type_Id : Natural) return Skill.Types.Pools.Pool is
         function Convert is new Ada.Unchecked_Conversion
           (Source => Pool,
            Target => Skill.Types.Pools.Base_Pool);
         function Convert is new Ada.Unchecked_Conversion
           (Source => Pool,
            Target => Skill.Types.Pools.Pool);

         This : Pool;
      begin
         This :=
           new Pool_T'
             (Name          => ${internalSkillName(t)},
              Type_Id       => Type_Id,
              Super         => null,
              Base          => null,
              Sub_Pools     => Sub_Pool_Vector_P.Empty_Vector,
              Data_Fields_F =>
                Skill.Field_Declarations.Field_Vector_P.Empty_Vector,
              Blocks      => Skill.Internal.Parts.Blocks_P.Empty_Vector,
              Fixed       => False,
              Cached_Size => 0,
              Data        => Skill.Types.Pools.Empty_Data,
              Owner       => null,
              Static_Data => A1.Empty_Vector,
              New_Objects => A1.Empty_Vector);

         This.Base := Convert (This);
         return Convert (This);
      exception
         when E : others =>
            Skill.Errors.Print_Stacktrace (E);
            Skill.Errors.Print_Stacktrace;
            raise Skill.Errors.Skill_Error with "$Name pool allocation failed";
      end Make;

      procedure Free (This : access Pool_T) is

         procedure Delete
           (This : Skill.Field_Declarations.Field_Declaration)
         is
         begin
            This.Free;
         end Delete;

         Data : Annotation_Array := This.Data;
         procedure Delete is new Ada.Unchecked_Deallocation
           (Skill.Types.Skill_Object,
            Skill.Types.Annotation);
         procedure Delete is new Ada.Unchecked_Deallocation
           (Skill.Types.Annotation_Array_T,
            Skill.Types.Annotation_Array);

         type P is access all Pool_T;
         procedure Delete is new Ada.Unchecked_Deallocation (Pool_T, P);
         D : P := P (This);
      begin
         for I in Data'Range loop
            Delete (Data (I));
         end loop;
         Delete (Data);

         This.Sub_Pools.Free;
         This.Data_Fields_F.Foreach (Delete'Access);
         This.Data_Fields_F.Free;
         This.Blocks.Free;
         This.Static_Data.Free;
         This.New_Objects.Free;
         Delete (D);
      end Free;

      function Add_Field
        (This : access Pool_T;
         ID   : Natural;
         T    : Field_Types.Field_Type;
         Name : String_Access)
         return Skill.Field_Declarations.Field_Declaration
      is
         pragma Warnings (Off);

         type P is access all Pool_T;
         function Convert is new Ada.Unchecked_Conversion
           (P, Field_Declarations.Owner_T);

         F : Field_Declarations.Field_Declaration;

         type Super is access all Base_Pool_T;
      begin
${
          t.getFields.foldRight("""
         return Super (This).Add_Field (ID, T, Name);""") {
            case (f, s) ⇒ s"""
         if Skill.Equals.Equals
             (Name,
              ${internalSkillName(f)})
         then
            F := ${PackagePrefix}.Known_Field_${escaped(t.getName.ada)}_${escaped(f.getName.ada)}.Make (ID, T, Convert (P (This)));
         else$s
         end if;"""
          }
        }${
          if (t.getFields.isEmpty()) ""
          else """

         -- TODO restrictions
         --          for (FieldRestriction<?> r : restrictions)
         --              f.addRestriction(r);
         This.Data_Fields.Append (F);

         return F;"""
        }
      end Add_Field;

      overriding function Insert_Instance
        (This : access Pool_T;
         ID   : Skill.Types.Skill_ID_T) return Boolean
      is
         function Convert is new Ada.Unchecked_Conversion
           (Source => $Type,
            Target => Skill.Types.Annotation);

         I : Natural := Natural (ID);
         R : $Type;
      begin
         if null /= This.Data (I) then
            return False;
         end if;

         R             := new ${Type}_T;
         R.Skill_ID    := ID;
         This.Data (I) := Convert (R);
         This.Static_Data.Append (R);
         return True;
      end Insert_Instance;

      overriding function Static_Size (This : access Pool_T) return Natural is
      begin
         return This.Static_Data.Length + This.New_Objects.Length;
      end Static_Size;

   end ${Name}_P;
"""
      }).mkString
    }
end Skill.Types.Pools.${PackagePrefix.replace('.', '_')}_Pools;
""")

    out.close()
  }
}
