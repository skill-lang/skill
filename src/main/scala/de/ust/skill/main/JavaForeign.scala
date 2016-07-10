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
  def run(mappingFile: String, skillTc: TypeContext, foreignSources: List[String]): Unit = {

    val mappingParser = new MappingParser()
    val parserResult = mappingParser.parse(mappingParser.mappingFile, new FileReader(mappingFile))
    val mappingRules = parserResult.get
    println("****** MAPPING *******")
    println(mappingRules.mkString("\n\n"))
    println("**********************\n\n")

    // get list of java class names that we want to map
    val javaTypeNames = mappingRules.map { _.getJavaTypeName }
    println("****** Classes *******")
    println(javaTypeNames.mkString("\n"))
    println("**********************\n\n")

    val mapper = new IRMapper(foreignSources)
    val foreignTc = mapper.mapClasses(javaTypeNames)
  }

}