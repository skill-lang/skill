/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
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
      if not Types.Contains (Type_Name) then
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
                  spsi         => 1,
                  lbpsi        => 1,
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

         if 0 /= Types.Element (Type_Name).Super_Name'Length then
            Types.Element (Type_Name).lbpsi := Positive (Byte_Reader.Read_v64 (Input_Stream));
         end if;

         Instance_Count := Natural (Byte_Reader.Read_v64 (Input_Stream));
         Skip_Restrictions;
      else
         if 0 /= Types.Element (Type_Name).Super_Name'Length then
            Types.Element (Type_Name).lbpsi := Positive (Byte_Reader.Read_v64 (Input_Stream));
         end if;

         Instance_Count := Natural (Byte_Reader.Read_v64 (Input_Stream));
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
            if (Known_Fields < Field_Count and then Known_Fields < I) or 0 = Instance_Count then
               Read_Field_Declaration (Type_Name);
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
         end loop;
      end;
   end Read_Type_Declaration;

   procedure Read_Field_Declaration (Type_Name : String) is
   begin
      Skip_Restrictions;

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
            when 20 =>
               declare
                  Base_Types_Count : Long := Byte_Reader.Read_v64 (Input_Stream);
               begin
                  for I in 1 .. Base_Types_Count loop
                     Base_Types.Append (Byte_Reader.Read_v64 (Input_Stream));
                  end loop;
               end;

            when others => null;
         end case;

         declare
            Field_Name : String := String_Pool.Element (Natural (Byte_Reader.Read_v64 (Input_Stream)));

            New_Field : Field_Information := new Field_Declaration'(
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
                     Sub_Type   : Type_Information := ${d.getName.ada}_Type_Declaration;
                     Super_Type : Type_Information := ${t.getName.ada}_Type_Declaration;
                     Index      : Natural          := (Sub_Type.lbpsi - Super_Type.lbpsi) + Super_Type.spsi + I - 1;
                  begin\r\n"""
          if (t == superTypes.last)
            output += s"""                     declare
                        procedure Free is new Ada.Unchecked_Deallocation (${t.getName.ada}_Type, ${t.getName.ada}_Type_Access);
                        Old_Object : ${t.getName}_Type_Access :=
                           ${t.getName.ada}_Type_Access (Super_Type.Storage_Pool.Element (Index));
                     begin
                        Object.skill_id := Old_Object.skill_id;
                        Free (Old_Object);
                     end;\r\n"""
          output += s"""                     Super_Type.Storage_Pool.Replace_Element (Index, Object);
                  end;"""
        })
        output
      }

      /**
       * Provides the default values of all fields of a given type.
       */
      def printDefaultValues(d : UserType) : String = {
        var output = s"""'(\r\n                     skill_id => ${if (null == d.getSuperType) s"Natural (${d.getBaseType.getName.ada}_Type_Declaration.Storage_Pool.Length) + 1" else "0"}"""
        val fields = d.getAllFields.filter({ f ⇒ !f.isConstant && !f.isIgnored })
        output += fields.map({ f ⇒
          s""",\r\n                     ${f.getSkillName} => ${defaultValue(f)}"""
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
          var output = s"""            ${d.getName.ada}_Type_Declaration : Type_Information := Types.Element ("${d.getSkillName}");\r\n"""
          val superTypes = getSuperTypes(d).toList.reverse
          superTypes.foreach({ t ⇒
            output += s"""            ${t.getName.ada}_Type_Declaration : Type_Information := Types.Element ("${t.getSkillName}");\r\n"""
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
        output += s"""                  Object : Skill_Type_Access := new ${d.getName.ada}_Type${printDefaultValues(d)};
               begin
                  ${d.getName.ada}_Type_Declaration.Storage_Pool.Append (Object);${printSuperTypes(d)}
               end;
            end loop;
         end;
      end if;\r\n"""
      }
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
      var output = "";
      /**
       * Reads the field data of all fields.
       */
      for (d ← IR) {
        output += d.getFields.filter({ f ⇒ !f.isAuto && !f.isIgnored }).map({ f ⇒
          s"""      if "${d.getSkillName}" = Type_Name and then "${f.getSkillName}" = Field_Name then
         for I in Item.Start_Index .. Item.End_Index loop
            declare
               Object : ${d.getName.ada}_Type_Access := ${d.getName.ada}_Type_Access (Storage_Pool (I));
            ${mapFileReader(d, f)}
            end;
         end loop;
         Skip_Bytes := False;
      end if;\r\n"""
        }).mkString("")
      }
      output.stripSuffix("\r\n")
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
        output += s"""   function Read_${d.getName.ada}_Type (Input_Stream : ASS_IO.Stream_Access) return ${d.getName.ada}_Type_Access is
      Index : Long := Byte_Reader.Read_v64 (Input_Stream);
   begin
      if 0 = Index then
         return null;
      else
         return ${d.getName.ada}_Type_Access (Types.Element ("${if (null == d.getSuperType) d.getSkillName else d.getBaseType.getSkillName}").Storage_Pool.Element (Positive (Index)));
      end if;
   end Read_${d.getName.ada}_Type;\r\n\r\n"""
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
         Types.Element ("${d.getSkillName}").spsi := Natural (Types.Element ("${d.getSkillName}").Storage_Pool.Length) + 1;
      end if;\r\n"""
      }
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
}
