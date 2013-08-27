package de.ust.skill.generator.scala

import de.ust.skill.ir.Declaration
import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.ir.Type

trait DeclarationInterfaceMaker {
  protected def makeDeclarationInterface(out: PrintWriter, d: Declaration) {
    //package
    if (packagePrefix.length > 0)
      out.write(s"package ${packagePrefix.substring(0, packagePrefix.length - 1)}\n\n")

    //imports

    //class prefix
    out.write("trait "+d.getName()+" {\n")

    //body
    out.write("//////// FIELD INTERACTION ////////\n")
    d.getFields().foreach({ f ⇒
      out.write("def "+f.getName()+"(): "+_T(f.getType())+"\n")
      out.write("def set"+f.getName().capitalize+"("+f.getName()+": "+_T(f.getType())+"):Unit\n")

    })

    //class postfix
    out.write("//////// OBJECT CREATION AND DESTRUCTION ////////\n")

    out.write("//////// UTILS ////////\n")

    //nice toString
    out.write("\noverride def toString(): String = \""+d.getName()+"(\"")
    d.getFields().foreach({ f ⇒
      out.write("+"+f.getName()+"+\", \"")
    })
    out.write("+\")\"\n")

    out.write("}");

    out.close()
  }

  /**
   * Assume the existence of a translation function for types.
   */
  protected def _T(t: Type): String

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix(): String
}