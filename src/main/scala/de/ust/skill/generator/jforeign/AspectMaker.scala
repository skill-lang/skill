/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.jforeign

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir.GroundType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import javassist.NotFoundException

trait AspectMaker extends GeneralOutputMaker {

  private def makeMapType(types : List[Type], actualMapType : String) : String = {
    val last :: rest = types.reverse.toList
    rest.foldLeft(mapType(last, true))((acc, curr) ⇒ s"$actualMapType<${mapType(curr, true)}, ${acc}>")
  }

  private def makeAddCode(typ : Type, ref : String) : String = typ match {
    case ut: UserType ⇒ s"$ref.selfAdd(sf);"
    case gt: GroundType ⇒ gt.getSkillName match {
      case "string" ⇒ s"sf.Strings().add($ref);"
      case _ ⇒ s"// no need to add ground type: ${mapType(gt, false)}"
    }
  }

  private def writeAddMaps(baseTypes : List[Type], depth : Int, mapRef : String, actualMapType: String, indent : String): String = baseTypes match {
    case Nil ⇒ throw new RuntimeException("Empty types list of map type")
    case head :: Nil ⇒ s"""$indent${makeAddCode(head, s"e${depth - 1}.getValue()")}"""
    case head :: rest ⇒
s"""${indent}for (java.util.Map.Entry<${mapType(head, true)}, ${makeMapType(rest, actualMapType)}> e$depth : $mapRef.entrySet()) {
$indent    ${makeAddCode(head, s"e$depth.getKey()")}
${writeAddMaps(rest, depth + 1, s"e$depth.getValue()", actualMapType, indent + "    ")}
$indent}
"""
  }

  abstract override def make {
    super.make

    for (t ← IR) {
      val out = files.open(name(t) + "Aspects.java")

      out.write(s"""
package ${packageName};
import ${packagePrefix}api.SkillFile;
import de.ust.skill.common.jforeign.internal.SkillObject;
import ${t.getName.getPackagePath}.${t.getName};


/**
 * Aspects for ${t.getName}.
 *
 * @author Constantin Weißer
 */
public aspect ${name(t)}Aspects {

${
  if (t.getSuperType() == null) {
s"""    // add skillID to ${name(t)}
    public long ${t.getName}.skillID;
    // getter and setter for skillID
    public long ${t.getName}.getSkillID() { return this.skillID; }
    public void ${t.getName}.setSkillID(long skillID) { this.skillID = skillID; }"""
  } else ""
}
    // Access to skillName
    public String ${t.getName}.skillName() { return "${t.getName.getInternalName()}"; }

${
      // add default constructor if missing
        try {
          rc.map(t).getConstructor("()V")
          ""
        } catch {
          case e: NotFoundException ⇒ s"""// Add default constructor
    public ${t.getName}.new() { super(); }""";
        }
}

    public ${t.getName}.new(${makeConstructorArguments(t)}${if (!t.getAllFields.isEmpty()) ", " else ""}long skillID, SkillObject ignored) {
        super();
${
  (for (f ← t.getAllFields if !(f.isConstant || f.isIgnored))
    yield s"        this.${name(f)} = ${name(f)};").mkString("\n")
}
        this.skillID = skillID;
        // this is stupid, but the parameter makes sure that there are no signature conflicts.
        assert ignored == null;
    }


    public void ${t.getName}.selfAdd(SkillFile sf) {
        if (this.getSkillID() == -9) {
            return;
        }
        this.setSkillID(-9);
        sf.${name(t)}s().add(this);
${
      t.getAllFields.map { f ⇒
        f.getType match {
          case ut: UserType ⇒ s"""
        if (this.${getterOrFieldAccess(t, f)} != null) {
            this.${getterOrFieldAccess(t, f)}.selfAdd(sf);
        }"""
          case gt: GroundType ⇒ if (gt.getSkillName.equals("string")) s"""
        sf.Strings().add(this.${getterOrFieldAccess(t, f)});"""
          else ""
          case lt: SingleBaseTypeContainer ⇒ lt.getBaseType match {
            case gt: GroundType ⇒ if (gt.getSkillName.equals("string")) s"""
        if (this.${getterOrFieldAccess(t, f)} != null) {
            for (String s : this.${getterOrFieldAccess(t, f)}) { sf.Strings().add(s); }
        }"""
            else ""
            case ut: UserType ⇒ s"""
        if (this.${getterOrFieldAccess(t, f)} != null) {
            for (${mapType(ut)} e : this.${getterOrFieldAccess(t, f)}) { e.selfAdd(sf); }
        }"""
          }
          case mt: MapType ⇒ s"""
        if (this.${getterOrFieldAccess(t, f)} != null) {
${writeAddMaps(mt.getBaseTypes.toList, 0, s"this.${getterOrFieldAccess(t, f)}", rc.map(f).getName, "            ")}
        }"""
          case x: Type ⇒ s"""
        // cannot addAll a ${x} because I don't know this type"""
        }
      }.mkString("")
}
    }

${
  if (t.getSuperType() == null) {
s"""    // Add SkillObject interface
    declare parents : ${t.getName} implements SkillObject;"""
  } else ""
}

    // Initialize skillID no matter which constructor is called
    before(${t.getName} x): target(x) && execution(${t.getName}.new(..)) {
        x.skillID = -1;
    }
}
""");

      out.close();

    }
  }
}
