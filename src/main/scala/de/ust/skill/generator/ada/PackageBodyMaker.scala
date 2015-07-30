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

    // only create an adb, if it would contain any code
    if (IR.size > 0) {

      val out = open(s"""${packagePrefix}.adb""")

      out.write(s"""
with Ada.Unchecked_Conversion;

-- types generated out of the specification
package body ${PackagePrefix} is
${
        (for (t ← IR)
          yield s"""
   function To_${name(t)} (This : Skill.Types.Annotation) return ${name(t)}
   is
      function Convert is new Ada.Unchecked_Conversion (Skill.Types.Annotation, ${name(t)});
   begin
      return Convert (This);
   end To_${name(t)};

   -- Age fields
${
          (for (f ← t.getFields)
            yield s"""
   function Get_${name(f)} (This : access ${name(t)}_T'Class) return ${mapType(f.getType)}
   is
   begin
      return This.${name(f)};
   end Get_${name(f)};

   procedure Set_${name(f)} (This : access ${name(t)}_T'Class; V : ${mapType(f.getType)})
   is
   begin
      This.${name(f)} := V;
   end Set_${name(f)};
"""
          ).mkString
        }"""
        ).mkString
      }
end ${PackagePrefix};
""")

      //    out.write(s"""
      //package body ${packagePrefix.capitalize} is
      //
      //   function Hash (Element : Short_Short_Integer) return Ada.Containers.Hash_Type is
      //      (Ada.Containers.Hash_Type'Mod (Element));
      //
      //   function Hash (Element : Short) return Ada.Containers.Hash_Type is
      //      (Ada.Containers.Hash_Type'Mod (Element));
      //
      //   function Hash (Element : Integer) return Ada.Containers.Hash_Type is
      //      (Ada.Containers.Hash_Type'Mod (Element));
      //
      //   function Hash (Element : Long) return Ada.Containers.Hash_Type is
      //      (Ada.Containers.Hash_Type'Mod (Element));
      //
      //   function Hash (Element : String_Access) return Ada.Containers.Hash_Type is
      //      (Ada.Strings.Hash (Element.all));
      //
      //   function Equals (Left, Right : String_Access) return Boolean is
      //      (Left = Right or else ((Left /= null and Right /= null) and then Left.all = Right.all));
      //
      //   function Hash (Element : Skill_Type_Access) return Ada.Containers.Hash_Type is
      //      function Convert is new Ada.Unchecked_Conversion (Skill_Type_Access, i64);
      //   begin
      //      return Ada.Containers.Hash_Type'Mod (Convert (Element));
      //   end;
      //${
      //
      //      /**
      //       * Provides the hash function of every type.
      //       */
      //      (for (t ← IR)
      //        yield s"""
      //   function Hash (Element : ${name(t)}_Type_Access) return Ada.Containers.Hash_Type is
      //      (Hash (Skill_Type_Access (Element)));
      //""").mkString
      //    }
      //${
      //      /**
      //       * Provides the accessor functions to the fields of every type.
      //       */
      //      (for (
      //        d ← IR;
      //        f ← d.getAllFields if !f.isIgnored
      //      ) yield if (f.isConstant) {
      //        s"""
      //   function Get_${name(f)} (Object : ${name(d)}_Type) return ${mapType(f.getType, f.getDeclaredIn, f)} is
      //      (${
      //          f.getType.getName.getSkillName match {
      //            case "i8"  ⇒ f.constantValue.toByte
      //            case "i16" ⇒ f.constantValue.toShort
      //            case "i32" ⇒ f.constantValue.toInt
      //            case _     ⇒ f.constantValue
      //          }
      //        });
      //"""
      //      } else {
      //        s"""
      //   function Get_${name(f)} (Object : ${name(d)}_Type) return ${mapType(f.getType, f.getDeclaredIn, f)} is
      //      (Object.${escapedLonely(f.getSkillName)});
      //
      //   procedure Set_${name(f)} (
      //      Object : in out ${name(d)}_Type;
      //      Value  :        ${mapType(f.getType, f.getDeclaredIn, f)}
      //   ) is
      //   begin
      //      Object.${escapedLonely(f.getSkillName)} := Value;
      //   end Set_${name(f)};
      //"""
      //      }
      //      ).mkString
      //    }
      //
      //end ${packagePrefix.capitalize};
      //""")

      out.close()
    }
  }
}
