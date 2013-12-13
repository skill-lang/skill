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
    if (packagePrefix.length > 0) {
      out.write(s"""package ${packagePrefix.substring(0, packagePrefix.length - 1)}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

""")
    }

    //imports
    if (null == d.getSuperType()) {
      out.write(s"import ${packagePrefix}api._\n\n")
    }

    //class comment
    out.write(s"/*${d.getSkillComment()}*/\n")

    //class prefix
    out.write(s"trait ${d.getName()} ${
      if (null != d.getSuperType()) { s"extends ${d.getSuperType().getName()}" }
      else { "extends KnownType" }
    } {\n")

    //body
    d.getFields().foreach({ f ⇒
      val name_ = escaped(f.getName)
      val Name = f.getName.capitalize

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
  /*${f.getSkillComment()}*/
  def $name_[T <: SkillType: ClassTag](): T
  /*${f.getSkillComment()}*/
  def ${name_}_=[T <: SkillType]($Name: T): Unit
""")
        } else {
          val argumentType = mapType(f.getType())
          out.write(s"""
  /*${f.getSkillComment()}*/
  def $name_(): $argumentType
  /*${f.getSkillComment()}*/
  def ${name_}_=($Name: $argumentType): Unit
""")
        }
      }
    })

    out.write("}\n");

    out.close()
  }
}
