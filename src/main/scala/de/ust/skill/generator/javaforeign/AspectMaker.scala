/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.javaforeign

import javassist.NotFoundException

trait AspectMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← IR) {
      val out = open(name(t) + "Aspects.java")

      out.write(s"""
package ${packageName};
import de.ust.skill.common.jforeign.internal.SkillObject;
import ${t.getName.getPackagePath}.${t.getName};


/**
 * Aspects for ${t.getName}.
 *
 * @author Constantin Weißer
 */
public aspect ${name(t)}Aspects {

    // add skillID to ${name(t)}
    public long ${t.getName}.skillID;
    // getter and setter for skillID
    public long ${t.getName}.getSkillID() { return this.skillID; }
    public void ${t.getName}.setSkillID(long skillID) { this.skillID = skillID; }
    // Access to skillName
    public String ${t.getName}.skillName() { return "${t.getName}"; }

    ${
        try {
          rc.map(t).getConstructor("()V")
          s"""// Default constructor is available, need not inject one
    //public ${t.getName}.new() { super(); }""";
        } catch {
          case e: NotFoundException ⇒ s"""// Add default constructor
    public ${t.getName}.new() { super(); }""";
        }
      }

    // Add SkillObject interface
    declare parents : ${t.getName} implements SkillObject;

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