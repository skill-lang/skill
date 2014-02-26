/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir.Declaration

trait DeclarationInterfaceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    IR.foreach({ d ⇒
      makeDeclarationInterface(open(d.getName()+".scala"), d)
    })
  }

  private def makeDeclarationInterface(out: PrintWriter, d: Declaration) {
    //package
    if (packagePrefix.length > 0)
      out.write(s"""package ${packagePrefix.substring(0, packagePrefix.length - 1)}""")

    out.write(s"""

import ${packagePrefix}api.SkillType

""")

    //class comment
    if (d.getSkillComment.size > 0)
      out.write(s"/*${d.getSkillComment()}*/\n")

    //class prefix
    val Name = d.getCapitalName;
    out.write(s"trait $Name ${
      if (null != d.getSuperType()) { s"extends ${d.getSuperType().getCapitalName()}" }
      else { "extends SkillType" }
    } {")

    //body
    d.getFields().foreach({ f ⇒
      val name_ = escaped(f.getName)

      @inline def mkComment = if (f.getSkillComment.size > 0) "  /*"+f.getSkillComment()+"*/\n" else ""

      if (f.isConstant) {
        // constants do not have a setter
        out.write(s"\n  def get$Name(): ${mapType(f.getType())} = ${f.constantValue}\n@inline def $name_ = get$Name\n")
      } else {
        // add a warning to auto fields
        if (f.isAuto) {
          out.write("\n  /** auto aka not serialized */")
        }

        // standard field data interface
        if ("annotation".equals(f.getType().getName())) {
          out.write(s"""
$mkComment  def $name_ : SkillType
$mkComment  def ${name_}_=(${name_} : SkillType): Unit
""")
        } else {
          val argumentType = mapType(f.getType())
          out.write(s"""
$mkComment  def $name_ : $argumentType
$mkComment  def ${name_}_=(${name_} : $argumentType): Unit
""")
        }
      }
    })

    out.write(s"""}

object $Name {
  def unapply(self:$Name) = ${(for (f ← d.getFields) yield "self."+f.getName).mkString("Some(", ", ", ")")}
}
""");

    out.close()
  }
}
