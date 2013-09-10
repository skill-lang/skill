package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait SkillExceptionMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/SkillException.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

""")

    //the body itself is always the same
    copyFromTemplate(out, "SkillException.scala.template")

    //class prefix
    out.close()
  }
}