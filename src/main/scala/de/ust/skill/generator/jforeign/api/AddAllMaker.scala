/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.jforeign

import javassist.NotFoundException
import scala.collection.JavaConversions._
import de.ust.skill.ir.UserType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.Type
import de.ust.skill.ir.MapType

trait AddAllMaker extends GeneralOutputMaker {
    def writeAddAllForMaps(baseTypes : List[Type]): String = baseTypes match {
      case head :: Nil ⇒ head match {
          case ut: UserType ⇒ s"v1.selfAdd(sf);"
          case gt: GroundType ⇒ gt.getSkillName match {
            case "string" ⇒ s"sf.Strings().add(v1);"
            case _ ⇒ "// no need to add ground types"
          }
        }

      case head :: rest ⇒ s""".forEach( (k${rest.size}, v${rest.size}) -> {
        ${head match {
          case ut: UserType ⇒ s"k${rest.size}.selfAdd(sf);"
          case gt: GroundType ⇒ gt.getSkillName match {
            case "string" ⇒ s"sf.Strings().add(k${rest.size});"
            case _ ⇒ "// no need to add ground types"
          }
        }}
        ${if (rest.size > 1) s"v${rest.size}" else ""}${writeAddAllForMaps(rest)}
    });"""
      case Nil ⇒ throw new RuntimeException("Map needs at least two types");
    }

  abstract override def make {
    super.make

    val out = open("api/AddAll.java")

      out.write(s"""
package ${packageName}.api;
import de.ust.skill.common.jforeign.internal.SkillObject;

public class AddAll { 

${ (for (t ← IR) yield s"""

    public static void add(SkillFile sf, ${mapType(t, false)} x) {
        if (x.getSkillID() == -9) {
            return;
        }
        x.setSkillID(-9);
        sf.${name(t)}s().add(x);
${
      t.getAllFields.map { f ⇒
        f.getType match {
          case ut: UserType ⇒ s"""
        if (x.${getterOrFieldAccess(t, f)} != null) {
            x.${getterOrFieldAccess(t, f)}.selfAdd(sf);
        }"""
          case gt: GroundType ⇒ if (gt.getSkillName.equals("string")) s"""
        sf.Strings().add(x.${getterOrFieldAccess(t, f)});"""
          else ""
          case lt: SingleBaseTypeContainer ⇒ lt.getBaseType match {
            case gt: GroundType ⇒ if (gt.getSkillName.equals("string")) s"""
        if (x.${getterOrFieldAccess(t, f)} != null) {
            x.${getterOrFieldAccess(t, f)}.forEach(e -> sf.Strings().add(e));
        }"""
            else ""
            case ut: UserType ⇒ s"""
        if (x.${getterOrFieldAccess(t, f)} != null) {
            x.${getterOrFieldAccess(t, f)}.forEach(e -> e.selfAdd(sf));
        }"""
          }
          case mt: MapType ⇒ s"""
        if (x.${getterOrFieldAccess(t, f)} != null) {
            x.${getterOrFieldAccess(t, f)}${writeAddAllForMaps(mt.getBaseTypes.toList)}
        }"""
          case x: Type ⇒ s"""
        // cannot addAll a ${x} because I don't know this type"""
        }
      }.mkString("")
}
    }
""").mkString("") }
}""");
      out.close();
  }
}
