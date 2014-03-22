/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._
import de.ust.skill.ir._

trait FileWriterBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-file_writer.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize}.Internal.File_Writer is

   Last_Types_End : Long := 0;

   procedure Write (pState : access Skill_State; File_Name : String) is
      Output_File : ASS_IO.File_Type;
   begin
      State := pState;

      ASS_IO.Create (Output_File, ASS_IO.Out_File, File_Name);
      ASS_IO.Create (Field_Data_File, ASS_IO.Out_File);

      Field_Data_Stream := ASS_IO.Stream (Field_Data_File);
      Output_Stream := ASS_IO.Stream (Output_File);

      Write_String_Pool;
      Write_Type_Block;

      Byte_Writer.Finalize_Buffer (Output_Stream);

      ASS_IO.Delete (Field_Data_File);
      ASS_IO.Flush (Output_File);
      ASS_IO.Close (Output_File);
   end Write;

   procedure Prepare_String_Pool is
   begin
      State.Get_Types.Iterate (Prepare_String_Pool_Iterator'Access);
   end Prepare_String_Pool;

   procedure Prepare_String_Pool_Iterator (Iterator : Types_Hash_Map.Cursor) is
      Type_Declaration : Type_Information := Types_Hash_Map.Element (Iterator);
      Type_Name : String := Type_Declaration.Name;
      Super_Name : String := Type_Declaration.Super_Name;
   begin
      if 0 < Natural (Type_Declaration.Storage_Pool.Length) then
         State.Put_String (Type_Name, Safe => True);
         State.Put_String (Super_Name, Safe => True);

         declare
            procedure Iterate (Iterator : Fields_Vector.Cursor) is
               Field_Declaration : Field_Information := Fields_Vector.Element (Iterator);
               Field_Name : String := Field_Declaration.Name;
            begin
               State.Put_String (Field_Name, Safe => True);
            end Iterate;
            pragma Inline (Iterate);
         begin
            Type_Declaration.Fields.Iterate (Iterate'Access);
         end;

         declare
            procedure Iterate (Iterator : Storage_Pool_Vector.Cursor) is
               Skill_Object : Skill_Type_Access := Storage_Pool_Vector.Element (Iterator);
            begin
${
  var output = "";
  for (d ← IR) {
    var hasOutput = false;
    output += s"""               if "${d.getSkillName}" = Type_Name then
                  declare
                     Object : ${d.getName}_Type_Access := ${d.getName}_Type_Access (Skill_Object);
                  begin
"""
    d.getFields.filter({ f ⇒ "string" == f.getType.getSkillName }).foreach({ f ⇒
      hasOutput = true;
      output += s"                     State.Put_String (SU.To_String (Object.${f.getSkillName}), Safe => True);\r\n"
    })
    d.getFields.foreach({ f =>
      f.getType match {
        case t: ConstantLengthArrayType ⇒
          if ("string" == t.getBaseType.getName) {
            hasOutput = true;
            output += s"""\r\n                     for I in Object.${f.getSkillName}'Range loop
                        State.Put_String (SU.To_String (Object.${f.getSkillName} (I)), Safe => True);
                     end loop;\r\n""";
          }
        case t: VariableLengthArrayType ⇒
          if ("string" == t.getBaseType.getName) {
            hasOutput = true;
            output += s"""\r\n                     declare
                        use ${mapType(t, d, f).stripSuffix(".Vector")};

                        Vector : ${mapType(t, d, f)} := Object.${f.getSkillName};

                        procedure Iterate (Position : Cursor) is
                        begin
                           State.Put_String (SU.To_String (Element (Position)), Safe => True);
                        end Iterate;
                        pragma Inline (Iterate);
                     begin
                        Vector.Iterate (Iterate'Access);
                     end;\r\n"""
          }
        case t: ListType ⇒
           if ("string" == t.getBaseType.getName) {
             hasOutput = true;
             output += s"""\r\n                     declare
                        use ${mapType(t, d, f).stripSuffix(".List")};

                        List : ${mapType(t, d, f)} := Object.${f.getSkillName};

                        procedure Iterate (Position : Cursor) is
                        begin
                           State.Put_String (SU.To_String (Element (Position)), Safe => True);
                        end Iterate;
                        pragma Inline (Iterate);
                     begin
                        List.Iterate (Iterate'Access);
                     end;\r\n"""
          }
        case t: SetType ⇒
           if ("string" == t.getBaseType.getName) {
             hasOutput = true;
             output += s"""\r\n                     declare
                        use ${mapType(t, d, f).stripSuffix(".Set")};

                        Set : ${mapType(t, d, f)} := Object.${f.getSkillName};

                        procedure Iterate (Position : Cursor) is
                        begin
                           State.Put_String (SU.To_String (Element (Position)), Safe => True);
                        end Iterate;
                        pragma Inline (Iterate);
                     begin
                        Set.Iterate (Iterate'Access);
                     end;\r\n"""
          }
        case t: MapType ⇒
          val types = t.getBaseTypes().reverse
          if (types.map({ x => x.getName }).contains("string")) {
            hasOutput = true;
            output += s"                     declare\r\n"
            types.slice(0, types.length-1).zipWithIndex.foreach({ case (t, i) =>
              val x = {
                var output = ""
                if (0 == i) {
                  if ("string" == types.get(i+1).getName) output += s"State.Put_String (SU.To_String (Key (Position)), Safe => True);"
                  if ("string" == types.get(i).getName) {
                    if (!output.isEmpty) output += "\r\n                     "
                    output += s"State.Put_String (SU.To_String (Element (Position)), Safe => True);"
                  }
                }
                else {
                  if ("string" == types.get(i+1).getName) output += s"State.Put_String (SU.To_String (Key (Position)), Safe => True);\r\n                              "
                  output += s"Read_Map_${types.length-i} (Element (Position));"
                }
                if (output.isEmpty) "null;" else output
              }
              output += s"""                        procedure Read_Map_${types.length-(i+1)} (Map : ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length-(i+1)}.Map) is
                           use ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length-(i+1)};

                           procedure Iterate (Position : Cursor) is
                           begin
                              ${x}
                           end Iterate;
                           pragma Inline (Iterate);
                        begin
                           Map.Iterate (Iterate'Access);
                        end Read_Map_${types.length-(i+1)};
                        pragma Inline (Read_Map_${types.length-(i+1)});\r\n\r\n"""
            })
            output = output.stripLineEnd
            output += s"""                     begin
                        Read_Map_1 (Object.${f.getSkillName});
                     end;\r\n"""
          }
        case _ ⇒ null
      }
    })
    if (!hasOutput) output += s"                     null;\r\n"
    output += s"""                  end;
               end if;\r\n"""
  }
  output.stripSuffix("\r\n")
}
            end Iterate;
            pragma Inline (Iterate);
         begin
            Type_Declaration.Storage_Pool.Iterate (Iterate'Access);
         end;
      end if;
   end Prepare_String_Pool_Iterator;

   procedure Write_String_Pool is
   begin
      Prepare_String_Pool;

      declare
         Size : Natural := State.String_Pool_Size;
         Last_String_End : Natural := 0;
      begin
         Byte_Writer.Write_v64 (Output_Stream, Long (Size));

         for I in 1 .. Size loop
            declare
               X : String := State.Get_String (I);
               String_Length : Positive := X'Length + Last_String_End;
            begin
               Byte_Writer.Write_i32 (Output_Stream, String_Length);
               Last_String_End := String_Length;
            end;
         end loop;

         for I in 1 .. Size loop
            Byte_Writer.Write_String (Output_Stream, State.Get_String (I));
         end loop;
      end;
   end Write_String_Pool;

   procedure Order_Types is
   begin${
  var output = ""
  for (d ← IR) {
    if (null == d.getSuperType) {
      val types = getSubTypes(d).+=:(d)

      output += s"""\r\n      declare
         use Storage_Pool_Vector;

         Type_Declaration : Type_Information := State.Get_Type ("${d.getSkillName}");
         Length : Natural := Natural (Type_Declaration.Storage_Pool.Length);

         type Temp_Type is array (1 .. Length) of Skill_Type_Access;
         Temp : Temp_Type;
         Index : Positive := 1;
"""
      types.foreach({ t =>
        output += s"""\r\n         ${t.getName}_Type_Declaration : Type_Information := State.Get_Type ("${t.getSkillName}");
         procedure Iterate_${t.getName} (Iterator : Cursor) is
            Object : Skill_Type_Access := Element (Iterator);
         begin
            if 0 = ${t.getName}_Type_Declaration.lbpsi then
               ${t.getName}_Type_Declaration.lbpsi := Index;
            end if;
            if "${t.getSkillName}" = Get_Object_Type (Object) then
               Temp (Index) := Object;
               Index := Index + 1;
            end if;
         end Iterate_${t.getName};
         pragma Inline (Iterate_${t.getName});\r\n"""
      })
      output += "      begin\r\n"
      types.foreach({ t =>
        output += s"""         ${t.getName}_Type_Declaration.lbpsi := 0;
         Type_Declaration.Storage_Pool.Iterate (Iterate_${t.getName}'Access);\r\n"""
      })
      output += "\r\n"
      types.foreach({ t =>
        output += s"""         declare
            Next_Type_Declaration : Type_Information := ${t.getName}_Type_Declaration;
            Start_Index : Natural := Next_Type_Declaration.lbpsi;
            End_Index : Integer := Start_Index + Natural (Next_Type_Declaration.Storage_Pool.Length) - 1;
         begin
            for I in Start_Index .. End_Index loop
               Temp (I).skill_id := I;
               Next_Type_Declaration.Storage_Pool.Replace_Element (I - Start_Index + 1, Temp (I));
            end loop;
         end;\r\n"""
      })
      output += "      end;"
      output
    }
  }
  output.stripLineEnd.stripLineEnd
}
   end Order_Types;

   function Count_Instantiated_Types return Long is
      use Types_Hash_Map;

      rval : Long := 0;

      procedure Iterate (Iterator : Cursor) is
         Type_Declaration : Type_Information := Types_Hash_Map.Element (Iterator);
         Type_Name : String := Type_Declaration.Name;
      begin
         if 0 < State.Storage_Pool_Size (Type_Name) then
            rval := rval + 1;
         end if;
      end Iterate;
      pragma Inline (Iterate);
   begin
      State.Get_Types.Iterate (Iterate'Access);
      return rval;
   end Count_Instantiated_Types;

   procedure Write_Type_Block is
   begin
      Order_Types;

      Byte_Writer.Write_v64 (Output_Stream, Count_Instantiated_Types);

${
  def inner (d : Type): String = {
    s"""      declare
         Type_Declaration : Type_Information := State.Get_Type ("${d.getSkillName}");
         Type_Name : String := Type_Declaration.Name;
      begin
         if 0 < Natural (Type_Declaration.Storage_Pool.Length) then
            Write_Type_Declaration (Type_Declaration);
         end if;
      end;\r\n"""
  }
  var output = ""
  for (d ← IR) {
    if (null == d.getSuperType) {
      output += inner(d)
      getSubTypes(d).foreach({ t => output += inner(t) })
    }
  }
  output
}
      Last_Types_End := 0;

      Copy_Field_Data;
   end Write_Type_Block;

   procedure Write_Type_Declaration (Type_Declaration : Type_Information) is
      Type_Name : String := Type_Declaration.Name;
      Super_Name : String := Type_Declaration.Super_Name;
      Field_Size : Natural := State.Field_Size (Type_Name);
   begin
      Byte_Writer.Write_v64 (Output_Stream, Long (State.Get_String_Index (Type_Name)));

      if 0 < Super_Name'Length then
         Byte_Writer.Write_v64 (Output_Stream, Long (State.Get_String_Index (Super_Name)));
         Byte_Writer.Write_v64 (Output_Stream, Long (Type_Declaration.lbpsi));
      else
         Byte_Writer.Write_v64 (Output_Stream, 0);
      end if;

      Byte_Writer.Write_v64 (Output_Stream, Long (State.Storage_Pool_Size (Type_Name)));
      Byte_Writer.Write_v64 (Output_Stream, 0);  --  restrictions
      Byte_Writer.Write_v64 (Output_Stream, Long (Field_Size));

      for I in 1 .. Field_Size loop
         Write_Field_Declaration (Type_Declaration, State.Get_Field (Type_Name, I));
      end loop;
   end Write_Type_Declaration;

   procedure Write_Field_Declaration (Type_Declaration : Type_Information; Field_Declaration : Field_Information) is
      Type_Name : String := Type_Declaration.Name;
      Field_Name : String := Field_Declaration.Name;
      Field_Type : Long := Field_Declaration.F_Type;
      Size : Long := Field_Data_Size (Type_Declaration, Field_Declaration);
      Base_Types : Base_Types_Vector.Vector := Field_Declaration.Base_Types;
   begin
      Byte_Writer.Write_v64 (Output_Stream, 0);  --  restrictions
      Byte_Writer.Write_v64 (Output_Stream, Long (Field_Type));

      case Field_Type is
         --  const i8, i16, i32, i64, v64
         when 0 => Byte_Writer.Write_i8 (Output_Stream, Short_Short_Integer (Field_Declaration.Constant_Value));
         when 1 => Byte_Writer.Write_i16 (Output_Stream, Short (Field_Declaration.Constant_Value));
         when 2 => Byte_Writer.Write_i32 (Output_Stream, Integer (Field_Declaration.Constant_Value));
         when 3 => Byte_Writer.Write_i64 (Output_Stream, Field_Declaration.Constant_Value);
         when 4 => Byte_Writer.Write_v64 (Output_Stream, Field_Declaration.Constant_Value);

         --  array T[i]
         when 15 =>
            Byte_Writer.Write_v64 (Output_Stream, Long (Field_Declaration.Constant_Array_Length));
            Byte_Writer.Write_v64 (Output_Stream, Field_Declaration.Base_Types.First_Element);

         --  array T[], list, set
         when 17 .. 19 => Byte_Writer.Write_v64 (Output_Stream, Field_Declaration.Base_Types.First_Element);

         --  map
         when 20 =>
            declare
               use Base_Types_Vector;

               procedure Iterate (Position : Cursor) is
                  X : Long := Element (Position);
               begin
                  Byte_Writer.Write_v64 (Output_Stream, X);
               end Iterate;
               pragma Inline (Iterate);
            begin
               Byte_Writer.Write_v64 (Output_Stream, Long (Base_Types.Length));
               Base_Types.Iterate (Iterate'Access);
            end;

         when others => null;
      end case;

      Byte_Writer.Write_v64 (Output_Stream, Long (State.Get_String_Index (Field_Name)));

      Last_Types_End := Last_Types_End + Size;
      Byte_Writer.Write_v64 (Output_Stream, Last_Types_End);
   end Write_Field_Declaration;

   function Field_Data_Size (Type_Declaration : Type_Information; Field_Declaration : Field_Information) return Long is
      Current_Index : Long := Long (ASS_IO.Index (Field_Data_File));
      rval : Long;
   begin
      Byte_Writer.Finalize_Buffer (Output_Stream);
      Write_Field_Data (Field_Data_Stream, Type_Declaration, Field_Declaration);
      Byte_Writer.Finalize_Buffer (Field_Data_Stream);
      rval := Long (ASS_IO.Index (Field_Data_File)) - Current_Index;
      return rval;
   end Field_Data_Size;

   procedure Write_Field_Data (Stream : ASS_IO.Stream_Access; Type_Declaration : Type_Information; Field_Declaration : Field_Information) is
      Type_Name : String := Type_Declaration.Name;
      Field_Name : String := Field_Declaration.Name;
   begin
${
  var output = "";
  for (d ← IR) {
    output += d.getFields.filter({ f ⇒ !f.isAuto && !f.isConstant && !f.isIgnored }).map({ f ⇒
      s"""      if "${d.getSkillName}" = Type_Name and then "${f.getSkillName}" = Field_Name then
         for I in 1 .. Natural (Type_Declaration.Storage_Pool.Length) loop
            declare
            ${mapFileWriter(d, f)}
            end;
         end loop;
      end if;\r\n"""}).mkString("")
  }
  output
}      null;
   end Write_Field_Data;

   procedure Copy_Field_Data is
   begin
      ASS_IO.Reset (Field_Data_File, ASS_IO.In_File);
      Byte_Reader.Buffer_Index := Byte_Reader.Buffer_Size;
      for I in Long (ASS_IO.Index (Field_Data_File)) .. Long (ASS_IO.Size (Field_Data_File)) loop
         Byte_Writer.Write_i8 (Output_Stream, Byte_Reader.Read_i8 (Field_Data_Stream));
      end loop;
   end Copy_Field_Data;

   procedure Write_Annotation (Stream : ASS_IO.Stream_Access; Object : Skill_Type_Access) is
      Type_Name : String := Get_Object_Type (Object);
      Types : Types_Hash_Map.Map := State.Get_Types;

      function Get_Base_Type (Type_Declaration : Type_Information) return String is
         Super_Name : String := Type_Declaration.Super_Name;
      begin
         if 0 = Super_Name'Length then
            return Type_Name;
         else
            return Get_Base_Type (State.Get_Type (Super_Name));
         end if;
      end Get_Base_Type;
   begin
      if 0 = Type_Name'Length then
         Byte_Writer.Write_v64 (Stream, 0);
         Byte_Writer.Write_v64 (Stream, 0);
      else
         Byte_Writer.Write_v64 (Stream, Long (State.Get_String_Index (Get_Base_Type (Types.Element (Type_Name)))));
         Byte_Writer.Write_v64 (Stream, Long (Object.skill_id));
      end if;
   end Write_Annotation;

   procedure Write_Unbounded_String (Stream : ASS_IO.Stream_Access; Value : SU.Unbounded_String) is
   begin
      Byte_Writer.Write_v64 (Stream, Long (State.Get_String_Index (SU.To_String (Value))));
   end Write_Unbounded_String;

${
  var output = "";
  for (d ← IR) {
    output += s"""   procedure Write_${d.getName}_Type (Stream : ASS_IO.Stream_Access; X : ${d.getName}_Type_Access) is
   begin
      if null = X then
         Byte_Writer.Write_v64 (Stream, 0);
      else
         Byte_Writer.Write_v64 (Stream, Long (X.skill_id));
      end if;
   end Write_${d.getName}_Type;\r\n\r\n"""
  }
  output.stripSuffix("\r\n")
}
   function Get_Object_Type (Object : Skill_Type_Access) return String is
      use Ada.Tags;
   begin
      if null = Object then
         return "";
      end if;

${
  var output = "";
  for (d ← IR) {
    output += s"""      if ${d.getName}_Type'Tag = Object'Tag then
         return "${d.getSkillName}";
      end if;\r\n"""
  }
  output
}
      return "";
   end Get_Object_Type;

end ${packagePrefix.capitalize}.Internal.File_Writer;
""")

    out.close()
  }
}
