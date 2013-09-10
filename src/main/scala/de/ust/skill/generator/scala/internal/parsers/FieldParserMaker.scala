package de.ust.skill.generator.scala.internal.parsers

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait FieldParserMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/parsers/FieldParser.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.parsers

import scala.collection.mutable.HashMap
import scala.collection.mutable.LinkedList

import ${packagePrefix}internal._
""")

    //the body itself is always the same
    copyFromTemplate(out, "FieldParser.scala.template")

    //class prefix
    out.close()
  }
}