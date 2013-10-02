/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait TypeInfoMaker extends GeneralOutputMaker {
  override def make {
    super.make
    val out = open("internal/TypeInfo.scala")
    //package
    out.write(s"package ${packagePrefix}internal\n\n")

    //(imports are part of the template)
    //the body itself is always the same
    copyFromTemplate(out, "TypeInfo.scala.template")

    //class prefix
    out.close()
  }
}
