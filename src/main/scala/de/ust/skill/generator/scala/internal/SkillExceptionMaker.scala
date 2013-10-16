/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait SkillExceptionMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/SkillException.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import ${packagePrefix}internal.parsers.ByteReader

""")

    //the body itself is always the same
    copyFromTemplate(out, "SkillException.scala.template")

    //class prefix
    out.close()
  }
}
