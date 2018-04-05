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
final class SIDLFileParser(
  _delimitWithUnderscore : Boolean,
  _delimitWithCamelCase :  Boolean)
  extends AbstractFileParser[SIDLDefinition](
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
  private def declaration : Parser[SIDLDefinition] =
    typedef ^^ { td ⇒ SIDLTypedef(td) } | enumType |
      interfaceType | userType | addedFields

  /**
   * A declaration may start with a description, is followed by modifiers and a name, might have a super class.
   */
  private def userType =
    typeDescription ~ id ~
      ((";" ^^ { x ⇒ List.empty }) | ("::=" ~> rep1sep(id, "|") <~ opt(";"))) ^^ {
        case d ~ n ~ s ⇒ new SIDLUserType(currentFile, d, n, s.sortBy(_.source))
      }

  /**
   * creates an interface definition
   */
  private def interfaceType =
    opt(comment) ~ ("interface" ~> id) ~
      ((";" ^^ { x ⇒ List.empty }) | ("::=" ~> rep1sep(id, "|") <~ opt(";"))) ^^ {
        case c ~ n ~ i ⇒ new SIDLInterface(currentFile, c.getOrElse(Comment.NoComment.get), n, i.sortBy(_.source))
      }

  /**
   * Add fields to a user type.
   */
  private def addedFields = opt(comment) ~ id ~ (("->" | "⇒") ~> rep1sep(field, ",") <~ opt(";")) ^^ {
    case c ~ n ~ f ⇒ new AddedField(c.getOrElse(Comment.NoComment.get), n, f, currentFile)
  }

  /**
   * creates an enum definition
   */
  private def enumType =
    opt(comment) ~ ("enum" ~> id) ~ ("::=" ~> rep1sep(id, "|") <~ opt(";")) ^^ {
        case c ~ n ~ i ⇒
          if (i.isEmpty)
            throw ParseException(s"Enum $n requires a non-empty list of instances!")
          else
            new SIDLEnum(currentFile, c.getOrElse(Comment.NoComment.get), n, i)
      }

  /**
   * A field is either a constant or a real data field.
   */
  private def field = (
    (opt(comment) ^^ { c ⇒ c.getOrElse(Comment.NoComment.get) }) >> { c ⇒ view(c) | customField(c) }
    | fieldDescription ~ ((constant | data)) ^^ { case d ~ f ⇒ { f.description = d; f } })

  /**
   * View an existing view as something else.
   */
  private def view(c : Comment) = id ~ (":" ~> fieldType) ~ ("view" ~> id) ~ opt("." ~> id) ^^ {
    case newName ~ newType ~ targetField ~ targetType ⇒ new View(c, targetType, targetField, newType, newName)
  }

  /**
   * Constants a recognized by the keyword "const" and are required to have a value.
   */
  private def constant = id ~ ("=" ~> int <~ ":") ~ ("const" ~> fieldType) ^^ { case n ~ v ~ t ⇒ new Constant(t, n, v) }

  /**
   * Data may be marked to be auto and will therefore only be present at runtime.
   */
  private def data = (id <~ ":") ~ opt("auto") ~ fieldType ^^ { case n ~ a ~ t ⇒ new Data(a.isDefined, t, n) }

  /**
   * The <b>main</b> function of the parser, which turn a string into a list of includes and declarations.
   */
  override def process(in : File) : (List[String], List[SIDLDefinition]) = {
    currentFile = in;
    val lines = scala.io.Source.fromFile(in, "utf-8").getLines.mkString("\n")

    parseAll(file, lines) match {
      case Success(rval, _) ⇒ rval
      case f                ⇒ ParseException(s"parsing failed in ${in.getName}: $f");
    }
  }
}
