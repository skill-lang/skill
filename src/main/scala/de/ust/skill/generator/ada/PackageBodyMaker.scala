/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import scala.collection.JavaConversions._

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

   function Hash (Element : String_Access) return Ada.Containers.Hash_Type is
      (Ada.Strings.Hash (Element.all));

   function "=" (Left, Right : String_Access) return Boolean is
      (Left.all = Right.all);

   function Hash (Element : Skill_Type_Access) return Ada.Containers.Hash_Type is
      function Convert is new Ada.Unchecked_Conversion (Skill_Type_Access, i64);
   begin
      return Ada.Containers.Hash_Type'Mod (Convert (Element));
   end;

${
  var output = "";

  /**
   * Provides the hash function of every type.
   */
  for (d ← IR) {
    output += s"""   function Hash (Element : ${escaped(d.getName)}_Type_Access) return Ada.Containers.Hash_Type is\r\n      (Hash (Skill_Type_Access (Element)));\r\n\r\n"""
  }

  /**
   * Provides the accessor functions to the fields of every type.
   */
  for (d ← IR) {
    d.getAllFields.filter { f ⇒ !f.isIgnored }.foreach({ f ⇒
      if (f.isConstant) {
        output += s"""   function Get_${f.getName.capitalize} (Object : ${escaped(d.getName)}_Type) return ${mapType(f.getType, d, f)} is\r\n      (${f.constantValue});\r\n\r\n"""
      }
      else {
        output += s"""   function Get_${f.getName.capitalize} (Object : ${escaped(d.getName)}_Type) return ${mapType(f.getType, d, f)} is\r\n      (Object.${f.getSkillName});\r\n\r\n"""
        output += s"""   procedure Set_${f.getName.capitalize} (
      Object : in out ${escaped(d.getName)}_Type;
      Value  :        ${mapType(f.getType, d, f)}
   ) is
   begin
      Object.${f.getSkillName} := Value;
   end Set_${f.getName.capitalize};\r\n\r\n"""
      }
    })
  }
  output.stripLineEnd.stripLineEnd
}

end ${packagePrefix.capitalize};
""")

    out.close()
  }
}
