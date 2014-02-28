/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import java.io.PrintWriter
import scala.collection.JavaConversions._

trait PackageBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}.adb""")

    out.write(s"""
package body ${packagePrefix.capitalize} is

${
  var output = "";
  for (t ← IR) {
    val name = t.getName
    output += t.getAllFields.filter { f ⇒ f.isConstant && !f.isIgnored }.map({ f ⇒
      s"""   function %s (Object : %s_Type) return %s is (%s);""".format(f.getName, name, mapType(f.getType), f.constantValue)
    }).mkString("\r\n")
  }
  output;
}

   protected body Skill_State is

      -------------------
      --  STRING POOL  --
      -------------------
      function Get_String (Position : Long) return String is (String_Pool.Element (Positive (Position)));

      procedure Put_String (Value : String) is
      begin
         String_Pool.Append (Value);
      end Put_String;

      --------------------
      --  STORAGE_POOL  --
      --------------------
      function Get_Object (Type_Name : String; Position : Positive) return Skill_Type_Access is
         (Get_Type (Type_Name).Storage_Pool.Element (Position));

      function Storage_Size (Type_Name : String) return Natural is
         (Natural (Get_Type (Type_Name).Storage_Pool.Length));

      procedure Put_Object (Type_Name : String; New_Object : Skill_Type_Access) is
      begin
         Get_Type (Type_Name).Storage_Pool.Append (New_Object);
      end Put_Object;

      procedure Replace_Object (Type_Name : String; Position : Positive; New_Object : Skill_Type_Access) is
      begin
         Get_Type (Type_Name).Storage_Pool.Replace_Element (Position, New_Object);
      end Replace_Object;

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
