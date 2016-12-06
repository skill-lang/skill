/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import de.ust.skill.ir._
import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._

trait KnownFieldsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← IR) {
      makeSpec(t)
      makeBody(t)
    }
  }

  private final def makeSpec(t : UserType) {

    val out = open(s"""${packagePrefix}-known_field_${name(t).toLowerCase}.ads""")
    
    val thisPackage = s"${PackagePrefix}.Known_Field_${name(t)}"

    out.write(s"""
with Skill.Field_Declarations;
with Skill.Field_Restrictions;
with Skill.Field_Types;
with Skill.Streams.Writer;

limited with ${poolsPackage}.${name(t)}_P;

package $thisPackage is
""")

    for(f <- t.getFields){
      val fn = fieldName(t, f)
      out.write(s"""

   type Known_Field_${fn}_T is
     new Skill.Field_Declarations.Field_Declaration_T with null record;
   type Known_Field_$fn is access Known_Field_${fn}_T'Class;

   function Make_${name(f)}
     (${
      if(f.isAuto())""
      else"""ID    : Natural;
      """
}T     : Skill.Field_Types.Field_Type;
      Owner : Skill.Field_Declarations.Owner_T;
      Restrictions : Skill.Field_Restrictions.Vector)
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
""")
}
    out.write(s"""
end $thisPackage;
""")

    out.close()
  }

  private final def makeBody(t : UserType) {
    
    // do not create empty package bodies
    if(t.getFields.isEmpty)
      return;

    val tIsBaseType = t.getSuperType == null
    val thisPackage = s"${PackagePrefix}.Known_Field_${name(t)}"

    val out = open(s"""${packagePrefix}-known_field_${name(t).toLowerCase}.adb""")


    out.write(s"""
with Ada.Unchecked_Conversion;
with Ada.Unchecked_Deallocation;

with Skill.Field_Types.Builtin;
with Skill.Field_Types.Builtin.String_Type_P;
with Skill.Internal.Parts;
with Skill.Streams.Reader;
with Skill.String_Pools;
with Skill.Types;
with $poolsPackage.${name(t)}_P;${
  t.getFields.map(_.getType).collect{
    case t : UserType ⇒ t
  }.map(t ⇒ s"""
with $poolsPackage.${name(t)}_P;""").mkString
}

with ${PackagePrefix}.Internal_Skill_Names;

package body $thisPackage is
   pragma Warnings(Off);
""")
    for(f <- t.getFields) {
    val fn = fieldName(t, f)
    // casting access to data array using index i
    val dataAccessI = s"Standard.$PackagePrefix.To_${name(t)} (Data (I))"
    val fieldAccessI = s"$dataAccessI.Get_${name(f)}"
    
      out.write(s"""
   function Make_${name(f)}
     (${
  if(f.isAuto())""
  else"""ID    : Natural;
      """
}T     : Skill.Field_Types.Field_Type;
      Owner : Skill.Field_Declarations.Owner_T;
      Restrictions : Skill.Field_Restrictions.Vector)
      return Skill.Field_Declarations.Field_Declaration
   is
   begin
      return new Known_Field_${fn}_T'
          (Data_Chunks   => Skill.Field_Declarations.Chunk_List_P.Empty_Vector,
           T             => T,
           Name          => ${internalSkillName(f)},
           Index         => ${if(f.isAuto())"0" else "ID"},
           Owner         => Owner,
           Future_Offset => 0,
           Restrictions  => Restrictions);
   end Make_${name(f)};

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
      Data  : constant Skill.Types.Annotation_Array := Owner_Dyn (This)${if(tIsBaseType)""else".Base"}.Data;
      Input : constant Skill.Streams.Reader.Stream  := CE.Input.To;
   begin
      if CE.C.all in Skill.Internal.Parts.Simple_Chunk then
         First := Natural (CE.C.To_Simple.BPO);
         Last  := First + Natural (CE.C.Count);
${readBlock(t, f)}
      else
         for Block_Index in 1 .. CE.C.To_Bulk.Block_Count loop
            First := Natural (This.Owner.Blocks.Element (Block_Index - 1).BPO);
            Last  := First + Natural (This.Owner.Blocks.Element (Block_Index - 1).Dynamic_Count);
${readBlock(t, f)}
         end loop;
      end if;
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
      pragma Inline_Always (Cast);

      Rang   : constant Skill.Internal.Parts.Block := This.Owner.Blocks.Last_Element;
      Data   : constant Skill.Types.Annotation_Array := This.Owner.Base.Data;
      Result : Skill.Types.v64              := 0;
      Low    : constant Natural             := Natural (Rang.BPO);
      High   : constant Natural             := Natural (Rang.BPO + Rang.Dynamic_Count);
   begin"""+{
        // this prelude is common to most cases
        val preludeData : String = """
      for I in Low + 1 .. High loop"""

        f.getType match {

          // read next element
          case fieldType : GroundType ⇒ fieldType.getSkillName match {

            case "annotation" ⇒ s"""
      declare
         pragma Warnings (Off);
         use type Skill.Types.Annotation;

         function Boxed is new Ada.Unchecked_Conversion (Skill.Types.Annotation, Skill.Types.Box);

         V   : Skill.Types.Annotation;
      begin
         for I in Low + 1 .. High loop
            V := $fieldAccessI;
            if null = V then
               Result := Result + 2;
            else
               Result := Result + This.T.Offset_Box (Boxed (V));
            end if;
         end loop;
      end;
      This.Future_Offset := Result;"""

            case "string" ⇒ s"""
      declare
         use type Skill.Types.String_Access;

         S   : Skill.Types.String_Access;
         V   : Skill.Types.Uv64;
         Ids : Skill.Field_Types.Builtin.String_Type_P.ID_Map :=
           Skill.Field_Types.Builtin.String_Type_P.Field_Type
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
        This.Future_Offset := Skill.Types.v64 (Rang.Dynamic_Count);"""

            case "i16" ⇒ s"""
        This.Future_Offset := 2 * Skill.Types.v64 (Rang.Dynamic_Count);"""

            case "i32" | "f32" ⇒ s"""
        This.Future_Offset := 4 * Skill.Types.v64 (Rang.Dynamic_Count);"""

            case "i64" | "f64" ⇒ s"""
        This.Future_Offset := 8 * Skill.Types.v64 (Rang.Dynamic_Count);"""

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

                    case fieldType : SingleBaseTypeContainer ⇒ s"""
      declare
         pragma Warnings (Off);
         use type ${mapType(f)};

         function Boxed is new Ada.Unchecked_Conversion (${mapType(f)}, Skill.Types.Box);

         V   : ${mapType(f)};
      begin
         for I in Low + 1 .. High loop
            V := $fieldAccessI;
            if null = V then
               Result := Result + 1;
            else
               Result := Result + This.T.Offset_Box (Boxed (V));
            end if;
         end loop;
      end;
      This.Future_Offset := Result;"""

                    case fieldType : MapType ⇒ s"""
      declare
         pragma Warnings (Off);
         use type ${mapType(f)};

         function Boxed is new Ada.Unchecked_Conversion (${mapType(f)}, Skill.Types.Box);

         V   : ${mapType(f)};
      begin
         for I in Low + 1 .. High loop
            V := $fieldAccessI;
            if null = V then
               Result := Result + 1;
            else
               Result := Result + This.T.Offset_Box (Boxed (V));
            end if;
         end loop;
      end;
      This.Future_Offset := Result;"""

          case fieldType : UserType ⇒ s"""$preludeData
        declare
            Instance : constant ${mapType(f.getType)} := $dataAccessI.Get_${name(f)};
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
          case _ ⇒ ???
        }
      }
    }
   end Offset;

   procedure Write
     (This   : access Known_Field_${fn}_T;
      Output : Skill.Streams.Writer.Sub_Stream)
   is${
      if (f.isConstant())
        """ null; -- this field is constant"""
      else
        s"""
      use type Skill.Types.v64;

      function Boxed is new Ada.Unchecked_Conversion (${mapType(f)}, Skill.Types.Box);

      Rang   : constant Skill.Internal.Parts.Block := This.Owner.Blocks.Last_Element;
      Data   : constant Skill.Types.Annotation_Array := This.Owner.Base.Data;
      Low    : constant Natural             := Natural (Rang.BPO);
      High   : constant Natural             := Natural (Rang.BPO + Rang.Dynamic_Count);
   begin

      for I in Low + 1 .. High loop
         ${
          // read next element
          f.getType match {
            case t : GroundType ⇒ t.getSkillName match {
              case "annotation" ⇒ s"""Skill.Field_Types.Builtin.Annotation_Type_P.Field_Type
           (This.T).Write_Box
         (Output, Skill.Field_Types.Builtin.Annotation_Type_P.Boxed
            ($fieldAccessI));"""
              case "string" ⇒ s"""Skill.Field_Types.Builtin.String_Type_P.Field_Type
             (This.T).Write_Single_Field ($fieldAccessI, Output);"""
              case _ ⇒ s"""Output.${t.getSkillName.capitalize} ($fieldAccessI);"""
            }

            case t : UserType ⇒ s"""declare
            Instance : ${mapType(f.getType)} := $dataAccessI.Get_${name(f)};
         begin
            if null = Instance Then
               Output.I8 (0);
            else
               Output.V64 (Skill.Types.V64(Instance.Skill_ID));
            end if;
         end;"""

            case t : ConstantLengthArrayType ⇒ s"""Skill.Field_Types.Builtin.Const_Arrays_P.Field_Type
           (This.T).Write_Box (Output, Boxed ($fieldAccessI));"""

            case t : VariableLengthArrayType ⇒ s"""Skill.Field_Types.Builtin.Var_Arrays_P.Field_Type
           (This.T).Write_Box (Output, Boxed ($fieldAccessI));"""

            case t : ListType ⇒ s"""Skill.Field_Types.Builtin.List_Type_P.Field_Type
           (This.T).Write_Box (Output, Boxed ($fieldAccessI));"""

            case t : SetType ⇒ s"""Skill.Field_Types.Builtin.Set_Type_P.Field_Type
           (This.T).Write_Box
         (Output, Skill.Field_Types.Builtin.Set_Type_P.Boxed
            ($fieldAccessI));"""

            case t : MapType ⇒ s"""Skill.Field_Types.Builtin.Map_Type_P.Field_Type
           (This.T).Write_Box (Output, Boxed ($fieldAccessI));"""
          }
        }
      end loop;
   end Write;"""
    }
""")
    }
    
    out.write(s"""
end $thisPackage;
""")

    out.close()
  }

  private def readBlock(t: Type, f : Field) : String = {
    def defaultBlock(read : String) : String = s"""
      for I in First + 1 .. Last loop
         To_${name(t)} (Data (I)).Set_${name(f)} ($read);
      end loop;"""

    if(f.isConstant())
      return ""

    f.getType match {
      case ft : UserType ⇒  s"""
         declare
            function Cast is new Ada.Unchecked_Conversion
              (Skill.Field_Types.Field_Type,
               ${poolsPackage}.${name(f.getType)}_P.Pool);

            F_T_Data : Skill.Types.Annotation_Array := Cast (This.T).Base.Data;
            Index    : Natural;
         begin
            for I in First + 1 .. Last loop
               Index := Natural (Input.V64);
               if Index in F_T_Data'Range then
                  To_${name(t)} (Data (I)).Set_${name(f)} (To_${name(f.getType)} (F_T_Data (Index)));
               else
                  To_${name(t)} (Data (I)).Set_${name(f)} (null);
               end if;
            end loop;
         end;"""

      case ft : GroundType ⇒ ft.getName.ada match {
        case "Annotation" ⇒  s"""
      declare
            Types : Skill.Types.Pools.Type_Vector := Skill.Field_Types.Builtin.Annotation_Type_P.Field_Type (This.T).Types;
            Type_ID : Natural;
            D : Skill.Types.Annotation_Array;
      begin
            for I in First + 1 .. Last loop
               Type_ID := Natural (Input.V64);
               if 0 = Type_ID then
                  Type_ID := Natural(Input.V64);
                  To_${name(t)} (Data (I)).Set_${name(f)} (null);
               else
                  D := Types.Element (Type_ID - 1).Base.Data;
                  To_${name(t)} (Data (I)).Set_${name(f)} (D (Natural (Input.V64)));
               end if;
         end loop;
      end;"""

        case "String"     ⇒ s"""
      declare
         Strings : Skill.String_Pools.Pool := Skill.Field_Types.Builtin.String_Type_P.Field_Type (This.T).Strings;
      begin
         for I in First + 1 .. Last loop
            To_${name(t)} (Data (I)).Set_${name(f)} (Strings.Get (Input.V64));
         end loop;
      end;"""

        case n ⇒ defaultBlock("Input."+n)
      }

      case ft : ConstantLengthArrayType ⇒ s"""
      declare
         B : ${fullTypePackage(ft)}.ref;
         Typ : Skill.Field_Types.Builtin.Const_Arrays_P.Field_Type
             := Skill.Field_Types.Builtin.Const_Arrays_P.Field_Type (This.T);
      begin
         for I in First + 1 .. Last loop
            B := ${fullTypePackage(ft)}.Make;
            for Idx in 1 .. Typ.Length loop
               B.Append (Typ.Base.Read_Box (Input));
            end loop; 
            
            To_${name(t)} (Data (I)).Set_${name(f)} (B);
         end loop;
      end;"""

      case ft : SingleBaseTypeContainer ⇒  
        val fieldType = "Skill.Field_Types.Builtin." + (ft match {
          case t : VariableLengthArrayType ⇒ "Var_Arrays_P"
          case t : ListType ⇒ "List_Type_P"
          case t : SetType ⇒ "Set_Type_P"
        }) + ".Field_Type"
      s"""
      declare
         B : ${fullTypePackage(ft)}.Ref;
         Typ : $fieldType :=
           $fieldType (This.T);
      begin
         for I in First + 1 .. Last loop
            B := ${fullTypePackage(ft)}.Make;
            for Idx in 1 .. Input.V64 loop
               B.Add (Typ.Base.Read_Box (Input));
            end loop; 
            
            To_${name(t)} (Data (I)).Set_${name(f)} (B);
         end loop;
      end;"""

      case ft : MapType                 ⇒
        val fieldType = "Skill.Field_Types.Builtin.Map_Type_P.Field_Type"
        s"""
      declare
         B : ${fullTypePackage(ft)}.Ref;
         T1 : $fieldType :=
           $fieldType (This.T);
         K,V1 : Skill.Types.Box;
      begin
         for I in First + 1 .. Last loop
            B := ${fullTypePackage(ft)}.Make;
            for Idx in 1 .. Input.V64 loop
               K := T1.Key.Read_Box (Input);
               ${readInnerMap(ft.getBaseTypes.toList.tail, 2)}
               B.Update (K, V1);
            end loop;
            To_${name(t)} (Data (I)).Set_${name(f)} (B);
         end loop;
      end;"""
      case _ ⇒ ???
    }
  }
  
  private final def readInnerMap(ts : List[Type], depth : Int) : String = {
    if(ts.length == 1) s"V${depth-1} := T${depth-1}.Value.Read_Box (Input);"
    else {
      val mapType = ts.map { x ⇒ escaped(x.getSkillName) }.mkString(s"Standard.$PackagePrefix.Skill_Map_", "_", "")
      s"""
               declare
                  M$depth : $mapType.Ref := $mapType.Make;
                  T$depth : Skill.Field_Types.Builtin.Map_Type_P.Field_Type :=
                         Skill.Field_Types.Builtin.Map_Type_P.Field_Type (T${depth-1}.Value);
                  K$depth,V$depth : Skill.Types.Box;
               begin
                  for Idx$depth in 1 .. Input.V64 loop
                     K$depth := T$depth.Key.Read_Box (Input);
                     ${readInnerMap(ts.tail, depth+1)}
                     M$depth.Update (K$depth, V$depth);
                  end loop;
                  V${depth-1} := Skill.Field_Types.Builtin.Map_Type_P.Boxed (M$depth);
               end;"""
    }
  }
}
