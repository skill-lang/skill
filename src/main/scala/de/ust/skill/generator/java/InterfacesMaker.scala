/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java

import java.io.PrintWriter
import scala.collection.JavaConversions._
import de.ust.skill.ir._
import de.ust.skill.ir.restriction._
import scala.collection.mutable.HashSet

trait InterfacesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← interfaces) {
      val out = open(name(t)+".java")

      //package
      out.write(s"""package ${this.packageName};

import de.ust.skill.common.java.api.FieldDeclaration;
import de.ust.skill.common.java.internal.NamedType;
import de.ust.skill.common.java.internal.SkillObject;
import de.ust.skill.common.java.internal.StoragePool;
""")

      val packageName = if (this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.') + 1) else this.packageName;

      val fields = t.getAllFields.filter(!_.isConstant)
      val relevantFields = fields.filter(!_.isIgnored)

      out.write(s"""
${
        comment(t)
      }${
        suppressWarnings
      }public interface ${name(t)} ${
        if (t.getSuperInterfaces.isEmpty) ""
        else
          t.getSuperInterfaces.map(name(_)).mkString("extends ", ", ", "")
      } {${
        if(t.getSuperType.isInstanceOf[UserType])
          s"""

    /**
     * cast to concrete type
     */
    public default ${mapType(t.getSuperType)} self() {
        return (${mapType(t.getSuperType)}) this;
    }
"""
        else ""
      }
${
        // collect visible fields
        var visibleFields : HashSet[Field] = t.getFields.filterNot(_.isInstanceOf[View]).to;
        if (t.getSuperType.isInstanceOf[UserType])
          visibleFields ++= t.getSuperType.asInstanceOf[UserType].getAllFields.filterNot(_.isInstanceOf[View]);

        ///////////////////////
        // getters & setters //
        ///////////////////////
        (
          for (f ← visibleFields) yield {
            if (f.isConstant)
              s"""
    //TODO default? ${comment(f)}static public ${mapType(f.getType())} get${escaped(f.getName.capital)}();
"""
            else
              s"""
    ${comment(f)}public ${mapType(f.getType())} get${escaped(f.getName.capital)}();

    ${comment(f)}public void set${escaped(f.getName.capital)}(${mapType(f.getType())} ${name(f)});
"""
          }
        ).mkString
      }
}
""");
      out.close()
    }
  }
}
