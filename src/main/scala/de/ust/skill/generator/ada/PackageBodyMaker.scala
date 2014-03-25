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
    output += s"""   function Hash (Element : ${escaped(d.getName)}_Type_Access) return Ada.Containers.Hash_Type is\r\n      (Hash (Skill_Type_Access (Element)));\r\n\r\n"""
  }

  for (d ← IR) {
    d.getAllFields.filter { f ⇒ !f.isIgnored }.foreach({ f ⇒
      if (f.isConstant) {
        output += s"""   function Get_${f.getName.capitalize} (Object : ${escaped(d.getName)}_Type) return ${mapType(f.getType, d, f)} is (${f.constantValue});\r\n\r\n"""
      }
      else {
        output += s"""   function Get_${f.getName.capitalize} (Object : ${escaped(d.getName)}_Type) return ${mapType(f.getType, d, f)} is (Object.${f.getSkillName});\r\n"""
        output += s"""   procedure Set_${f.getName.capitalize} (Object : in out ${escaped(d.getName)}_Type; Value : ${mapType(f.getType, d, f)}) is
   begin
      Object.${f.getSkillName} := Value;
   end Set_${f.getName.capitalize};\r\n\r\n"""
      }
    })
  }
  output.stripSuffix("\r\n\r\n");
}

end ${packagePrefix.capitalize};
""")

    out.close()
  }
}
