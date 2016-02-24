/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.api.internal

import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.ListType
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Type
import de.ust.skill.ir.MapType
import de.ust.skill.ir.UserType
import de.ust.skill.ir.ContainerType
import de.ust.skill.ir.SingleBaseTypeContainer
import scala.collection.mutable.HashSet
import scala.annotation.tailrec

trait PoolsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    makePackage

    for(t <- IR){
      makeSpec(t)
      makeBody(t)
    }
  }

  private final def makePackage {
    val out = open(s"""skill-types-pools-${packagePrefix.replace('-', '_')}_pools.ads""")

    out.write(s"""
package Skill.Types.Pools.${PackagePrefix.replace('.', '_')}_Pools is

end Skill.Types.Pools.${PackagePrefix.replace('.', '_')}_Pools;
""")
    out.close
  }

  private final def makeSpec(t : UserType) {
        val isBase = null == t.getSuperType
        val Name = name(t)
        val Type = s"Standard.$PackagePrefix.$Name"

    val out = open(s"""skill-types-pools-${packagePrefix.replace('-', '_')}_pools-${Name.toLowerCase}_p.ads""")

    out.write(s"""
with Ada.Unchecked_Conversion;

with Skill.Books;
with Skill.Containers.Vectors;
with Skill.Containers.Arrays;
with Skill.Field_Declarations;
with Skill.Field_Types.Builtin;
with Skill.Field_Types.Builtin.String_Type_P;
with Skill.Streams.Reader;
with Skill.Streams.Writer;
with Skill.Types;
with Skill.Types.Pools;
with Skill.Types.Pools.Sub;

with $PackagePrefix;

-- instantiated pool packages
package Skill.Types.Pools.${PackagePrefix.replace('.', '_')}_Pools.${Name}_P is
   pragma Warnings (Off);

   type Pool_T is new ${if(isBase)"Base"else"Sub"}_Pool_T with private;
   type Pool is access Pool_T;

   -- API methods
   function Get (This : access Pool_T; ID : Skill_ID_T) return $Type;
   pragma Inline (Get);

   -- constructor for instances
   procedure Make
     (This  : access Pool_T${
       (
         for (f ← t.getAllFields)
           yield s""";
      F_${name(f)} : ${mapType(f)} := ${defaultValue(f)}"""
       ).mkString
     });
   function Make
     (This  : access Pool_T${
       (
         for (f ← t.getAllFields)
           yield s""";
      F_${name(f)} : ${mapType(f)} := ${defaultValue(f)}"""
       ).mkString
     }) return ${mapType(t)};

   -- create a boxed instance with default values;
   -- only useful in reflective applications
   overriding
   function Make_Boxed_Instance (This : access Pool_T) return Box;

   ----------------------
   -- internal methods --
   ----------------------

   -- constructor invoked by new_pool
   function Make_Pool (Type_Id : Natural${
       if (isBase) ""
       else "; Super : Skill.Types.Pools.Pool"
     }) return Pools.Pool;
   -- destructor invoked by close
   procedure Free (This : access Pool_T);

   overriding
   function Add_Field
     (This : access Pool_T;
      ID   : Natural;
      T    : Field_Types.Field_Type;
      Name : String_Access)
      return Skill.Field_Declarations.Field_Declaration;

   procedure Add_Known_Field
     (This : access Pool_T;
      Name : String_Access;
      String_Type : Field_Types.Builtin.String_Type_P.Field_Type;
      Annotation_Type : Field_Types.Builtin.Annotation_Type_P.Field_Type);

   overriding
   procedure Resize_Pool (This : access Pool_T);

   package Sub_Pools is new Sub
     (T    => ${Type}_T,
      P    => $Type,
      To_P => Standard.${PackagePrefix}.To_$Name);

   overriding
   function Make_Sub_Pool
     (This : access Pool_T;
      ID   : Natural;
      Name : String_Access) return Skill.Types.Pools.Pool is
     (Sub_Pools.Make (This.To_Pool, ID, Name));

   -- RTTI implementation
   function Boxed is new Ada.Unchecked_Conversion
     ($Type, Types.Box);
   function Unboxed is new Ada.Unchecked_Conversion
     (Types.Box, $Type);

   function Read_Box
     (This : access Pool_T;
      Input : Skill.Streams.Reader.Sub_Stream) return Types.Box is
     (Boxed (This.Get(Skill_ID_T(Input.V64))));

   function Offset_Box
     (This : access Pool_T;
      Target : Types.Box) return Types.V64;

   procedure Write_Box
     (This : access Pool_T;
      Output : Streams.Writer.Sub_Stream; Target : Types.Box);

   function Content_Tag (This : access Pool_T) return Ada.Tags.Tag is
     (${Type}_T'Tag);

private
   
   package Book_P is new Skill.Books(${Type}_T, $Type);

   type Auto_Field_Array is
     array (0 .. ${t.getFields.filter { _.isAuto() }.size - 1}) of Skill.Field_Declarations.Field_Declaration;

   type Pool_T is new ${if(isBase)"Base"else"Sub"}_Pool_T with record
      -- memory management
      Book : aliased Book_P.Book;
      
      -- the list of all auto fields
      Auto_Fields : Auto_Field_Array;
   end record;

end Skill.Types.Pools.${PackagePrefix.replace('.', '_')}_Pools.${Name}_P;
""")

 out.close()
  }

  private final def makeBody(t : UserType) {
        val isBase = null == t.getSuperType
        val Name = name(t)
        val Type = s"Standard.$PackagePrefix.$Name"

    val out = open(s"""skill-types-pools-${packagePrefix.replace('-', '_')}_pools-${Name.toLowerCase}_p.adb""")

    out.write(s"""
with Ada.Unchecked_Conversion;
with Ada.Unchecked_Deallocation;

with Skill.Equals;
with Skill.Errors;
with Skill.Field_Declarations;
with Skill.Field_Types;
with Skill.Internal.Parts;
with Skill.Streams;

with $PackagePrefix.Internal_Skill_Names;
with $PackagePrefix.Known_Field_${name(t)};

-- instantiated pool packages
package body Skill.Types.Pools.${PackagePrefix.replace('.', '_')}_Pools.${Name}_P is

   use type $Type;

   -- API methods
   function Get
     (This : access Pool_T;
      ID   : Skill_ID_T) return $Type
   is
   begin
      if 0 = ID then
         return null;
      else
         return Standard.${PackagePrefix}.To_$Name (This${if(isBase)""else".Base"}.Data (ID));
      end if;
   end Get;

   procedure Make
     (This  : access Pool_T${
       (
         for (f ← t.getAllFields)
           yield s""";
      F_${name(f)} : ${mapType(f)} := ${defaultValue(f)}"""
       ).mkString
     }) is
      R : constant ${mapType(t)} := This.Book.Next;
   begin
      R.Skill_ID := -1;${
       (
         for (f ← t.getAllFields)
           yield s"""
      R.Set_${name(f)} (F_${name(f)});"""
       ).mkString
     }
      This.New_Objects.Append (R.To_Annotation);
   end Make;
   function Make
     (This  : access Pool_T${
       (
         for (f ← t.getAllFields)
           yield s""";
      F_${name(f)} : ${mapType(f)} := ${defaultValue(f)}"""
       ).mkString
     }) return ${mapType(t)} is
      R : constant ${mapType(t)} := This.Book.Next;
   begin
      R.Skill_ID := -1;${
       (
         for (f ← t.getAllFields)
           yield s"""
      R.Set_${name(f)} (F_${name(f)});"""
       ).mkString
     }
      This.New_Objects.Append (R.To_Annotation);
      return R;
   end Make;

   overriding
   function Make_Boxed_Instance (This : access Pool_T) return Box is
   begin
      return Boxed (This.Make);
   end;

   ----------------------
   -- internal methods --
   ----------------------

   -- constructor invoked by new_pool
   function Make_Pool (Type_Id : Natural${
       if (null == t.getSuperType) ""
       else "; Super : Skill.Types.Pools.Pool"
     }) return Skill.Types.Pools.Pool is
      function Convert is new Ada.Unchecked_Conversion
        (Source => Pool,
         Target => Skill.Types.Pools.${if(null == t.getSuperType)"Base"else "Sub"}_Pool);
      function Convert is new Ada.Unchecked_Conversion
        (Source => Pool,
         Target => Skill.Types.Pools.Pool);

      This : Pool;
   begin
      This :=
        new Pool_T'
          (Name          => ${internalSkillName(t)},
           Type_Id       => Type_Id,${
       if (null == t.getSuperType) """
           Super         => null,
           Base          => null,
           Data          => Skill.Types.Pools.Empty_Data,
           Owner         => null,"""
       else """
           Super         => Super,
           Base          => Super.Base,"""
     }
           Sub_Pools     => Sub_Pool_Vector_P.Empty_Vector,
           Next     => null, -- can only be calculated after all types are known
           Super_Type_Count => ${superTypeCount(t)},
           Data_Fields_F =>
             Skill.Field_Declarations.Field_Vector_P.Empty_Vector,
           Auto_Fields => (others => null),
           Known_Fields => ${
       if (t.getFields.isEmpty())
         "No_Known_Fields"
       else {
         (for ((f, i) ← t.getFields.zipWithIndex)
           yield s"                 ${i + 1} => ${internalSkillName(f)}\n"
         ).mkString("new String_Access_Array'((\n", ",", "                ))")
       }
     },
           Blocks      => Skill.Internal.Parts.Blocks_P.Empty_Vector,
           Fixed       => False,
           Cached_Size => 0,
           Book        => <>,
           Static_Data_Instances => 0,
           New_Objects => New_Objects_P.Empty_Vector);
${
       if (null == t.getSuperType) """
      This.Base := Convert (This);"""
       else """
      This.Super.Sub_Pools.Append (Convert (This));"""
     }
      return Convert (This);
   exception
      when E : others =>
         Skill.Errors.Print_Stacktrace (E);
         Skill.Errors.Print_Stacktrace;
         raise Skill.Errors.Skill_Error with "$Name pool allocation failed";
   end Make_Pool;

   procedure Free (This : access Pool_T) is

      procedure Delete
        (This : Skill.Field_Declarations.Field_Declaration)
      is
      begin
         This.Free;
      end Delete;
${
           if(isBase)"""
      Data : Annotation_Array := This.Data;"""
           else ""
         }
      procedure Delete is new Ada.Unchecked_Deallocation
        (Skill.Types.Skill_Object,
         Skill.Types.Annotation);
      procedure Delete is new Ada.Unchecked_Deallocation
        (Skill.Types.Annotation_Array_T,
         Skill.Types.Annotation_Array);
      procedure Delete is new Ada.Unchecked_Deallocation
        (Skill.Types.String_Access_Array,
         Skill.Types.String_Access_Array_Access);

      type P is access all Pool_T;
      procedure Delete is new Ada.Unchecked_Deallocation (Pool_T, P);
      D : P := P (This);
   begin${
     if(isBase)"""
      if 0 /= Data'Length then
         Delete (Data);
      end if;"""
     else ""
   }
      This.Sub_Pools.Free;
      This.Data_Fields_F.Foreach (Delete'Access);
      This.Data_Fields_F.Free;
      This.Blocks.Free;
      This.Book.Free;
      This.New_Objects.Free;
      if No_Known_Fields /= This.Known_Fields then
         Delete(This.Known_Fields);
      end if;
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

      type Super is access all Pools.${if(isBase)"Base"else"Sub"}_Pool_T;
   begin
${
       t.getFields.filterNot { _.isAuto }.foldRight("""
      return Super (This).Add_Field (ID, T, Name);""") {
         case (f, s) ⇒ s"""
      if Skill.Equals.Equals
          (Name,
           ${internalSkillName(f)})
      then
         F := Standard.${PackagePrefix}.Known_Field_${name(t)}.Make_${name(f)} (ID, T, Convert (P (This)));
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

   procedure Add_Known_Field
     (This        : access Pool_T;
      Name        : String_Access;
      String_Type : Field_Types.Builtin.String_Type_P.Field_Type;
      Annotation_Type : Field_Types.Builtin.Annotation_Type_P.Field_Type)
   is
      use Interfaces;

      type P is access all Pool_T;
      function Convert is new Ada.Unchecked_Conversion
        (P, Field_Declarations.Owner_T);

      F : Field_Declarations.Field_Declaration;
   begin${
       (for (f ← t.getFields if !f.isAuto)
         yield s"""
      if Skill.Equals.Equals
          (${internalSkillName(f)},
           Name)
      then
         F :=
           This.Add_Field
           (ID => 1 + This.Data_Fields_F.Length,
            T => ${mapToFieldType(f, isBase)},
            Name => Name);
         return;
      end if;"""
       ).mkString
     }${
       var index = 0;
       (for (f ← t.getFields if f.isAuto)
         yield s"""
      if Skill.Equals.Equals
          (${internalSkillName(f)},
           Name)
      then
         F := Standard.$PackagePrefix.Known_Field_${name(t)}.Make_${name(f)}
           (T     => ${mapToFieldType(f, isBase)},
            Owner => Convert (P (This)));
         This.Auto_Fields(${index += 1; index - 1}) := F;
         return;
      end if;"""
       ).mkString
     }
      raise Constraint_Error
        with "generator broken in pool::add_known_field: unexpeted add for:" & Name.all;
   end Add_Known_Field;

   -- @note: this might not be correct in general (first/last index calculation)
   procedure Resize_Pool (This : access Pool_T) is
      ID   : Skill_ID_T := 1 + Skill_ID_T (This.Blocks.Last_Element.BPO);
      Size : Skill_ID_T := This.Blocks.Last_Element.Static_Count;

      Data : Skill.Types.Annotation_Array;

      SD : Book_P.Page;

      use Interfaces;
   begin${
       if (null == t.getSuperType) """
      This.Resize_Data;"""
       else ""
     }
      Data := This${if(isBase)""else".Base"}.Data;

      This.Static_Data_Instances := This.Static_Data_Instances + Size;

      if 0 = Size then
         return;
      end if;

      SD := This.Book.Make_Page(Size);

      -- set skill IDs and insert into data
      for R of SD.all loop
         R.Skill_ID := ID;
         Data (ID)  := R.To_Annotation;
         ID         := ID + 1;
      end loop;
   end Resize_Pool;

   function Offset_Box
     (This : access Pool_T;
      Target : Types.Box) return Types.V64 is
   begin
      if null = Unboxed(Target) then
         return 0;
      else
         return Field_Types.Builtin.Offset_Single_V64(Types.V64(Unboxed(Target).Skill_ID));
      end if;
   end Offset_Box;

   procedure Write_Box
     (This : access Pool_T;
      Output : Streams.Writer.Sub_Stream; Target : Types.Box) is
   begin
      if null = Unboxed(Target) then
         Output.I8(0);
      else 
         Output.V64(Types.V64(Unboxed(Target).Skill_Id));
      end if;
   end Write_Box;

end Skill.Types.Pools.${PackagePrefix.replace('.', '_')}_Pools.${Name}_P;
""")

    out.close()
  }
  
  @tailrec
  private final def superTypeCount(t : UserType, count : Int = 0) : Int = 
    if(t.getSuperType!=null) superTypeCount(t.getSuperType, count + 1)
    else count

   private final def mapToFieldType(f : Field, isBase : Boolean) : String = {
    //@note temporary string & annotation will be replaced later on
    @inline def mapGroundType(t : Type) : String = t.getSkillName match {
      case "annotation" ⇒ "Field_Types.Field_Type(Annotation_Type)"
      case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
        if (f.isConstant) s"Field_Types.Builtin.Const_${t.getName.capital}(${mapConstantValue(f)})"
        else s"Field_Types.Builtin.${t.getName.capital}"
      case "string" ⇒ "Field_Types.Field_Type(String_Type)"

      case s        ⇒ s"Field_Types.Field_Type(This${if(isBase)""else".Base"}.Owner.Types_By_Name.Element(${internalSkillName(t)}))"
    }

    f.getType match {
      case t : GroundType  ⇒ mapGroundType(t)
            case t : ConstantLengthArrayType ⇒ s"Field_Types.Builtin.Const_Array(${t.getLength}, ${mapGroundType(t.getBaseType)})"
            case t : VariableLengthArrayType ⇒ s"Field_Types.Builtin.Var_Array(${mapGroundType(t.getBaseType)})"
            case t : ListType                ⇒ s"Field_Types.Builtin.List_Type(${mapGroundType(t.getBaseType)})"
            case t : SetType                 ⇒ s"Field_Types.Builtin.Set_Type(${mapGroundType(t.getBaseType)})"
            case t : MapType                 ⇒ t.getBaseTypes().map(mapGroundType).reduceRight((k, v) ⇒ s"Field_Types.Builtin.Map_Type($k, $v)")
      case t : Declaration ⇒ s"Field_Types.Field_Type(This${if(isBase)""else".Base"}.Owner.Types_By_Name.Element(${internalSkillName(t)}))"
    }
  }
}
