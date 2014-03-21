/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import java.io.PrintWriter
import scala.collection.JavaConversions._
import de.ust.skill.ir._

trait PackageBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize} is

   function Hash (Element : Short_Short_Integer) return Ada.Containers.Hash_Type is
      (Ada.Containers.Hash_Type'Mod (Element));

   function Hash (Element : Short) return Ada.Containers.Hash_Type is
      (Ada.Containers.Hash_Type'Mod (Element));

   function Hash (Element : Integer) return Ada.Containers.Hash_Type is
      (Ada.Containers.Hash_Type'Mod (Element));

   function Hash (Element : Long) return Ada.Containers.Hash_Type is
      (Ada.Containers.Hash_Type'Mod (Element));

   function Hash (Element : SU.Unbounded_String) return Ada.Containers.Hash_Type is
      (Ada.Strings.Unbounded.Hash (Element));

   function Hash (Element : Skill_Type_Access) return Ada.Containers.Hash_Type is
      (Ada.Containers.Hash_Type'Mod (Element.skill_id));

${
  var output = "";
  for (d ← IR) {
    output += s"""   function Hash (Element : ${d.getName}_Type_Access) return Ada.Containers.Hash_Type is\r\n      (Hash (Skill_Type_Access (Element)));\r\n\r\n"""
//    output += s"""   function Is_Equal (Left, Right : ${d.getName}_Type_Access) return Boolean is\r\n      (Skill_Type_Access (Left) = Skill_Type_Access (Right));\r\n\r\n"""
  }

  for (d ← IR) {
    d.getAllFields.filter { f ⇒ !f.isIgnored }.foreach({ f ⇒
      if (f.isConstant) {
        output += s"""   function Get_${f.getSkillName.capitalize} (Object : ${d.getName}_Type) return ${mapType(f.getType, d, f)} is (${f.constantValue});\r\n\r\n"""
      }
      else {
        output += s"""   function Get_${f.getSkillName.capitalize} (Object : ${d.getName}_Type) return ${mapType(f.getType, d, f)} is (Object.${f.getSkillName});\r\n"""
        output += s"""   procedure Set_${f.getSkillName.capitalize} (Object : in out ${d.getName}_Type; Value : ${mapType(f.getType, d, f)}) is
   begin
      Object.${f.getSkillName} := Value;
   end Set_${f.getSkillName.capitalize};\r\n\r\n"""
      }
    })
  }
  output.stripSuffix("\r\n\r\n");
}

   protected body Skill_State is

      -------------------
      --  STRING POOL  --
      -------------------
      function Get_String (Index : Positive) return String is
         (String_Pool.Element (Index));

      function Get_String (Index : Long) return String is
         (Get_String (Positive (Index)));

      function Get_String (Index : Positive) return SU.Unbounded_String is
         (SU.To_Unbounded_String (Get_String (Index)));

      function Get_String (Index : Long) return SU.Unbounded_String is
         (SU.To_Unbounded_String (Get_String (Positive (Index))));

      function Get_String_Index (Value : String) return Positive is
         Index : Natural := String_Pool.Reverse_Find_Index (Value);
         Skill_Unknown_String_Index : exception;
      begin
         if 0 = Index then
            raise Skill_Unknown_String_Index;
         end if;
         return Index;
      end Get_String_Index;

      function String_Pool_Size return Natural is
         (Natural (String_Pool.Length));

      procedure Put_String (Value : String; Safe : Boolean := False) is
         Append : Boolean := True;
      begin
         if True = Safe then
            declare
               Index : Natural := String_Pool.Reverse_Find_Index (Value);
            begin
               if 0 < Index or 0 = Value'Length then
                  Append := False;
               end if;
            end;
         end if;

         if True = Append then
            String_Pool.Append (Value);
         end if;
      end Put_String;

      --------------------
      --  STORAGE_POOL  --
      --------------------
      function Storage_Pool_Size (Type_Name : String) return Natural is
         (Natural (Get_Type (Type_Name).Storage_Pool.Length));

      --------------------------
      --  FIELD DECLARATIONS  --
      --------------------------
      function Field_Size (Type_Name : String) return Natural is
         (Natural (Types.Element (Type_Name).Fields.Length));

      function Has_Field (Type_Name, Field_Name : String) return Boolean is
         use Fields_Vector;

         Type_Declaration : Type_Information := Get_Type (Type_Name);
         Position : Cursor := Type_Declaration.Fields.First;
      begin
         while Position /= No_Element loop
            declare
               Index : Positive := To_Index (Position);
            begin
               if Field_Name = Type_Declaration.Fields.Element (Index).Name then
                  return True;
               end if;
            end;
            Next (Position);
         end loop;
         return False;
      end Has_Field;

      function Get_Field (Type_Name : String; Index : Positive) return Field_Information is
         X : Type_Information := Get_Type (Type_Name);
      begin
         return X.Fields.Element (Index);
      end Get_Field;

      function Get_Field (Type_Name : String; Index : Long) return Field_Information is
         (Get_Field (Type_Name, Positive (Index)));

      function Get_Field (Type_Name, Field_Name : String) return Field_Information is
         use Fields_Vector;

         X : Type_Information := Get_Type (Type_Name);
         Position : Cursor := X.Fields.First;

         Skill_Unexcepted_Field_Name : exception;
      begin
         while Position /= No_Element loop
            declare
               Index : Positive := To_Index (Position);
            begin
               if Field_Name = X.Fields.Element (Index).Name then
                  return X.Fields.Element (Index);
               end if;
            end;
            Next (Position);
         end loop;
         raise Skill_Unexcepted_Field_Name;
      end;

      procedure Put_Field (Type_Name : String; New_Field : Field_Information) is
         Type_Declaration : Type_Information := Types.Element (Type_Name);
      begin
         Type_Declaration.Fields.Append (New_Field);
      end Put_Field;

      --------------------------
      --  TYPES DECLARATIONS  --
      --------------------------
      function Type_Size return Natural is
         (Natural (Types.Length));

      function Has_Type (Name : String) return Boolean is
      begin
         return Types.Contains (Name);
      end Has_Type;

      function Get_Type (Name : String) return Type_Information is
         Skill_Unexcepted_Type_Name : exception;
      begin
         if not Has_Type (Name) then
            raise Skill_Unexcepted_Type_Name;
         end if;
         return Types.Element (Name);
      end Get_Type;

      procedure Put_Type (New_Type : Type_Information) is
      begin
         Types.Insert (New_Type.Name, New_Type);
      end Put_Type;

      function Get_Types return Types_Hash_Map.Map is
         (Types);

      -------------
      --  STATE  --
      -------------
      function Is_Consumed return Boolean is
         (Consumed = State);

      procedure Consume is
      begin
         State := Consumed;
      end Consume;

   end Skill_State;

end ${packagePrefix.capitalize};
""")

    out.close()
  }
}
