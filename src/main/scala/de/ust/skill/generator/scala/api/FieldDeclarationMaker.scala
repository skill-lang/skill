/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait FieldDeclarationMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/FieldDeclaration.scala")
    //package
    out.write(s"""package ${packagePrefix}api

trait FieldDeclaration{
  val name: String
}
""")

    //class prefix
    out.close()
  }
}
