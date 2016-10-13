/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.jforeign

import javassist.NotFoundException
import scala.collection.JavaConversions._

trait AspectMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← IR) {
      val out = open(name(t) + "Aspects.java")

      out.write(s"""
package ${packageName};
import ${packagePrefix}internal.SkillState;
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
    public String ${t.getName}.skillName() { return "${t.getName.getSkillName.toLowerCase()}"; }

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

    public void ${t.getName}.selfAdd(SkillState state) {
        state.addAll(this);
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
