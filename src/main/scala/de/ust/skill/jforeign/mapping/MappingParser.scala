/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign.mapping

import java.io.Reader

import scala.util.parsing.combinator.RegexParsers

class MappingParser extends RegexParsers {

  def name: Parser[String] = """[a-zA-Z.0-9$]+""".r ^^ { _.toString() }

  def implicitMapping: Parser[ImplicitMappingRule] = ("implicit!" | "implicit") ~ name ~ "->" ~ name ~ ";" ^^ {
    case "implicit" ~ skill ~ _ ~ java ~ _ => new ImplicitMappingRule(skill, java, false)
    case "implicit!" ~ skill ~ _ ~ java ~ _ => new ImplicitMappingRule(skill, java, true)
  }

  def explicitMapping: Parser[ExplicitMappingRule] = "map" ~ name ~ "->" ~ name ~ "{" ~ rep(fieldMapping) ~ "}" ^^ {
    case _ ~ skill ~ _ ~ java ~ _ ~ fields ~ _ => new ExplicitMappingRule(skill, java, fields)
  }

  def unboundMapping: Parser[UnboundMappingRule] = "new" ~ name ~ "{" ~ fieldList ~ "}" ^^ {
    case _ ~ java ~ _ ~ fields ~ _ => new UnboundMappingRule(java, fields, false)
  }

  def unboundTotalMapping: Parser[UnboundMappingRule] = "new!" ~ name ~ ";" ^^ {
    case _ ~ java ~ _ => new UnboundMappingRule(java, List(), true);
  }

  def fieldMapping: Parser[FieldMappingRule] = name ~ "->" ~ name ~ ";" ^^ {
    case skill ~ _ ~ java ~ _ =>
      new FieldMappingRule(skill, java)
  }

  def fieldList: Parser[List[String]] = rep(name ~ ";" ^^ { case n ~ _ => n })

  def mapping: Parser[MappingRule] = explicitMapping | implicitMapping | unboundMapping | unboundTotalMapping

  def mappingFile: Parser[List[MappingRule]] = rep(mapping)

  def process(reader: Reader): (List[MappingRule]) = {

    parseAll(mappingFile, reader) match {
      case Success(rval, _) ⇒ rval
      case f ⇒ throw new RuntimeException(s"Parsing mapping failed: $f");
    }
  }
}
