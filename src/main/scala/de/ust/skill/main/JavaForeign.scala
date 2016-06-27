package de.ust.skill.main

import de.ust.skill.javacf.mapping.Mapping
import de.ust.skill.javacf.IRMapper
import de.ust.skill.ir.TypeContext
import de.ust.skill.javacf.mapping.MappingParser

object JavaForeign {

  /** Runner for java-foreign specific stuff. */
  def run(mappingFile: String, skillTc: TypeContext): Unit = {
    val mapping = MappingParser.parseFile(mappingFile)
    //val mapper = new IRMapper(List("../test-cases/bin"))
    //val tc = mapper.mapClasses(List("simple.SimpleType", "simplereference.SimpleReference"))
  }

}