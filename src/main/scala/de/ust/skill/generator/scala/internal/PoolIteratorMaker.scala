package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait PoolIteratorMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/PoolIterator.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import ${packagePrefix}api.KnownType
import ${packagePrefix}internal.pool.KnownPool

""")

    //the body itself is always the same
    copyFromTemplate(out, "PoolIterator.scala.template")

    //class prefix
    out.close()
  }
}
