/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.javaforeign

import java.io.PrintWriter
import scala.collection.JavaConversions._
import de.ust.skill.ir._
import de.ust.skill.ir.restriction._
import scala.collection.mutable.HashSet

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
 * @author Timm Felden
 */
public aspect ${name(t)}Aspects {

    public long ${t.getName}.skillID;
    public long ${t.getName}.getSkillID() { return this.skillID; }
    public void ${t.getName}.setSkillID(long skillID) { this.skillID = skillID; }
    public String ${t.getName}.skillName() { return "${t.getName}"; }

    //public ${t.getName}.new() { super(); }
    public ${t.getName}.new(long skillID) { super(); this.skillID = skillID; }

    declare parents : ${t.getName} implements SkillObject;
}
""");

      out.close();

    }
  }
}