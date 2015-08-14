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
with Skill.Types;
with Skill.Types.Pools;
with Skill.Types.Pools.Sub;
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

      -- constructor for instances
      procedure Make
        (This  : access Pool_T${
          (
            for (f ← t.getAllFields)
              yield s""";
         F_${name(f)} : ${mapType(f.getType)} := ${defaultValue(f)}"""
          ).mkString
        });
      function Make
        (This  : access Pool_T${
          (
            for (f ← t.getAllFields)
              yield s""";
         F_${name(f)} : ${mapType(f.getType)} := ${defaultValue(f)}"""
          ).mkString
        }) return ${mapType(t)};

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
         Name : String_Access)
         return Skill.Field_Declarations.Field_Declaration;

      overriding
      procedure Resize_Pool
        (This       : access Pool_T;
         Targets    : Type_Vector;
         Self_Index : Natural);

      overriding function Static_Size (This : access Pool_T) return Natural;

      -- applies F for each element in this
--        procedure Foreach
--          (This : access Pool_T;
--           F    : access procedure (I : $Name));

      package Sub_Pools is new Sub
        (T    => ${Type}_T,
         P    => $Type,
         To_P => ${PackagePrefix}.To_$Name);

      function Make_Sub_Pool
        (This : access Pool_T;
         ID   : Natural;
         Name : String_Access) return Skill.Types.Pools.Pool is
        (Sub_Pools.Make (This.To_Pool, ID, Name));

   private

      type Static_Data_Array_T is array (Positive range <>) of aliased ${Type}_T;
      type Static_Data_Array is access Static_Data_Array_T;

      package A1 is new Vectors (Natural, $Type);
      subtype New_Instance_Vector is A1.Vector;

      package A2 is new Vectors (Natural, Static_Data_Array);
      subtype Static_Instance_Vector is A2.Vector;

      type Pool_T is new Base_Pool_T with record
         Static_Data : Static_Instance_Vector;
         New_Objects : New_Instance_Vector;
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
with Skill.Internal.Parts;
with Skill.Streams;
with Skill.String_Pools;

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

      procedure Make
        (This  : access Pool_T${
          (
            for (f ← t.getAllFields)
              yield s""";
         F_${name(f)} : ${mapType(f.getType)} := ${defaultValue(f)}"""
          ).mkString
        }) is
         R : ${mapType(t)} := new ${mapType(t)}_T;
      begin
         R.Skill_ID := -1;${
          (
            for (f ← t.getAllFields)
              yield s"""
         R.Set_${name(f)} (F_${name(f)});"""
          ).mkString
        }
         This.New_Objects.Append (R);
      end Make;
      function Make
        (This  : access Pool_T${
          (
            for (f ← t.getAllFields)
              yield s""";
         F_${name(f)} : ${mapType(f.getType)} := ${defaultValue(f)}"""
          ).mkString
        }) return ${mapType(t)} is
         R : ${mapType(t)} := new ${mapType(t)}_T;
      begin
         R.Skill_ID := -1;${
          (
            for (f ← t.getAllFields)
              yield s"""
         R.Set_${name(f)} (F_${name(f)});"""
          ).mkString
        }
         This.New_Objects.Append (R);
         return R;
      end Make;

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
              Static_Data => A2.Empty_Vector,
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

         procedure Delete_SA (This : Static_Data_Array) is
            type P is access all Static_Data_Array_T;
            D : P := P (This);

            procedure Free is new Ada.Unchecked_Deallocation
              (Static_Data_Array_T,
               P);
         begin
            Free (D);
         end Delete_SA;

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
         if 0 /= Data'Length then
            Delete (Data);
         end if;

         This.Sub_Pools.Free;
         This.Data_Fields_F.Foreach (Delete'Access);
         This.Data_Fields_F.Free;
         This.Blocks.Free;
         This.Static_Data.Foreach (Delete_SA'Access);
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

      procedure Resize_Pool
        (This       : access Pool_T;
         Targets    : Type_Vector;
         Self_Index : Natural)
      is
         Size : Natural;
         ID   : Skill_ID_T := 1 + Skill_ID_T (This.Blocks.Last_Element.BPO);

         SD : Static_Data_Array;
         R  : $Type;

         use Interfaces;
      begin
         This.Resize_Data;

         if Self_Index = Targets.Length - 1
           or else Targets.Element (Self_Index + 1).Super /= This.To_Pool
         then
            Size := Natural (This.Blocks.Last_Element.Count);
         else
            Size :=
              Natural
                (Targets.Element (Self_Index + 1).Blocks.Last_Element.BPO -
                 This.Blocks.Last_Element.BPO);
         end if;

         SD := new Static_Data_Array_T (1 .. Size);
         This.Static_Data.Append (SD);

         -- set skill IDs and insert into data
         for I in SD'Range loop
            R              := SD (I).Unchecked_Access;
            R.Skill_ID     := ID;
            This.Data (ID) := R.To_Annotation;
            ID             := ID + 1;
         end loop;
      end Resize_Pool;

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
