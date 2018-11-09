/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import scala.collection.JavaConversions._
import de.ust.skill.ir.UserType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.MapType
import de.ust.skill.ir.Type
import de.ust.skill.ir.Field
import de.ust.skill.ir.View

trait PackageBodyMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    // only create an adb, if it would contain any code
    if (IR.size > 0) {

      val out = files.open(s"""${packagePrefix}.adb""")

      out.write(s"""
with Ada.Unchecked_Conversion;

with Interfaces;

with Skill.Field_Types.Builtin.String_Type_P;
with ${PackagePrefix}.Internal_Skill_Names;
${
        (
          for (t ← IR) yield s"""
with $poolsPackage.${name(t)}_P;
with $PackagePrefix.Known_Field_${name(t)};"""
        ).mkString
      }


-- types generated out of the specification
package body ${PackagePrefix} is
${
        (for (t ← IR)
          yield s"""
   overriding
   function Skill_Name (This : access constant ${name(t)}_T) return Standard.Skill.Types.String_Access is
     (${internalSkillName(t)});

   function To_${name(t)} (This : Skill.Types.Annotation) return ${name(t)} is
      function Convert is new Ada.Unchecked_Conversion (Skill.Types.Annotation, ${name(t)});
   begin
      return Convert (This);
   end To_${name(t)};

   function Unchecked_Access (This : access ${name(t)}_T) return ${name(t)} is
      type ZT is access all ${name(t)}_T;
      function Convert is new Ada.Unchecked_Conversion (ZT, ${name(t)});
   begin
      return Convert (ZT (This));
   end Unchecked_Access;${
          // type conversions to super types
          var r = new StringBuilder
          var s = t.getSuperType
          if (null != s) {
            r ++= s"""
   function To_${name(s)} (This : access ${name(t)}_T'Class) return ${name(s)} is
      type T is access all ${name(t)}_T;
      function Convert is new Ada.Unchecked_Conversion (T, ${name(s)});
   begin
      return Convert (T (This));
   end To_${name(s)};
"""
            s = s.getSuperType
          }

          // type conversions to subtypes
          def asSub(sub : UserType) {
            r ++= s"""
   function As_${name(sub)} (This : access ${name(t)}_T'Class) return ${name(sub)} is
      type T is access all ${name(t)}_T;
      function Convert is new Ada.Unchecked_Conversion (T, ${name(sub)});
   begin
      return Convert (T (This));
   end As_${name(sub)};
"""
            sub.getSubTypes.foreach(asSub)
          }

          // only base types get unsafe conversions, as they are inherited to subtypes and completely identical
          if (null == t.getSuperType)
            t.getSubTypes.foreach(asSub)

          r.toString
        }

   function Dynamic_${name(t)} (This : access ${name(t)}_T) return ${name(t)}_Dyn is
      function Convert is new Ada.Unchecked_Conversion (Skill.Types.Annotation, ${name(t)}_Dyn);
   begin
      return Convert (This.To_Annotation);
   end Dynamic_${name(t)};${
          if (visited.contains(t.getSkillName)) s"""
   procedure Acc (This : access ${name(t)}_T; V : access Abstract_Visitor'Class) is
   begin
      V.Visit (${name(t)} (This));
   end Acc;
"""
          else ""
        }

   -- reflective getter
   function Reflective_Get
     (This : not null access ${name(t)}_T;
      F : Skill.Field_Declarations.Field_Declaration) return Skill.Types.Box is
   begin${
          (
            for (f ← t.getAllFields if !f.isConstant) yield s"""
      if F.all in Standard.${PackagePrefix}.Known_Field_${name(f.getDeclaredIn)}.Known_Field_${fieldName(f.getDeclaredIn, f)}_T then
         return ${boxCall(f.getType)} (This.${name(f)});
      end if;"""
          ).mkString
        }

      return This.To_Annotation.Reflective_Get (F);
   end Reflective_Get;

   -- reflective setter
   procedure Reflective_Set
     (This : not null access ${name(t)}_T;
      F : Skill.Field_Declarations.Field_Declaration;
      V : Skill.Types.Box) is
   begin${
          (
            for (f ← t.getAllFields if !f.isConstant) yield s"""
      if F.all in Standard.${PackagePrefix}.Known_Field_${name(f.getDeclaredIn)}.Known_Field_${fieldName(f.getDeclaredIn, f)}_T then
         This.${name(f)} := ${unboxCall(f.getType)} (V);
         return;
      end if;"""
          ).mkString
        }

      This.To_Annotation.Reflective_Set (F, V);
   end Reflective_Set;

   -- ${name(t)} fields
${
          (for (f ← t.getFields)
            yield s"""
   function Get_${name(f)} (This : not null access constant ${name(t)}_T'Class) return ${mapType(f)}
   is
      use Interfaces;
   begin
      return ${
            if (f.isConstant()) mapConstantValue(f)
            else s"This.${name(f)}"
          };
   end Get_${name(f)};

   procedure Set_${name(f)} (This : not null access ${name(t)}_T'Class; V : ${mapType(f)})
   is
   begin
      ${
            // we wont raise an exception, because we rather want the code to be deleted by the compiler
            if (f.isConstant()) "null;"
            else s"This.${name(f)} := V;"
          }
   end Set_${name(f)};
${
            f.getType match {
              case ft : SingleBaseTypeContainer ⇒ s"""
   function Box_${name(f)} (This : access ${name(t)}_T'Class; V : ${mapType(ft.getBaseType)}) return Skill.Types.Box is
      pragma Warnings (Off);
      function Convert is new Ada.Unchecked_Conversion (${mapType(ft.getBaseType)}, Skill.Types.Box);
   begin
      return Convert (V);
   end Box_${name(f)};

   function Unbox_${name(f)} (This : access ${name(t)}_T'Class; V : Skill.Types.Box) return ${mapType(ft.getBaseType)} is
      pragma Warnings (Off);
      function Convert is new Ada.Unchecked_Conversion (Skill.Types.Box, ${mapType(ft.getBaseType)});
   begin
      return Convert (V);
   end Unbox_${name(f)};
"""
              case ft : MapType ⇒

                mapBoxing(t, f, "", ft.getBaseTypes.toList).mkString

              case _ ⇒ ""
            }
          }"""
          ).mkString
        }"""
        ).mkString
      }${
        // views
        (for (
          t ← IR;
          v ← t.getViews
        ) yield {
          s"""
${comment(v)}procedure Set_${name(v)} (This : not null access ${name(t)}_T'Class; V : ${mapType(v.getType)})
   is
   begin
      Set_${name(v.getTarget)} (This.To_${v.getTarget.getDeclaredIn.getName.ada}, V.To_${
            v.getTarget match {
              case f : View  ⇒ f.getType.getName.ada
              case f : Field ⇒ f.getType.getName.ada
            }
          });
   end Set_${name(v)};"""
        }).mkString
      }
end ${PackagePrefix};
""")

      out.close()
    }
  }

  private final def mapBoxing(t : Type, f : Field, Vs : String, ts : List[Type]) : Seq[String] = {
    val k : Type = ts.head

    Seq(s"""
   function Box_${name(f)}_${Vs}K (This : access ${name(t)}_T'Class; V : ${mapType(k)}) return Skill.Types.Box is
      pragma Warnings (Off);
      function Convert is new Ada.Unchecked_Conversion (${mapType(k)}, Skill.Types.Box);
   begin
      return Convert (V);
   end Box_${name(f)}_${Vs}K;

   function Unbox_${name(f)}_${Vs}K (This : access ${name(t)}_T'Class; V : Skill.Types.Box) return ${mapType(k)} is
      pragma Warnings (Off);
      function Convert is new Ada.Unchecked_Conversion (Skill.Types.Box, ${mapType(k)});
   begin
      return Convert (V);
   end Unbox_${name(f)}_${Vs}K;
""") ++
      (ts.tail match {
        case (v : Type) :: Nil ⇒ Seq(s"""
   function Box_${name(f)}_${Vs}V (This : access ${name(t)}_T'Class; V : ${mapType(v)}) return Skill.Types.Box is
      pragma Warnings (Off);
      function Convert is new Ada.Unchecked_Conversion (${mapType(v)}, Skill.Types.Box);
   begin
      return Convert (V);
   end Box_${name(f)}_${Vs}V;

   function Unbox_${name(f)}_${Vs}V (This : access ${name(t)}_T'Class; V : Skill.Types.Box) return ${mapType(v)} is
      pragma Warnings (Off);
      function Convert is new Ada.Unchecked_Conversion (Skill.Types.Box, ${mapType(v)});
   begin
      return Convert (V);
   end Unbox_${name(f)}_${Vs}V;
""")
        case vs : List[Type] ⇒ Seq(s"""
   function Box_${name(f)}_${Vs}V (This : access ${name(t)}_T'Class; V : access Skill.Containers.Boxed_Map_T'Class) return Skill.Types.Box is
      pragma Warnings (Off);
      function Convert is new Ada.Unchecked_Conversion (Skill.Types.Boxed_Map, Skill.Types.Box);
   begin
      return Convert (V);
   end Box_${name(f)}_${Vs}V;

   function Unbox_${name(f)}_${Vs}V (This : access ${name(t)}_T'Class; V : Skill.Types.Box) return Skill.Types.Boxed_Map is
      pragma Warnings (Off);
      function Convert is new Ada.Unchecked_Conversion (Skill.Types.Box, Skill.Types.Boxed_Map);
   begin
      return Convert (V);
   end Unbox_${name(f)}_${Vs}V;
""") ++ mapBoxing(t, f, Vs + "V", vs)

      })
  }
}
