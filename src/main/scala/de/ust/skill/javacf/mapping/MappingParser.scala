package de.ust.skill.javacf.mapping

import scala.io.Source
import scala.util.parsing.combinator._

class MappingParser extends RegexParsers {

  def name: Parser[String] = """[a-zA-Z.0-9]+""".r ^^ { _.toString() }

  def explicitMapping: Parser[ExplicitMappingRule] = "map" ~ name ~ "->" ~ name ~ "{" ~ rep(fieldMapping) ~ "}" ^^ {
    case _ ~ skill ~ _ ~ java ~ _ ~ fields ~ _ => new ExplicitMappingRule(skill, java, fields)
  }

  def fieldMapping: Parser[FieldMappingRule] = name ~ "->" ~ name ~ ";" ^^ {
    case skill ~ _ ~ java ~ _ =>
      new FieldMappingRule(skill, java)
  }

  def mappingFile: Parser[List[ExplicitMappingRule]] = rep(explicitMapping) ^^ { case x => x }

}