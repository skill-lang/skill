/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import de.ust.skill.ir._
import scala.collection.JavaConversions._

trait PackageSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}.ads""")

    out.write(s"""
with Ada.Containers;
with Ada.Unchecked_Conversion;

with Skill.Containers;
with Skill.Containers.Arrays;
with Skill.Containers.Maps;
with Skill.Containers.Sets;
with Skill.Equals;
with Skill.Hashes;
with Skill.Types;
with Skill.Field_Declarations;

-- types generated out of the specification
package ${PackagePrefix} is
   pragma Warnings (Off);
   use Skill.Equals;
   use Skill.Hashes;
   use type Skill.Types.V64;
   use type Skill.Types.String_Access;
${
      (for (t ← IR)
        yield s"""
   type ${name(t)}_T is new ${
        if (null == t.getSuperType) "Skill.Types.Skill_Object"
        else name(t.getSuperType) + "_T"
      } with private;

${comment(t)}type ${name(t)} is access all ${name(t)}_T;
   type ${name(t)}_Dyn is access all ${name(t)}_T'Class;
   function Hash is new Ada.Unchecked_Conversion(${name(t)}, Ada.Containers.Hash_Type);
   function Equals (ZA, ZB : ${name(t)}) return Boolean is (ZA = ZB);
""").mkString
    }${
      // predefine known containers
      (for (t ← IR; f ← t.getFields if f.getType.isInstanceOf[ContainerType]) yield f.getType match {
        case t : SetType ⇒ Set(s"""
   package ${simpleTypePackage(f.getType)} is new Standard.Skill.Containers.Sets(${mapType(t.getBaseType)});""")
        case t : SingleBaseTypeContainer ⇒ Set(s"""
   package ${simpleTypePackage(f.getType)} is new Standard.Skill.Containers.Arrays(${mapType(t.getBaseType)});""")
        case t : MapType ⇒ makeMapDeclarations(t.getBaseTypes.to)
      }).toSet.flatten.toArray.sortBy(_.size).mkString
    }
${
      (for (t ← IR)
        yield s"""
   overriding
   function Skill_Name (This : access ${name(t)}_T) return Standard.Skill.Types.String_Access;

   -- ${name(t)} type conversions
   function To_${name(t)} (This : Skill.Types.Annotation) return ${name(t)}
     with Inline, Pure_Function;

   function Unchecked_Access (This : access ${name(t)}_T) return ${name(t)};
   pragma Inline (Unchecked_Access);${
        // type conversions to super types
        var r = new StringBuilder
        var s = t.getSuperType
        if (null != s) {
          r ++= s"""
   function To_${name(s)} (This : access ${name(t)}_T'Class) return ${name(s)};
   pragma Inline (To_${name(s)});
"""
          s = s.getSuperType
        }

        // type conversions to subtypes
        def asSub(sub : UserType) {
          r ++= s"""
   function As_${name(sub)} (This : access ${name(t)}_T'Class) return ${name(sub)};
   pragma Inline (As_${name(sub)});
"""
          sub.getSubTypes.foreach(asSub)
        }

        t.getSubTypes.foreach(asSub)

        r.toString
      }

   function Dynamic_${name(t)} (This : access ${name(t)}_T) return ${name(t)}_Dyn;
   pragma Inline (Dynamic_${name(t)});

   -- reflective getter
   function Reflective_Get
     (This : not null access ${name(t)}_T;
      F : Skill.Field_Declarations.Field_Declaration) return Skill.Types.Box;

   -- reflective setter
   procedure Reflective_Set
     (This : not null access ${name(t)}_T;
      F : Skill.Field_Declarations.Field_Declaration;
      V : Skill.Types.Box);

   -- ${name(t)} fields
${
        (for (f ← t.getFields)
          yield s"""
${comment(f)}function Get_${name(f)} (This : not null access ${name(t)}_T'Class) return ${mapType(f)};
   pragma Inline (Get_${name(f)});

${comment(f)}procedure Set_${name(f)} (This : not null access ${name(t)}_T'Class; V : ${mapType(f)});
   pragma Inline (Set_${name(f)});
${
          f.getType match {
            case ft : SingleBaseTypeContainer ⇒ s"""
   function Box_${name(f)} (This : access ${name(t)}_T'Class; V : ${mapType(ft.getBaseType)}) return Skill.Types.Box;
   function Unbox_${name(f)} (This : access ${name(t)}_T'Class; V : Skill.Types.Box) return ${mapType(ft.getBaseType)};"""

            case ft : MapType ⇒
              def boxing(Vs : String, ts : List[Type]) : Seq[String] = {
                val k = ts.head
                Seq(s"""
   function Box_${name(f)}_${Vs}K (This : access ${name(t)}_T'Class; V : ${mapType(k)}) return Skill.Types.Box;
   function Unbox_${name(f)}_${Vs}K (This : access ${name(t)}_T'Class; V : Skill.Types.Box) return ${mapType(k)};
""") ++
                  (ts.tail match {
                    case v :: Nil ⇒ Seq(s"""
   function Box_${name(f)}_${Vs}V (This : access ${name(t)}_T'Class; V : ${mapType(v)}) return Skill.Types.Box;
   function Unbox_${name(f)}_${Vs}V (This : access ${name(t)}_T'Class; V : Skill.Types.Box) return ${mapType(v)};
""")
                    case vs : List[Type] ⇒ Seq(s"""
   function Box_${name(f)}_${Vs}V (This : access ${name(t)}_T'Class; V : access Skill.Containers.Boxed_Map_T'Class) return Skill.Types.Box;
   function Unbox_${name(f)}_${Vs}V (This : access ${name(t)}_T'Class; V : Skill.Types.Box) return Skill.Types.Boxed_Map;
""") ++ boxing(Vs + "V", vs)
                  })
              }

              boxing("", ft.getBaseTypes.toList).mkString

            case _ ⇒ ""
          }
        }"""
        ).mkString
      }
""").mkString
    }
   -----------
   -- views --
   -----------
${
      // views
      (for (
        t ← IR;
        v ← t.getViews
      ) yield {
        s"""
${comment(v)}function View_${name(v)} (This : not null access ${name(t)}_T) return ${mapType(v.getType)} is
   (This.Get_${name(v.getTarget)}.As_${v.getType.getName.ada()});
   pragma Inline (View_${name(v)});"""
      }).mkString
    }
private
${
      (for (t ← IR)
        yield s"""
   type ${name(t)}_T is new ${
        if (null == t.getSuperType) "Skill.Types.Skill_Object"
        else name(t.getSuperType) + "_T"
      } with record${
        if (t.getFields.isEmpty())
          """
      null;"""
        else
          (for (f ← t.getFields)
            yield s"""
      ${name(f)} : ${mapType(f)};"""
          ).mkString
      }
   end record;
""").mkString
    }
end ${PackagePrefix};
""")
    out.close()
  }

  private final def mapName(ts : List[Type]) : String = ts.map { x ⇒ escaped(x.getSkillName) }.mkString("Skill_Map_", "_", "");

  private final def makeMapDeclarations(ts : List[Type]) : Set[String] = {
    val head = if (ts.size > 2) makeMapDeclarations(ts.tail) else Set[String]()
    head + s"""
   package ${mapName(ts)} is new Standard.Skill.Containers.Maps(${mapType(ts.head)}, ${
      if (ts.size > 2) mapName(ts.tail) + ".Ref"
      else mapType(ts(1))
    });
   use type ${mapName(ts)}.Ref;"""
  }
}
