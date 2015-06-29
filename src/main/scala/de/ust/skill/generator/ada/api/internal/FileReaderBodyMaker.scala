/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import de.ust.skill.ir._
import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._

trait FileReaderBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api-internal-file_reader.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Api.Internal.File_Reader is

   String_Pool : String_Pool_Access;
   Types       : Types_Hash_Map_Access;

   procedure Read (
      State     : access Skill_State;
      File_Name :        String
   ) is
   begin
      String_Pool := State.String_Pool;
      Types       := State.Types;

      Byte_Reader.Reset_Buffer;

      ASS_IO.Open (Input_File, ASS_IO.In_File, File_Name);
      Input_Stream := ASS_IO.Stream (Input_File);

      while not ASS_IO.End_Of_File (Input_File) or else not Byte_Reader.End_Of_Buffer loop
         Read_String_Block;
         Read_Type_Block;
         Update_Storage_Pool_Start_Index;
      end loop;

      ASS_IO.Close (Input_File);
   end Read;

   procedure Read_String_Block is
      Count          : Long := Byte_Reader.Read_v64 (Input_Stream);
      String_Lengths : array (1 .. Count) of i32;
      Last_End       : i32  := 0;
   begin
      --  read ends and calculate lengths
      for I in String_Lengths'Range loop
         declare
            String_End    : i32 := i32 (Byte_Reader.Read_i32 (Input_Stream));
            String_Length : i32 := String_End - Last_End;
         begin
            String_Lengths (I) := String_End - Last_End;
            Last_End := String_End;
         end;
      end loop;

      --  read strings
      for I in String_Lengths'Range loop
         declare
            String_Length : i32    := String_Lengths (I);
            Next_String   : String := Byte_Reader.Read_String (Input_Stream, String_Length);
         begin
            String_Pool.Append (Next_String);
         end;
      end loop;
   end Read_String_Block;

   procedure Read_Type_Block is
      Count    : Long := Byte_Reader.Read_v64 (Input_Stream);
      Last_End : Long := 0;
   begin
      for I in 1 .. Count loop
         Read_Type_Declaration (Last_End);
      end loop;

      Read_Field_Data;
   end Read_Type_Block;

   procedure Read_Type_Declaration (Last_End : in out Long) is
      Type_Name      : String := String_Pool.Element (Natural (Byte_Reader.Read_v64 (Input_Stream)));
      Instance_Count : Natural;
      Field_Count    : Long;
   begin
      Instance_Count := Natural (Byte_Reader.Read_v64 (Input_Stream));

      if not Types.Contains (Type_Name) then
         Skip_Restrictions;

         declare
            procedure Free is new Ada.Unchecked_Deallocation (String, String_Access);

            Super_Name_Index : Long          := Byte_Reader.Read_v64 (Input_Stream);
            Super_Name       : String_Access := new String'("");
         begin
            if Super_Name_Index > 0 then
               Super_Name := new String'(String_Pool.Element (Natural (Super_Name_Index)));
            end if;

            declare
               New_Type_Fields       : Fields_Vector.Vector;
               New_Type_Storage_Pool : Storage_Pool_Vector.Vector;
               New_Type              : Type_Information := new Type_Declaration'(
                  Type_Size    => Type_Name'Length,
                  Super_Size   => Super_Name.all'Length,
                  id           => Long (Natural (Types.Length) + 32),
                  Name         => Type_Name,
                  Super_Name   => Super_Name.all,
                  spsi         => 0,
                  lbpsi        => 0,
                  Fields       => New_Type_Fields,
                  Storage_Pool => New_Type_Storage_Pool,
                  Known        => False,
                  Written      => True
               );
            begin
               Free (Super_Name);
               Types.Insert (New_Type.Name, New_Type);
            end;
         end;
      end if;

      if 0 /= Types.Element (Type_Name).Super_Name'Length then
         Types.Element (Type_Name).lbpsi := Natural (Byte_Reader.Read_v64 (Input_Stream));
      end if;

      Field_Count := Byte_Reader.Read_v64 (Input_Stream);

      declare
         Field_Index  : Long;
         Known_Fields : Long := Long (Types.Element (Type_Name).Fields.Length);
         Start_Index  : Natural;
         End_Index    : Natural;
      begin
         if 0 = Instance_Count then
            End_Index := Natural (Types.Element (Type_Name).Storage_Pool.Length);
         else
            Start_Index := Natural (Types.Element (Type_Name).Storage_Pool.Length) + 1;
            End_Index   := Start_Index + Instance_Count - 1;
            Create_Objects (Type_Name, Instance_Count);
         end if;

         for I in 1 .. Field_Count loop
            declare
               Field_Id : Long := Long (Byte_Reader.Read_v64 (Input_Stream));
            begin
               if (Known_Fields < Field_Count and then Known_Fields < I) or 0 = Instance_Count then
                  Read_Field_Declaration (Type_Name, Field_Id);
                  Start_Index := 1;
               end if;

               Field_Index := I;
               if 0 = Instance_Count then
                  Field_Index := Field_Index + Known_Fields;
               end if;

               declare
                  Field_End         : Long              := Byte_Reader.Read_v64 (Input_Stream);
                  Data_Length       : Long              := Field_End - Last_End;
                  Field_Declaration : Field_Information :=
                     Types.Element (Type_Name).Fields.Element (Positive (Field_Index));
                  Item              : Queue_Item        := (
                     Type_Declaration  => Types.Element (Type_Name),
                     Field_Declaration => Field_Declaration,
                     Start_Index       => Start_Index,
                     End_Index         => End_Index,
                     Data_Length       => Data_Length
                  );
               begin
                  Last_End := Field_End;
                  Read_Queue.Append (Item);
               end;
            end;
         end loop;
      end;
   end Read_Type_Declaration;

   procedure Read_Field_Declaration (Type_Name : String; Field_Id : Long) is
      Field_Name : String := String_Pool.Element (Natural (Byte_Reader.Read_v64 (Input_Stream)));

      procedure Read_Map_Definition (Base_Types : in out Base_Types_Vector.Vector) is
      begin
         Base_Types.Append (Byte_Reader.Read_v64 (Input_Stream));

         declare
            Next_Type : Long := Byte_Reader.Read_v64 (Input_Stream);
         begin
            if 20 = Next_Type then
               Read_Map_Definition (Base_Types);
            else
               Base_Types.Append (Next_Type);
            end if;
         end;
      end Read_Map_Definition;
   begin
      declare
         Field_Type            : Long := Byte_Reader.Read_v64 (Input_Stream);
         Constant_Value        : Long := 0;
         Constant_Array_Length : Long := 0;

         --  see comment in file ${packagePrefix.toLowerCase}.ads
         Base_Types            : Base_Types_Vector.Vector;
      begin
         case Field_Type is
            --  const i8, i16, i32, i64, v64
            when 0 => Constant_Value := Long (Byte_Reader.Read_i8 (Input_Stream));
            when 1 => Constant_Value := Long (Byte_Reader.Read_i16 (Input_Stream));
            when 2 => Constant_Value := Long (Byte_Reader.Read_i32 (Input_Stream));
            when 3 => Constant_Value := Byte_Reader.Read_i64 (Input_Stream);
            when 4 => Constant_Value := Byte_Reader.Read_v64 (Input_Stream);

            --  array T[i]
            when 15 =>
               Constant_Array_Length := Byte_Reader.Read_v64 (Input_Stream);
               Base_Types.Append (Byte_Reader.Read_v64 (Input_Stream));

            --  array T[], list, set
            when 17 .. 19 => Base_Types.Append (Byte_Reader.Read_v64 (Input_Stream));

            --  map
            when 20 => Read_Map_Definition (Base_Types);

            when others => null;
         end case;

         declare
            New_Field : Field_Information := new Field_Declaration'(
               id                    => Field_Id,
               Size                  => Field_Name'Length,
               Name                  => Field_Name,
               F_Type                => Field_Type,
               Constant_Value        => Constant_Value,
               Constant_Array_Length => Constant_Array_Length,
               Base_Types            => Base_Types,
               Known                 => False,
               Written               => True
            );
         begin
            Types.Element (Type_Name).Fields.Append (New_Field);
         end;
      end;

      Skip_Restrictions;
   end Read_Field_Declaration;

   procedure Read_Field_Data is
   begin
      Read_Queue.Iterate (Read_Queue_Vector_Iterator'Access);
      Read_Queue.Clear;
   end Read_Field_Data;

   procedure Create_Objects (
      Type_Name      : String;
      Instance_Count : Natural
   ) is
   begin
${
      /**
       * Replaces the "old/wrong" object by the new object in all super types.
       */
      def printSuperTypes(d : UserType) : String = {
        var output = "";
        val superTypes = getSuperTypes(d).toList.reverse
        superTypes.foreach({ t ⇒
          output += s"""\r\n\r\n                  declare
                     Sub_Type   : Type_Information := ${name(d)}_Type_Declaration;
                     Super_Type : Type_Information := ${name(t)}_Type_Declaration;
                     Index      : Natural          := (Sub_Type.lbpsi - Super_Type.lbpsi) + Super_Type.spsi + I;
                  begin\r\n"""
          if (t == superTypes.last)
            output += s"""                     declare
                        procedure Free is new Ada.Unchecked_Deallocation (${name(t)}_Type, ${name(t)}_Type_Access);
                        Old_Object : ${name(t)}_Type_Access :=
                           ${name(t)}_Type_Access (Super_Type.Storage_Pool.Element (Index));
                     begin
                        Object.skill_id := Old_Object.skill_id;
                        Free (Old_Object);
                     end;\r\n"""
          output += s"""                     Super_Type.Storage_Pool.Replace_Element (Index, Object);
                  end;"""
        })

        if (output.isEmpty())
          "null;"
        else
          output
      }

      /**
       * Provides the default values of all fields of a given type.
       */
      def printDefaultValues(d : UserType) : String = {
        var output = s"""'(\r\n                     skill_id => ${if (null == d.getSuperType) s"Natural (${name(d.getBaseType)}_Type_Declaration.Storage_Pool.Length) + 1" else "0"}"""
        val fields = d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored })
        output += fields.map({ f ⇒
          s""",\r\n                     ${escapedLonely(f.getSkillName)} => ${defaultValue(f)}"""
        }).mkString("")
        output += "\r\n                  )";
        output
      }

      var output = "";
      /**
       * Provides the type record information with the fields and their default values of all types.
       */
      for (d ← IR) {
        output += s"""      if "${d.getSkillName}" = Type_Name then
         declare
${
          var output = s"""            ${name(d)}_Type_Declaration : Type_Information := Types.Element ("${d.getSkillName}");\r\n"""
          val superTypes = getSuperTypes(d).toList.reverse
          superTypes.foreach({ t ⇒
            output += s"""            ${name(t)}_Type_Declaration : Type_Information := Types.Element ("${t.getSkillName}");\r\n"""
          })
          output.stripLineEnd
        }
         begin
            for I in 1 .. Instance_Count loop
               declare
"""
        output += d.getFields.filter({ f ⇒ !f.isIgnored }).map({ f ⇒
          f.getType match {
            case t : VariableLengthArrayType ⇒
              s"               New_${mapType(f.getType, d, f).stripSuffix(".Vector")} : ${mapType(f.getType, d, f)};\r\n"
            case t : ListType ⇒
              s"               New_${mapType(f.getType, d, f).stripSuffix(".List")} : ${mapType(f.getType, d, f)};\r\n"
            case t : SetType ⇒
              s"               New_${mapType(f.getType, d, f).stripSuffix(".Set")} : ${mapType(f.getType, d, f)};\r\n"
            case t : MapType ⇒
              s"               New_${mapType(f.getType, d, f).stripSuffix(".Map")} : ${mapType(f.getType, d, f)};\r\n"
            case _ ⇒ ""
          }
        }).mkString("")
        output += s"""                  Object : Skill_Type_Access := new ${name(d)}_Type${printDefaultValues(d)};
               begin
                  ${name(d)}_Type_Declaration.Storage_Pool.Append (Object);${printSuperTypes(d)}
               end;
            end loop;
         end;
      end if;\r\n"""
      }
      if (output.isEmpty)
        "null;"
      else
        output.stripSuffix("\r\n")
    }
   end Create_Objects;

   procedure Read_Queue_Vector_Iterator (Iterator : Read_Queue_Vector.Cursor) is
      use Storage_Pool_Vector;

      Item       : Queue_Item := Read_Queue_Vector.Element (Iterator);
      Skip_Bytes : Boolean    := True;

      Type_Declaration  : Type_Information          := Item.Type_Declaration;
      Field_Declaration : Field_Information         := Item.Field_Declaration;
      Storage_Pool      : Storage_Pool_Array_Access :=
         new Storage_Pool_Array (1 .. Natural (Type_Declaration.Storage_Pool.Length));

      Type_Name  : String := Type_Declaration.Name;
      Field_Name : String := Field_Declaration.Name;

      procedure Iterate (Position : Cursor) is
         Index : Positive := To_Index (Position);
      begin
         Storage_Pool (Index) := Element (Position);
      end Iterate;
      pragma Inline (Iterate);
   begin
      Type_Declaration.Storage_Pool.Iterate (Iterate'Access);

${
      /**
       * Reads the field data of all fields.
       */
      (for (d ← IR; f ← d.getFields if !f.isAuto() && !f.isIgnored())
        yield s"""
      if "${d.getSkillName}" = Type_Name and then "${f.getSkillName}" = Field_Name then
         for I in Item.Start_Index .. Item.End_Index loop
            declare
               Object : ${name(d)}_Type_Access := ${name(d)}_Type_Access (Storage_Pool (I));
            ${mapFileReader(d, f)}
            end;
         end loop;
         Skip_Bytes := False;
      end if;
""").mkString("")
    }

      if True = Skip_Bytes then
         Byte_Reader.Skip_Bytes (Input_Stream, Item.Data_Length);
      end if;

      Free (Storage_Pool);
   end Read_Queue_Vector_Iterator;

   function Read_Annotation (Input_Stream : ASS_IO.Stream_Access) return Skill_Type_Access is
      Base_Type_Name : v64 := Byte_Reader.Read_v64 (Input_Stream);
      Index          : v64 := Byte_Reader.Read_v64 (Input_Stream);
   begin
      if 0 = Base_Type_Name then
         return null;
      else
         return Types.Element (String_Pool.Element (Natural (Base_Type_Name))).Storage_Pool.Element (Positive (Index));
      end if;
   end Read_Annotation;

   function Read_String (Input_Stream : ASS_IO.Stream_Access) return String_Access is
      (new String'(String_Pool.Element (Natural (Byte_Reader.Read_v64 (Input_Stream)))));

${
      var output = "";
      /**
       * Reads the skill id of a given object.
       */
      for (d ← IR) {
        output += s"""   function Read_${name(d)}_Type (Input_Stream : ASS_IO.Stream_Access) return ${name(d)}_Type_Access is
      Index : Long := Byte_Reader.Read_v64 (Input_Stream);
   begin
      if 0 = Index then
         return null;
      else
         return ${name(d)}_Type_Access (Types.Element ("${if (null == d.getSuperType) d.getSkillName else d.getBaseType.getSkillName}").Storage_Pool.Element (Positive (Index)));
      end if;
   end Read_${name(d)}_Type;\r\n\r\n"""
      }
      output.stripSuffix("\r\n")
    }
   procedure Update_Storage_Pool_Start_Index is
   begin
${
      var output = "";
      /**
       * Corrects the SPSI (storage pool start index) of all types.
       */
      for (d ← IR) {
        output += s"""      if Types.Contains ("${d.getSkillName}") then
         Types.Element ("${d.getSkillName}").spsi := Natural (Types.Element ("${d.getSkillName}").Storage_Pool.Length);
      end if;\r\n"""
      }
      if (output.isEmpty())
        "null;"
      else
        output.stripSuffix("\r\n")
    }
   end Update_Storage_Pool_Start_Index;

   procedure Skip_Restrictions is
      Zero : Long := Byte_Reader.Read_v64 (Input_Stream);
   begin
      null;
   end Skip_Restrictions;

end ${packagePrefix.capitalize}.Api.Internal.File_Reader;
""")

    out.close()
  }

  /**
   * Generates the read functions into the procedure Read_Queue_Vector_Iterator in package File_Reader.
   */
  protected def mapFileReader(d : UserType, f : Field) : String = {
    /**
     * The basis type of the field that will be read.
     */
    def inner(t : Type, _d : UserType, _f : Field) : String = {
      t match {
        case t : GroundType ⇒ t.getName.lower match {
          case "annotation" ⇒
            s"Read_Annotation (Input_Stream)"
          case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
            s"Byte_Reader.Read_${mapType(t, _d, _f)} (Input_Stream)"
          case "string" ⇒
            s"Read_String (Input_Stream)"
        }
        case t : Declaration ⇒
          s"""Read_${name(t)}_Type (Input_Stream)"""
      }
    }

    f.getType() match {
      case t : GroundType ⇒ t.getName.lower match {
        case "annotation" ⇒
          s"""begin
               Object.${escapedLonely(f.getSkillName)} := ${inner(f.getType, d, f)};"""

        case "bool" | "i8" | "i16" | "i32" | "i64" | "v64" | "f32" | "f64" ⇒
          if (f.isConstant) {
            s"""   Skill_Parse_Constant_Error : exception;
            begin
               if Object.Get_${name(f)} /= ${mapType(f.getType, d, f)} (Field_Declaration.Constant_Value) then
                  raise Skill_Parse_Constant_Error;
               end if;"""
          } else {
            s"""begin
               Object.${escapedLonely(f.getSkillName)} := ${inner(f.getType, d, f)};"""
          }

        case "string" ⇒
          s"""begin
               Object.${escapedLonely(f.getSkillName)} := ${inner(f.getType, d, f)};"""
      }

      case t : ConstantLengthArrayType ⇒
        s"""
               Skill_Parse_Constant_Array_Length_Error : exception;
            begin
               if ${t.getLength} /= Field_Declaration.Constant_Array_Length then
                  raise Skill_Parse_Constant_Array_Length_Error;
               end if;
               for I in 1 .. ${t.getLength} loop
                  Object.${escapedLonely(f.getSkillName)} (I) := ${inner(t.getBaseType, d, f)};
               end loop;"""

      case t : VariableLengthArrayType ⇒
        s"""begin
               for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                  Object.${escapedLonely(f.getSkillName)}.Append (${inner(t.getBaseType, d, f)});
               end loop;"""

      case t : ListType ⇒
        s"""begin
               for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                  Object.${escapedLonely(f.getSkillName)}.Append (${inner(t.getBaseType, d, f)});
               end loop;"""

      case t : SetType ⇒
        s"""
               Inserted : Boolean;
               Position : ${name(d)}_${f.getSkillName.capitalize}_Set.Cursor;
               pragma Unreferenced (Position);
            begin
               for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                  ${name(d)}_${f.getSkillName.capitalize}_Set.Insert (Object.${escapedLonely(f.getSkillName)}, ${inner(t.getBaseType, d, f)}, Position, Inserted);
               end loop;"""

      case t : MapType ⇒
        s"""${
          var output = ""
          val types = t.getBaseTypes().reverse
          types.slice(0, types.length - 1).zipWithIndex.foreach({
            case (t, i) ⇒
              val x = {
                if (0 == i)
                  s"""declare
                        Key   : ${mapType(types.get(i + 1), d, f)} := ${inner(types.get(i + 1), d, f)};
                        Value : ${mapType(types.get(i), d, f)} := ${inner(types.get(i), d, f)};
                     begin
                        Map.Insert (Key, Value);
                     end;"""
                else
                  s"""declare
                        Key : ${mapType(types.get(i + 1), d, f)} := ${inner(types.get(i + 1), d, f)};
                     begin
                        Map.Insert (Key, Read_Map_${types.length - i});
                     end;"""
              }
              output += s"""               function Read_Map_${types.length - (i + 1)} return ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length - (i + 1)}.Map is
                  Map : ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length - (i + 1)}.Map;
               begin
                  for I in 1 .. Byte_Reader.Read_v64 (Input_Stream) loop
                     ${x}
                  end loop;
                  return Map;
               end Read_Map_${types.length - (i + 1)};
               pragma Inline (Read_Map_${types.length - (i + 1)});\r\n\r\n"""
          })
          output.stripLineEnd.stripLineEnd
        }
            begin
               Object.${escapedLonely(f.getSkillName)} := Read_Map_1;"""

      case t : Declaration ⇒
        s"""begin
               Object.${escapedLonely(f.getSkillName)} := ${inner(f.getType, d, f)};"""
    }
  }
}
