package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait IteratorMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/Iterator.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import ${packagePrefix}internal.StoragePool

""")

    //the body itself is always the same
    copyFromTemplate(out, "Iterator.scala.template")

    //class prefix
    out.close()
  }
}