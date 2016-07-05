package de.ust.skill.javacf.mapping

import scala.io.Source
import scala.util.parsing.combinator._

class MappingParser extends RegexParsers {

  def name: Parser[String] = """[a-zA-Z.0-9]+""".r ^^ { _.toString() }

  def implicitMapping: Parser[ImplicitMappingRule] = ("implicit" | "total") ~ name ~ "->" ~ name ~ ";" ^^ {
    case "implicit" ~ skill ~ _ ~ java ~ _ => new ImplicitMappingRule(skill, java, false)
    case "total" ~ skill ~ _ ~ java ~ _ => new ImplicitMappingRule(skill, java, true)
  }

  def explicitMapping: Parser[ExplicitMappingRule] = "map" ~ name ~ "->" ~ name ~ "{" ~ rep(fieldMapping) ~ "}" ^^ {
    case _ ~ skill ~ _ ~ java ~ _ ~ fields ~ _ => new ExplicitMappingRule(skill, java, fields)
  }

  def fieldMapping: Parser[FieldMappingRule] = name ~ "->" ~ name ~ ";" ^^ {
    case skill ~ _ ~ java ~ _ =>
      new FieldMappingRule(skill, java)
  }

  def mapping: Parser[MappingRule] = explicitMapping | implicitMapping

  def mappingFile: Parser[List[MappingRule]] = rep(mapping) ^^ { case x => x }

}
