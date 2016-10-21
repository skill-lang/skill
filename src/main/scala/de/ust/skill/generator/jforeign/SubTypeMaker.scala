/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.jforeign

import java.io.PrintWriter
import scala.collection.JavaConversions._
import de.ust.skill.ir._
import de.ust.skill.ir.restriction._
import scala.collection.mutable.HashSet

trait SubTypeMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    
    for (t ← IR) {
      val abstrct : Boolean = t.getRestrictions.filter { p => p.isInstanceOf[AbstractRestriction] }.nonEmpty
      if (!abstrct) {
      val out = open("internal/" + name(t)+ "SubType.java")
      
      out.write(s"""
package ${packagePrefix}internal;
import ${t.getName.getPackagePath}.${t.getName};

import de.ust.skill.common.jforeign.internal.NamedType;
import de.ust.skill.common.jforeign.internal.StoragePool;

/**
 * Generic sub types of ${t.getName}.
 * 
 * @author Timm Felden
 */
public final class ${name(t)}SubType extends ${name(t)} implements NamedType {
    private final StoragePool<?, ?> τPool;

    /** internal use only!!! */
    public ${name(t)}SubType(StoragePool<?, ?> τPool, long skillID) {
        super();
        this.setSkillID(skillID);
        this.τPool = τPool;
    }

    @Override
    public StoragePool<?, ?> τPool() {
      return τPool;
    }

    @Override
    public String skillName() {
        return τPool.name();
    }

    @Override
    public String toString() {
        return skillName() + "#" + getSkillID();
    }
}
""");
      
      out.close();
      
      }
    }
  }
}
