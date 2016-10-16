package de.ust.skill.main

import de.ust.skill.jforeign.mapping.ExplicitMappingRule
import de.ust.skill.jforeign.IRMapper
import de.ust.skill.ir.TypeContext
import de.ust.skill.jforeign.mapping.MappingParser
import de.ust.skill.jforeign.mapping.MappingParser
import scala.io.Source
import scala.util.parsing.input.Reader
import java.io.FileReader
import javassist.CtClass
import scala.collection.mutable.HashMap
import de.ust.skill.ir.Type
import de.ust.skill.jforeign.typing.TypeChecker
import de.ust.skill.jforeign.ReflectionContext
import de.ust.skill.generator.jforeign.Main

object JavaForeign {

  /** Runner for java-foreign specific stuff. */
  def run(generator: Main, skillTc: TypeContext) : (TypeContext, ReflectionContext) = {
    // parse mapping
    val mappingParser = new MappingParser()
    val mappingRules = mappingParser.process(new FileReader(generator.getMappingFile()))
    // get list of java class names that we want to map
    val javaTypeNames = mappingRules.map { _.getJavaTypeName }
    // map
    val mapper = new IRMapper(generator.getForeignSources())
    val (javaTc, rc) = mapper.mapClasses(javaTypeNames)
    // bind and typecheck
    val typeRules = mappingRules.flatMap { r => r.bind(skillTc, javaTc) }
    val checker = new TypeChecker
    checker.check(typeRules, skillTc, javaTc, rc)
    // prepare generator
    generator.setForeignTC(javaTc)
    generator.setReflectionContext(rc)
    (javaTc, rc)
  }

}