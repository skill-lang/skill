/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import java.io.File
import java.io.FileNotFoundException
import java.lang.Long
import java.nio.file.FileSystems

import scala.annotation.migration
import scala.annotation.tailrec
import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.util.parsing.combinator.RegexParsers

import de.ust.skill.ir
import de.ust.skill.ir.Comment
import de.ust.skill.ir.Hint
import de.ust.skill.ir.Restriction
import de.ust.skill.ir.TypeContext
import de.ust.skill.ir.restriction.AbstractRestriction
import de.ust.skill.ir.restriction.ConstantLengthPointerRestriction
import de.ust.skill.ir.restriction.FloatDefaultRestriction
import de.ust.skill.ir.restriction.FloatRangeRestriction
import de.ust.skill.ir.restriction.IntDefaultRestriction
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.MonotoneRestriction
import de.ust.skill.ir.restriction.NameDefaultRestriction
import de.ust.skill.ir.restriction.NonNullRestriction
import de.ust.skill.ir.restriction.SingletonRestriction
import de.ust.skill.ir.restriction.StringDefaultRestriction
import de.ust.skill.ir.restriction.UniqueRestriction
import de.ust.skill.ir.restriction.DefaultRestriction

/**
 * Converts a character stream into an AST using parser combinators.
 *
 * Grammar as explained in the paper.
 */
final class SkillFileParser(
    _delimitWithUnderscore : Boolean,
    _delimitWithCamelCase : Boolean)
      extends AbstractFileParser[Declaration](
        _delimitWithUnderscore,
        _delimitWithCamelCase) {

  /**
   * A file is a list of includes followed by a list of declarations.
   */
  private def file = headComment ~> rep(includes) ~! rep(declaration) ^^ {
    case i ~ d ⇒ (i.fold(List[String]())(_ ++ _), d)
  }

  /**
   * Declarations add or modify user defined types.
   */
  private def declaration : Parser[Declaration] = typedef | enumType | interfaceType | userType

  /**
   * A declaration may start with a description, is followed by modifiers and a name, might have a super class and has
   * a body.
   */
  private def userType = typeDescription ~ id ~ rep((":" | "with" | "extends") ~> id) ~!
    ("{" ~> rep(field) <~ "}") ^^ {
      case d ~ n ~ s ~ b ⇒ new UserType(currentFile, d, n, s.sortBy(_.source), b)
    }

  /**
   * creates an enum definition
   */
  private def enumType = opt(comment) ~ ("enum" ~> id) ~ ("{" ~> repsep(id, ",") <~ ";") ~ (rep(field) <~ "}") ^^ {
    case c ~ n ~ i ~ f ⇒
      if (i.isEmpty)
        throw ParseException(s"Enum $n requires a non-empty list of instances!")
      else
        new EnumDefinition(currentFile, c.getOrElse(Comment.NoComment.get), n, i, f)
  }

  /**
   * creates an interface definition
   */
  private def interfaceType = opt(comment) ~ ("interface" ~> id) ~ rep((":" | "with" | "extends") ~> id) ~ (
    "{" ~> rep(field) <~ "}") ^^ {
      case c ~ n ~ i ~ f ⇒
        new InterfaceDefinition(currentFile, c.getOrElse(Comment.NoComment.get), n, i.sortBy(_.source), f)
    }

  /**
   * A field is either a constant or a real data field.
   */
  private def field = (
    (opt(comment) ^^ { c ⇒ c.getOrElse(Comment.NoComment.get) }) >> { c ⇒ view(c) | customField(c) } <~ ";"
    | fieldDescription ~ ((constant | data) <~ ";") ^^ { case d ~ f ⇒ { f.description = d; f } })

  /**
   * View an existing view as something else.
   */
  private def view(c : Comment) = ("view" ~> opt(id <~ ".")) ~ (id <~ "as") ~ fieldType ~! id ^^ {
    case targetType ~ targetField ~ newType ~ newName ⇒ new View(c, targetType, targetField, newType, newName)
  }

  /**
   * Constants a recognized by the keyword "const" and are required to have a value.
   */
  private def constant = "const" ~> fieldType ~! id ~! ("=" ~> int) ^^ { case t ~ n ~ v ⇒ new Constant(t, n, v) }

  /**
   * Data may be marked to be auto and will therefore only be present at runtime.
   */
  private def data = opt("auto") ~ fieldType ~! id ^^ { case a ~ t ~ n ⇒ new Data(a.isDefined, t, n) }

  /**
   * The <b>main</b> function of the parser, which turn a string into a list of includes and declarations.
   */
  override
  def process(in : File) : (List[String], List[Declaration]) = {
    currentFile = in;
    val lines = scala.io.Source.fromFile(in, "utf-8").getLines.mkString("\n")

    parseAll(file, lines) match {
      case Success(rval, _) ⇒ rval
      case f                ⇒ ParseException(s"parsing failed in ${in.getName}: $f");
    }
  }
}
