package de.ust.skill.main

import de.ust.skill.javacf.mapping.ExplicitMappingRule
import de.ust.skill.javacf.IRMapper
import de.ust.skill.ir.TypeContext
import de.ust.skill.javacf.mapping.MappingParser
import de.ust.skill.javacf.mapping.MappingParser
import scala.io.Source
import scala.util.parsing.input.Reader
import java.io.FileReader

object JavaForeign {

  /** Runner for java-foreign specific stuff. */
  def run(mappingFile: String, skillTc: TypeContext): Unit = {

    val mappingParser = new MappingParser()
    println(mappingParser.parse(mappingParser.mappingFile, new FileReader(mappingFile)))
    //val mapper = new IRMapper(List("../test-cases/bin"))
    //val tc = mapper.mapClasses(List("simple.SimpleType", "simplereference.SimpleReference"))
  }

}