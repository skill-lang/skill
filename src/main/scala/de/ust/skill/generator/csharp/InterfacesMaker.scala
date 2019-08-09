/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp

import java.io.PrintWriter
import scala.collection.JavaConversions._
import de.ust.skill.ir._
import de.ust.skill.ir.restriction._
import scala.collection.mutable.HashSet

trait InterfacesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← interfaces) {
      val out = files.open(s"${name(t)}.cs")

      //package
      out.write(s"""
using FieldDeclaration = de.ust.skill.common.csharp.api.FieldDeclaration;
using NamedType = de.ust.skill.common.csharp.@internal.NamedType;
using SkillObject = de.ust.skill.common.csharp.@internal.SkillObject;
using AbstractStoragePool = de.ust.skill.common.csharp.@internal.AbstractStoragePool;

namespace ${this.packageName}
{
""")

      val packageName =
        if (this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.') + 1)
        else this.packageName;

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
          t.getSuperInterfaces.map(name(_)).mkString(": ", ", ", "")
    } {

        ${
        ///////////////////////
        // getters & setters //
        ///////////////////////
        (
          for (f ← t.getAllFields) yield {
            if (f.isConstant)
            s"""
            //TODO default? ${comment(f)}static public ${mapType(f.getType())} ${name(f)};
"""
            else
            s"""
            ${comment(f)}${mapType(f.getType())} ${name(f)}{ get;set; }
"""
          }
        ).mkString
      }
    }
}
""");
      out.close()
    }
  }
}
