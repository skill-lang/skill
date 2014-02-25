/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import java.io.PrintWriter

trait PackageBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize} is

   protected body Skill_State is

      -------------------
      --  STRING POOL  --
      -------------------
      function Get_String (Position : Long) return String is
         (String_Pool.Element (Positive (Position)));

      procedure Put_String (Value : String) is
      begin
         String_Pool.Append (Value);
      end Put_String;

      --------------------
      --  STORAGE_POOL  --
      --------------------
      function Get_Instance (Type_Name : String; Position : Positive) return Instance'Class is
         A_Type : Type_Information := Get_Type (Type_Name);
      begin
         return A_Type.Storage_Pool.Element (Position);
      end Get_Instance;

      function Storage_Size (Type_Name : String) return Natural is
         A_Type : Type_Information := Get_Type (Type_Name);
      begin
         return Natural (A_Type.Storage_Pool.Length);
      end;

      procedure Put_Instance (Type_Name : String; New_Instance : Instance'Class) is
         A_Type : Type_Information := Get_Type (Type_Name);
      begin
         A_Type.Storage_Pool.Append (New_Instance);
      end Put_Instance;

      procedure Replace_Instance (Type_Name : String; Position : Positive; New_Instance : Instance'Class) is
         A_Type : Type_Information := Get_Type (Type_Name);
      begin
         A_Type.Storage_Pool.Replace_Element (Position, New_Instance);
      end Replace_Instance;

      --------------------------
      --  FIELD DECLARATIONS  --
      --------------------------
      function Known_Fields (Name : String) return Long is
         (Long (Types.Element (Name).Fields.Length));

      function Get_Field (Type_Name : String; Position : Long) return Field_Information is
         X : Type_Information := Get_Type (Type_Name);
      begin
         return X.Fields.Element (Positive (Position));
      end Get_Field;

      procedure Put_Field (Type_Name : String; New_Field : Field_Information) is
         Type_Declaration : Type_Information := Types.Element (Type_Name);
      begin
         Type_Declaration.Fields.Append (New_Field);
      end Put_Field;

      --------------------------
      --  TYPES DECLARATIONS  --
      --------------------------
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

   end Skill_State;

end ${packagePrefix.capitalize};
""")

    out.close()
  }
}
