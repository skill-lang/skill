/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import scala.collection.JavaConversions._
import de.ust.skill.ir._

trait PackageSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}.ads""")

    out.write(s"""
with Ada.Containers.Doubly_Linked_Lists;
with Ada.Containers.Hashed_Maps;
with Ada.Containers.Hashed_Sets;
with Ada.Containers.Indefinite_Hashed_Maps;
with Ada.Containers.Indefinite_Vectors;
with Ada.Containers.Vectors;
with Ada.Strings.Hash;
with Ada.Strings.Unbounded;
with Ada.Strings.Unbounded.Hash;
with Ada.Tags;

with Ada.Text_IO;

package ${packagePrefix.capitalize} is

   package SU renames Ada.Strings.Unbounded;

   -------------
   --  TYPES  --
   -------------
   --  Short_Short_Integer
   subtype i8 is Short_Short_Integer'Base range -(2**7) .. +(2**7 - 1);

   --  Short_Integer
   subtype Short is Short_Integer'Base range -(2**15) .. +(2**15 - 1);
   subtype i16 is Short;

   --  Integer
   subtype i32 is Integer'Base range -(2**31) .. +(2**31 - 1);

   --  Long_Integer
   subtype Long is Long_Integer'Base range -(2**63) .. +(2**63 - 1);
   subtype i64 is Long;
   subtype v64 is Long;

   -------------
   --  SKILL  --
   -------------
   function Hash (Element : Short_Short_Integer) return Ada.Containers.Hash_Type;
   function Hash (Element : Short) return Ada.Containers.Hash_Type;
   function Hash (Element : Integer) return Ada.Containers.Hash_Type;
   function Hash (Element : Long) return Ada.Containers.Hash_Type;
   function Hash (Element : SU.Unbounded_String) return Ada.Containers.Hash_Type;
   function "=" (Left, Right : SU.Unbounded_String) return Boolean renames SU."=";

   type Skill_State is limited private;
   type Skill_Type is abstract tagged private;
   type Skill_Type_Access is access all Skill_Type'Class;
   function Hash (Element : Skill_Type_Access) return Ada.Containers.Hash_Type;

${
  var output = "";
  for (d ← IR) {
    output += s"""   type ${escaped(d.getName)}_Type is new Skill_Type with private;\r\n"""
    output += s"""   type ${escaped(d.getName)}_Type_Access is access all ${escaped(d.getName)}_Type;\r\n"""
    output += s"""   type ${escaped(d.getName)}_Type_Array is array (Natural range <>) of ${escaped(d.getName)}_Type_Access;\r\n"""
    output += s"""   type ${escaped(d.getName)}_Type_Accesses is access ${escaped(d.getName)}_Type_Array;\r\n"""
    output += s"""   function Hash (Element : ${escaped(d.getName)}_Type_Access) return Ada.Containers.Hash_Type;\r\n\r\n"""
  }

  output.stripSuffix("\r\n")

  for (d ← IR) {
    d.getFields.filter({ f ⇒ !f.isIgnored }).foreach({ f ⇒
      f.getType match {
        case t: ConstantLengthArrayType ⇒
          output += s"   type ${mapType(f.getType, d, f)} is array (1 .. ${t.getLength}) of ${mapType(t.getBaseType, d, f)};\r\n"
        case t: VariableLengthArrayType ⇒
          output += s"   package ${mapType(f.getType, d, f).stripSuffix(".Vector")} is new Ada.Containers.Vectors (Positive, ${mapType(t.getBaseType, d, f)});\r\n"
        case t: ListType ⇒
          output += s"""   package ${mapType(f.getType, d, f).stripSuffix(".List")} is new Ada.Containers.Doubly_Linked_Lists (${mapType(t.getBaseType, d, f)}, "=");\r\n"""
        case t: SetType ⇒
          output += s"""   package ${mapType(f.getType, d, f).stripSuffix(".Set")} is new Ada.Containers.Hashed_Sets (${mapType(t.getBaseType, d, f)}, Hash, "=");\r\n"""
        case t: MapType ⇒ {
          val types = t.getBaseTypes().reverse
          types.slice(0, types.length-1).zipWithIndex.foreach({ case (t, i) =>
            val x = {
              if (0 == i)
                mapType(types.get(i), d, f)
              else
                s"""${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length-i}.Map"""
            }

            output += s"""   package ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length-(i+1)} is new Ada.Containers.Hashed_Maps (${mapType(types.get(i+1), d, f)}, ${x}, Hash, "=");\r\n"""
            output += s"""   function "=" (Left, Right : ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length-(i+1)}.Map) return Boolean renames ${mapType(f.getType, d, f).stripSuffix(".Map")}_${types.length-(i+1)}."=";\r\n""";
          })
          output += s"""   package ${mapType(f.getType, d, f).stripSuffix(".Map")} renames ${mapType(f.getType, d, f).stripSuffix(".Map")}_1;\r\n"""
        }
        case _ ⇒ null
      }
    })
  }

  for (d ← IR) {
    d.getAllFields.filter({ f ⇒ !f.isIgnored }).foreach({ f ⇒
      output += s"""   function Get_${f.getName.capitalize} (Object : ${escaped(d.getName)}_Type) return ${mapType(f.getType, d, f)};\r\n"""
      if (!f.isConstant)
        output += s"""   procedure Set_${f.getName.capitalize} (Object : in out ${escaped(d.getName)}_Type; Value : ${mapType(f.getType, d, f)});\r\n"""
    })
  }
  output
}
private

   type Skill_States is (Unused, Append, Create, Read, Write);
   Skill_State_Error : exception;

   type Skill_Type is abstract tagged
      record
         skill_id : Natural;
      end record;

${
  var output = "";
  for (d ← IR) {
    val superType = if (d.getSuperType == null) "Skill" else d.getSuperType.getName
    output += s"""   type ${escaped(d.getName)}_Type is new ${superType}_Type with\r\n      record\r\n"""
    val fields = d.getFields.filter({ f ⇒ !f.isConstant && !f.isIgnored })
    output += fields.map({ f ⇒
      var comment = "";
      if (f.isAuto()) comment = "  --  auto aka not serialized"
      s"""         ${f.getSkillName} : ${mapType(f.getType, d, f)};${comment}"""
    }).mkString("\r\n")
    if (fields.length <= 0) output += s"""         null;"""
    output += s"""\r\n      end record;\r\n\r\n"""
  }
  output.stripSuffix("\r\n")
}
   ------------------
   --  STRING POOL --
   ------------------
   package String_Pool_Vector is new Ada.Containers.Indefinite_Vectors (Positive, String);
   type String_Pool_Access is access String_Pool_Vector.Vector;

   --------------------
   --  STORAGE POOL  --
   --------------------
   package Storage_Pool_Vector is new Ada.Containers.Vectors (Positive, Skill_Type_Access);

   --------------------------
   --  FIELD DECLARATIONS  --
   --------------------------
   package Base_Types_Vector is new Ada.Containers.Vectors (Positive, Long);
   type Field_Declaration (Size : Positive) is
      record
         Name : String (1 .. Size);
         F_Type : Long;
         Constant_Value : Long;
         Constant_Array_Length : Long;
         Base_Types : Base_Types_Vector.Vector;
         Known : Boolean;
         Written : Boolean;
      end record;
   type Field_Information is access Field_Declaration;

   package Fields_Vector is new Ada.Containers.Vectors (Positive, Field_Information);

   -------------------------
   --  TYPE DECLARATIONS  --
   -------------------------
   type Type_Declaration (Type_Size : Positive; Super_Size : Natural) is
      record
         id : Long;
         Name : String (1 .. Type_Size);
         Super_Name : String (1 .. Super_Size);
         spsi : Positive;
         lbpsi : Natural;
         Fields : Fields_Vector.Vector;
         Storage_Pool : Storage_Pool_Vector.Vector;
         Known : Boolean;
         Written : Boolean;
      end record;
   type Type_Information is access Type_Declaration;

   package Types_Hash_Map is new Ada.Containers.Indefinite_Hashed_Maps
      (String, Type_Information, Ada.Strings.Hash, "=");
   type Types_Hash_Map_Access is access Types_Hash_Map.Map;

   -------------------
   --  SKILL STATE  --
   -------------------
   type Skill_State is
      record
         File_Name : SU.Unbounded_String;
         State : Skill_States := Unused;
         String_Pool : String_Pool_Access := new String_Pool_Vector.Vector;
         Types : Types_Hash_Map_Access := new Types_Hash_Map.Map;
      end record;

end ${packagePrefix.capitalize};
""")

    out.close()
  }
}
