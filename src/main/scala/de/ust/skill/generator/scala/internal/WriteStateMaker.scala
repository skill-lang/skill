/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait WriteStateMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/WriteState.scala")
    //package
    out.write(s"""package ${packagePrefix}internal

import ${packagePrefix}api.SkillType
import ${packagePrefix}api.SkillState
import ${packagePrefix}internal.pool.BasePool""")

    //(imports are part of the template)
    //the body itself is always the same
    copyFromTemplate(out, "WriteState.scala.template")

    //class prefix
    out.close()
  }
}
