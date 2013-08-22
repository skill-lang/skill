package de.ust.skill.generator.scala

import de.ust.skill.ir.Declaration
import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.ir.Type

trait DeclarationInterfaceMaker {
  protected def makeDeclarationInterface(out:PrintWriter, d:Declaration){
     //package

      //imports

      //class prefix
      out.write("trait " + d.getName() + " {\n")

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
      out.write("\noverride def toString(): String = \"" + d.getName() + "(\"")
      d.getFields().foreach({ f ⇒
        out.write("+" + f.getName()+"+\", \"")
      })
      out.write("+\")\"\n")
      
      out.write("}");

      out.close()
  }
  
  protected def _T(t: Type): String
}