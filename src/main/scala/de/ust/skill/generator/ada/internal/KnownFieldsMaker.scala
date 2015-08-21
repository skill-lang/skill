/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import de.ust.skill.ir._
import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._

trait KnownFieldsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (
      t ← IR.par;
      f ← t.getFields.par
    ) {
      makeSpec(t, f)
      makeBody(t, f)
    }
  }

  private final def makeSpec(t : UserType, f : Field) {

    val out = open(s"""${packagePrefix}-known_field_${escaped(t.getName.ada).toLowerCase}_${escaped(f.getName.ada).toLowerCase}.ads""")

    val fn = s"${escaped(t.getName.ada)}_${escaped(f.getName.ada)}"

    out.write(s"""
with Skill.Files;
with Skill.Field_Declarations;
with Skill.Field_Types;
with Skill.Streams.Writer;

limited with ${poolsPackage};

package ${PackagePrefix}.Known_Field_$fn is

   type Known_Field_${fn}_T is
     new Skill.Field_Declarations.Field_Declaration_T with private;
   type Known_Field_$fn is access Known_Field_${fn}_T'Class;

   function Make
     (ID    : Natural;
      T     : Skill.Field_Types.Field_Type;
      Owner : Skill.Field_Declarations.Owner_T)
      return Skill.Field_Declarations.Field_Declaration;

   overriding
   procedure Free (This : access Known_Field_${fn}_T);

   function Owner_Dyn
     (This : access Known_Field_${fn}_T)
      return ${poolsPackage}.${name(t)}_P.Pool;

   overriding
   procedure Read
     (This : access Known_Field_${fn}_T;
      CE   : Skill.Field_Declarations.Chunk_Entry);

   overriding
   procedure Offset (This : access Known_Field_${fn}_T);

   overriding
   procedure Write
     (This   : access Known_Field_${fn}_T;
      Output : Skill.Streams.Writer.Sub_Stream);

private

   type Known_Field_${fn}_T is new Skill.Field_Declarations
     .Field_Declaration_T with
   record
      null;
   end record;

end ${PackagePrefix}.Known_Field_$fn;
""")

    out.close()
  }

  private final def makeBody(t : UserType, f : Field) {

    val tIsBaseType = t.getSuperType == null

    // casting access to data array using index i
    val dataAccessI = s"$PackagePrefix.To_${name(t)} (Data (I))"
    val fieldAccessI = s"$PackagePrefix.To_${name(t)} (Data (I)).Get_${name(f)}"

    val out = open(s"""${packagePrefix}-known_field_${escaped(t.getName.ada).toLowerCase}_${escaped(f.getName.ada).toLowerCase}.adb""")

    val fn = s"${escaped(t.getName.ada)}_${escaped(f.getName.ada)}"

    out.write(s"""
with Ada.Unchecked_Conversion;
with Ada.Unchecked_Deallocation;

with Skill.Files;
with Skill.Field_Declarations;
with Skill.Field_Types;
with Skill.Internal.Parts;
with Skill.Streams.Reader;
with Skill.String_Pools;
with Skill.Types;
with $poolsPackage;

with ${PackagePrefix}.Internal_Skill_Names;

package body ${PackagePrefix}.Known_Field_$fn is

   function Make
     (ID    : Natural;
      T     : Skill.Field_Types.Field_Type;
      Owner : Skill.Field_Declarations.Owner_T)
      return Skill.Field_Declarations.Field_Declaration
   is
   begin
      return new Known_Field_${fn}_T'
          (Data_Chunks   => Skill.Field_Declarations.Chunk_List_P.Empty_Vector,
           T             => T,
           Name          => ${internalSkillName(f)},
           Index         => ID,
           Owner         => Owner,
           Future_Offset => 0);
   end Make;

   procedure Free (This : access Known_Field_${fn}_T) is
      type P is access all Known_Field_${fn}_T;

      procedure Delete is new Ada.Unchecked_Deallocation
        (Known_Field_${fn}_T,
         P);
      D : P := P (This);
   begin
      This.Data_Chunks.Foreach (Skill.Field_Declarations.Delete_Chunk'Access);
      This.Data_Chunks.Free;
      Delete (D);
   end Free;

   function Owner_Dyn
     (This : access Known_Field_${fn}_T)
      return ${poolsPackage}.${name(t)}_P.Pool
   is
      function Cast is new Ada.Unchecked_Conversion
        (Skill.Field_Declarations.Owner_T,
         ${poolsPackage}.${name(t)}_P.Pool);
   begin
      return Cast (This.Owner);
   end Owner_Dyn;

   procedure Read
     (This : access Known_Field_${fn}_T;
      CE   : Skill.Field_Declarations.Chunk_Entry)
   is
      First : Natural;
      Last  : Natural;
      Data  : Skill.Types.Annotation_Array    := Owner_Dyn (This)${if(tIsBaseType)""else".Base"}.Data;
      Input : Skill.Streams.Reader.Sub_Stream := CE.Input;
   begin
      if CE.C.all in Skill.Internal.Parts.Simple_Chunk then
         First := Natural (CE.C.To_Simple.BPO);
         Last  := First + Natural (CE.C.Count);
      else
         First := Natural (This.Owner.Blocks.Last_Element.BPO);
         Last  := First + Natural (This.Owner.Blocks.Last_Element.Count);
         -- TODO This is horribly incorrect!!!
      end if;
${
  f.getType.getSkillName match {
    case "string" ⇒ s"""
      declare
         Strings : Skill.String_Pools.Pool := Skill.Field_Types.Builtin.String_Type_T.Field_Type(This.T).Strings;
      begin
         for I in First + 1 .. Last loop
            To_${name(t)} (Data (I)).Set_${name(f)} (Strings.Get(Input.V64));
         end loop;
      end;"""
    case _ ⇒ s"""
      for I in First + 1 .. Last loop
         To_${name(t)} (Data (I)).Set_${name(f)} (${read(f)});
      end loop;"""
  }
}
   end Read;

   procedure Offset (This : access Known_Field_${fn}_T) is${
      if (f.isConstant())
        """
   begin
        return; -- this field is constant"""
      else """
      use type Skill.Types.v64;
      use type Skill.Types.Uv64;

      function Cast is new Ada.Unchecked_Conversion
        (Skill.Types.v64,
         Skill.Types.Uv64);

      Rang   : Skill.Internal.Parts.Block   := This.Owner.Blocks.Last_Element;
      Data   : Skill.Types.Annotation_Array := This.Owner.Base.Data;
      Result : Skill.Types.v64              := 0;
      Low    : constant Natural             := Natural (Rang.BPO);
      High   : constant Natural             := Natural (Rang.BPO + Rang.Count);
   begin"""+{
        // this prelude is common to most cases
        val preludeData : String = """
      for I in Low + 1 .. High loop"""

        f.getType match {

          // read next element
          case fieldType : GroundType ⇒ fieldType.getSkillName match {

            //            case "annotation" ⇒ s"""
            //        final Annotation t = (Annotation) type;
            //        $preludeData
            //            SkillObject v = (${if (tIsBaseType) "" else s"(${mapType(t)})"}data[i]).get${escaped(f.getName.capital)};
            //            if(null==v)
            //                result++;
            //            else
            //                result += t.singleOffset(v);
            //        }
            //        return result;"""
            //
            case "string" ⇒ s"""
      declare
         use type Skill.Types.String_Access;

         S   : Skill.Types.String_Access;
         V   : Skill.Types.Uv64;
         Ids : Skill.Field_Types.Builtin.String_Type_T.ID_Map :=
           Skill.Field_Types.Builtin.String_Type_T.Field_Type
             (This.T).Get_Id_Map;

         use type Ada.Containers.Count_Type;
      begin
         if Ids.Length < 255 then
            This.Future_Offset := Skill.Types.v64 (High - Low);
            return;
         end if;

         for I in Low + 1 .. High loop
            S := $dataAccessI.Get_${name(f)};
            if null = S then
               Result := Result + 1;
            else
               V := Cast (Skill.Types.v64 (Ids.Element (S)));

               if 0 = (V and 16#FFFFFFFFFFFFFF80#) then
                  Result := Result + 1;
               elsif 0 = (V and 16#FFFFFFFFFFFFC000#) then
                  Result := Result + 2;
               elsif 0 = (V and 16#FFFFFFFFFFE00000#) then
                  Result := Result + 3;
               elsif 0 = (V and 16#FFFFFFFFF0000000#) then
                  Result := Result + 4;
               elsif 0 = (V and 16#FFFFFFF800000000#) then
                  Result := Result + 5;
               elsif 0 = (V and 16#FFFFFC0000000000#) then
                  Result := Result + 6;
               elsif 0 = (V and 16#FFFE000000000000#) then
                  Result := Result + 7;
               elsif 0 = (V and 16#FF00000000000000#) then
                  Result := Result + 8;
               else
                  Result := Result + 9;
               end if;
            end if;
         end loop;
      end;
      This.Future_Offset := Result;"""

            case "i8" | "bool" ⇒ s"""
        This.Future_Offset := rang.count;"""

            case "i16" ⇒ s"""
        This.Future_Offset := 2 * rang.count;"""

            case "i32" | "f32" ⇒ s"""
        This.Future_Offset := 4 * rang.count;"""

            case "i64" | "f64" ⇒ s"""
        This.Future_Offset := 8 * rang.count;"""

            case "v64" ⇒ s"""$preludeData
         declare
            v : constant Skill.Types.Uv64 :=
              Cast ($dataAccessI.Get_${name(f)});
         begin
            if 0 = (v and 16#FFFFFFFFFFFFFF80#) then
               Result := Result + 1;
            elsif 0 = (v and 16#FFFFFFFFFFFFC000#) then
               Result := Result + 2;
            elsif 0 = (v and 16#FFFFFFFFFFE00000#) then
               Result := Result + 3;
            elsif 0 = (v and 16#FFFFFFFFF0000000#) then
               Result := Result + 4;
            elsif 0 = (v and 16#FFFFFFF800000000#) then
               Result := Result + 5;
            elsif 0 = (v and 16#FFFFFC0000000000#) then
               Result := Result + 6;
            elsif 0 = (v and 16#FFFE000000000000#) then
               Result := Result + 7;
            elsif 0 = (v and 16#FF00000000000000#) then
               Result := Result + 8;
            else
               Result := Result + 9;
            end if;
         end;
      end loop;
      This.Future_Offset := Result;"""

            case _ ⇒ s"""
        raise Constraint_Error with "TODO";"""
          }

          //          case fieldType : ConstantLengthArrayType ⇒ s"""
          //        final SingleArgumentType t = (SingleArgumentType) type;
          //        final FieldType baseType = t.groundType;
          //        $preludeData
          //            final ${mapType(f.getType)} v = (${if (tIsBaseType) "" else s"(${mapType(t)})"}data[i]).get${escaped(f.getName.capital)}();
          //            assert null==v;
          //            result += baseType.calculateOffset(v);
          //        }
          //        return result;"""
          //
          //          case fieldType : SingleBaseTypeContainer ⇒ s"""
          //        final SingleArgumentType t = (SingleArgumentType) type;
          //        final FieldType baseType = t.groundType;
          //        $preludeData
          //            final ${mapType(f.getType)} v = (${if (tIsBaseType) "" else s"(${mapType(t)})"}data[i]).get${escaped(f.getName.capital)}();
          //            if(null==v)
          //                result++;
          //            else {
          //                result += V64.singleV64Offset(v.size());
          //                result += baseType.calculateOffset(v);
          //            }
          //        }
          //        return result;"""
          //
          //          case fieldType : MapType ⇒ s"""
          //        final MapType t = (MapType) type;
          //        final FieldType keyType = t.keyType;
          //        final FieldType valueType = t.valueType;
          //        $preludeData
          //            final ${mapType(f.getType)} v = (${if (tIsBaseType) "" else s"(${mapType(t)})"}data[i]).get${escaped(f.getName.capital)}();
          //            if(null==v)
          //                result++;
          //            else {
          //                result += V64.singleV64Offset(v.size());
          //                result += keyType.calculateOffset(v.keySet());
          //                result += valueType.calculateOffset(v.values());
          //            }
          //        }
          //        return result;"""

          case fieldType : UserType ⇒ s"""$preludeData
        declare
            Instance : ${mapType(f.getType)} := $dataAccessI.Get_${name(f)};
            V : Skill.Types.Uv64;
         begin
            if null = Instance Then
               Result := Result + 1;
            else
               V := Cast (Skill.Types.V64(Instance.Skill_ID));
               if 0 = (V and 16#FFFFFFFFFFFFFF80#) then
                  Result := Result + 1;
               elsif 0 = (V and 16#FFFFFFFFFFFFC000#) then
                  Result := Result + 2;
               elsif 0 = (V and 16#FFFFFFFFFFE00000#) then
                  Result := Result + 3;
               elsif 0 = (V and 16#FFFFFFFFF0000000#) then
                  Result := Result + 4;
               elsif 0 = (V and 16#FFFFFFF800000000#) then
                  Result := Result + 5;
               elsif 0 = (V and 16#FFFFFC0000000000#) then
                  Result := Result + 6;
               elsif 0 = (V and 16#FFFE000000000000#) then
                  Result := Result + 7;
               elsif 0 = (V and 16#FF00000000000000#) then
                  Result := Result + 8;
               else
                  Result := Result + 9;
               end if;
            end if;
         end;
      end loop;
      This.Future_Offset := Result;"""
          case _ ⇒ s"""
        raise constraint_error with "TODO";"""
        }
      }
    }
   end Offset;

   procedure Write
     (This   : access Known_Field_${fn}_T;
      Output : Skill.Streams.Writer.Sub_Stream) is${
      if (f.isConstant())
        """ null; -- this field is constant"""
      else
        s"""
      use type Skill.Types.v64;
      use type Skill.Types.Uv64;

      function Cast is new Ada.Unchecked_Conversion
        (Skill.Types.v64,
         Skill.Types.Uv64);

      Data : Skill.Types.Annotation_Array := This.Owner.Base.Data;
      C    : Skill.Internal.Parts.Chunk   := This.Data_Chunks.Last_Element.C;
      Low  : Natural;
      High : Natural;
   begin
      if C.all in Skill.Internal.Parts.Simple_Chunk then
         Low := Natural(Skill.Internal.Parts.Simple_Chunk(C.all).BPO);
         High := Low + Natural(C.Count);
      else${
          // we have to use the offset of the pool
          if (tIsBaseType) """
         Low := 1;
         High := This.Owner.Size;"""
          else """
         if This.Owner.Size > 0 then
            -- TODO not correct for appends !!
            Low := Natural (This.Owner.Blocks.Last_Element.BPO) + 1;
         else
            Low := 1;
         end if;
         High := Low + This.Owner.Size - 1;"""
        }
      end if;

      for I in Low .. High loop
         ${
          // read next element
          f.getType match {
            case t : GroundType ⇒ t.getSkillName match {
              case "annotation" ⇒ s"""raise Constraint_Error with "todo: write annotation";"""
              case "string" ⇒ s"""Skill.Field_Types.Builtin.String_Type_T.Field_Type
             (This.T).Write_Single_Field($fieldAccessI, output);"""
              case _ ⇒ s"""Output.${t.getSkillName.capitalize}($fieldAccessI);"""
            }

            case t : UserType ⇒ s"""declare
            Instance : ${mapType(f.getType)} := $dataAccessI.Get_${name(f)};
         begin
            if null = Instance Then
               Output.I8 (0);
            else
               Output.V64(Skill.Types.V64(Instance.Skill_ID));
            end if;
         end;"""
            case _ ⇒ s"""raise Constraint_Error with "todo";"""
          }
        }
      end loop;
   end Write;"""
    }

end ${PackagePrefix}.Known_Field_$fn;
""")

    out.close()
  }

  private def read(f : Field) : String = f.getType match {
    case t : UserType ⇒ "null"
    case t : GroundType ⇒ t.getName.ada match {
      case "Annotation" ⇒ "null"
      case "String"     ⇒ "null"
      case n            ⇒ "Input."+n
    }
    case _ ⇒ "null"
  }
}
