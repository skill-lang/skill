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
import java.io.File
import java.io.PrintWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream
import scala.collection.JavaConversions._

object JavaForeign {

  /** Runner for java-foreign specific stuff. */
  def run(generator: Main, skillTc: TypeContext): Unit = {
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
    // generate specification file if requested
    generator.getGenSpecPath.foreach { path =>
      val f = new File(path)
      val prettySkillSpec = new PrintWriter(new OutputStreamWriter(new FileOutputStream(f)))
      javaTc.getUsertypes.foreach { ut =>
        prettySkillSpec.write(ut.prettyPrint() + "\n")
      }
      prettySkillSpec.close()
    }
    // prepare generator
    generator.setForeignTC(javaTc)
    generator.setReflectionContext(rc)
    (javaTc, rc)
  }

}