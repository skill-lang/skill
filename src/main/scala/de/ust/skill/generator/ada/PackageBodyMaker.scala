/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
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

      /**
       * Provides the hash function of every type.
       */
      (for (t ← IR)
        yield s"""
   function Hash (Element : ${name(t)}_Type_Access) return Ada.Containers.Hash_Type is
      (Hash (Skill_Type_Access (Element)));
""").mkString
    }
${
      /**
       * Provides the accessor functions to the fields of every type.
       */
      (for (
        d ← IR;
        f ← d.getAllFields if !f.isIgnored
      ) yield if (f.isConstant) {
        s"""
   function Get_${name(f)} (Object : ${name(d)}_Type) return ${mapType(f.getType, f.getDeclaredIn, f)} is
      (${f.constantValue});
"""
      } else {
        s"""
   function Get_${name(f)} (Object : ${name(d)}_Type) return ${mapType(f.getType, f.getDeclaredIn, f)} is
      (Object.${f.getSkillName});

   procedure Set_${name(f)} (
      Object : in out ${d.getName.ada}_Type;
      Value  :        ${mapType(f.getType, f.getDeclaredIn, f)}
   ) is
   begin
      Object.${f.getSkillName} := Value;
   end Set_${name(f)};
"""
      }
      ).mkString
    }

end ${packagePrefix.capitalize};
""")

    out.close()
  }
}
