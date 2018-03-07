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
import scala.collection.mutable.HashMap
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

// TODO AbstractParser und IdlParser
// TODO merging passiert in IdlParser
// TODO CommandLine anpassen hoere auf .sidl dateien

abstract class AbstractParser() {
  def process(input : File,
              keepSpecificationOrder : Boolean = false,
              delimitWithUnderscore : Boolean = true,
              delimitWithCamelCase : Boolean = true,
              verboseOutput : Boolean = false) : TypeContext
}

/**
 * The Parser does everything required for turning a set of files into a list of definitions.
 * @see #process
 * @see SKilL V1.0 Appendix A
 * @author Timm Felden
 * @param delimitWithUnderscore if true, underscores in words are used as delimiters. This will influence name
 * equivalence
 */
final class Parser(
    private val delimitWithUnderscore : Boolean = true,
    private val delimitWithCamelCase : Boolean = true,
    verboseOutput : Boolean = false) {

  val tc = new ir.TypeContext

  /**
   * Parses a file and all related files and passes back a List of definitions. The returned definitions are also type
   * checked.
   */
  private def parseAll(input : File) = {
    val parser = new FileParser(delimitWithUnderscore, delimitWithCamelCase)
    val base = new File(System.getProperty("user.dir")).toURI();
    val todo = new HashSet[String]();
    todo.add(base.relativize(input.toURI()).getPath());
    val done = new HashSet[String]();
    var rval = new ArrayBuffer[Declaration]();
    while (!todo.isEmpty) {
      val file = todo.head
      todo -= file;
      if (!done.contains(file)) {
        done += file;

        try {
          val result = parser.process(new File(file))

          // add includes to the todo list
          for (path ← result._1) {
            // strip common prefix, if possible
            todo += base.relativize(new File(path).toURI()).getPath();
          }

          // add definitions
          rval = rval ++ result._2
          if (verboseOutput)
            println(s"acc: $file ⇒ ${rval.size}")
        } catch {
          case e : FileNotFoundException ⇒ ParseException(
            s"The include $file could not be resolved to an existing file: ${e.getMessage()} \nWD: ${
              FileSystems.getDefault().getPath(".").toAbsolutePath().toString()
            }", e)
        }
      }
    }
    rval
  }
}

object Parser extends AbstractParser {

  /**
   * @return a type context containing all type information obtained from the argument file
   */
  def process(input : File,
              keepSpecificationOrder : Boolean = false,
              delimitWithUnderscore : Boolean = true,
              delimitWithCamelCase : Boolean = true,
              verboseOutput : Boolean = false) : TypeContext = {

    val p = new Parser(delimitWithUnderscore, delimitWithCamelCase, verboseOutput)
    IRBuilder.buildIR(p.parseAll(input).to, verboseOutput, keepSpecificationOrder)
  }
}

// TODO how to remove duplicate code?

/**
 * The Parser does everything required for turning a set of files into a list of definitions.
 * @see #process
 * @see SKilL V1.0 Appendix A
 * @author Timm Felden
 * @param delimitWithUnderscore if true, underscores in words are used as delimiters. This will influence name
 * equivalence
 */
final class IdlParser(
    private val delimitWithUnderscore : Boolean = true,
    private val delimitWithCamelCase : Boolean = true,
    verboseOutput : Boolean = false) {

  val tc = new ir.TypeContext

  /**
   * Parses a file and all related files and passes back a List of definitions. The returned definitions are also type
   * checked.
   */
  private def parseAll(input : File) = {
    val parser = new IdlFileParser(delimitWithUnderscore, delimitWithCamelCase)
    val base = new File(System.getProperty("user.dir")).toURI();
    val todo = new HashSet[String]();
    todo.add(base.relativize(input.toURI()).getPath());
    val done = new HashSet[String]();
    var rval = new ArrayBuffer[IdlDefinition]();
    while (!todo.isEmpty) {
      val file = todo.head
      todo -= file;
      if (!done.contains(file)) {
        done += file;

        try {
          val result = parser.process(new File(file))

          // add includes to the todo list
          for (path ← result._1) {
            // strip common prefix, if possible
            todo += base.relativize(new File(path).toURI()).getPath();
          }

          // add definitions
          rval = rval ++ result._2
          if (verboseOutput)
            println(s"acc: $file ⇒ ${rval.size}")
        } catch {
          case e : FileNotFoundException ⇒ ParseException(
            s"The include $file could not be resolved to an existing file: ${e.getMessage()} \nWD: ${
              FileSystems.getDefault().getPath(".").toAbsolutePath().toString()
            }", e)
        }
      }
    }
    combine(rval)
  }

  private def mergeComments(c1: Comment, c2: Comment) = {
    Comment.NoComment.get
  }

  private def mergeDescriptions(d1: Description, d2: Description) = {
    new Description(
      mergeComments(d1.comment, d2.comment),
      d1.restrictions ++ d2. restrictions,
      d1.hints ++ d2.hints)
  }

  private def combine(items : ArrayBuffer[IdlDefinition]) : ArrayBuffer[Declaration] = {
    val defs = items.filter(!_.isInstanceOf[AddedField])
    val addedFields = items.collect { case t : AddedField ⇒ t }

    val definitionNames = new HashMap[Name, IdlDefinition];
    // TODO .withDefaultValue({new ArrayBuffer()}) with lambda ?
    val superTypes = new HashMap[Name, ArrayBuffer[Name]]()

    // merge descriptnion and find superTypes
    for (d ← defs) {
      // TODO remove repetision? additional "interface" ?
      d match {
        case e: IdlUserType => {
          println(s"try insert for UT ${e.name} (${e.subTypes})")
          for (n <- e.subTypes) {
            if (!superTypes.contains(n)) {
              superTypes.put(n, new ArrayBuffer[Name]())
            }
            println(s"insert ${e.name} for ${n}")
            superTypes(n).append(e.name)
          }
        }
        case e: IdlInterface => {
          println(s"try insert for I ${e.name} (${e.subTypes})")
          for (n <- e.subTypes) {
            if (!superTypes.contains(n)) {
              superTypes.put(n, new ArrayBuffer[Name]())
            }
            println(s"insert ${e.name} for ${n}")
            superTypes(n).append(e.name)
          }
        }
        case _ => { }
      }
      if (definitionNames.contains(d.name)) {
        val old = definitionNames(d.name)
        definitionNames(d.name) = (old, d) match {
          case (p: IdlUserType, q: IdlUserType) => {
            IdlUserType(
              p.declaredIn, // TODO welches File?
              mergeDescriptions(p.description, q.description),
              p.name,
              List.empty)
          }
          case (p: IdlEnum, q: IdlEnum) => {
            IdlEnum(
              p.declaredIn, // TODO welches File?
              mergeComments(p.comment, q.comment),
              p.name,
              p.instances ++ q.instances)
          }
          case (p: IdlInterface, q: IdlInterface) => {
            IdlInterface(
              p.declaredIn, // TODO welches File?
              mergeComments(p.comment, q.comment),
              p.name,
              List.empty)
          }
          case _ => ParseException("TODO")
        }
      } else {
        definitionNames.put(d.name, d)
      }
    }

    val astNames = new HashMap[Name, Declaration];

    // convert to AST nodes
    for (d ← definitionNames.values) {
      // TODO get rid of filling in all the default super types ?
      if (!superTypes.contains(d.name)) {
        println(s"empty super for ${d.name}")
        superTypes.put(d.name, new ArrayBuffer[Name]())
      }
      println(s"${d.name} extends ${superTypes(d.name)}")
      astNames.put(d.name, d match {
        case d: IdlUserType => {
          new UserType(
            d.declaredIn,
            d.description,
            d.name,
            superTypes(d.name).to,
            List.empty)
        }
        case d: IdlEnum => {
          new EnumDefinition(
            d.declaredIn,
            d.comment,
            d.name,
            d.instances,
            List.empty)
        }
        case d: IdlInterface => {
          new InterfaceDefinition(
            d.declaredIn,
            d.comment,
            d.name,
            superTypes(d.name).to,
            List.empty)
        }
        case d: IdlTypedef => d.typedef
        case _ => ParseException("TODO")
      })
    }

    // merge fields into AST nodes
    for (addedField <- addedFields) {
      if (!(definitionNames contains addedField.name)) {
        if (!superTypes.contains(addedField.name)) {
          println(s"empty super for ${addedField.name}")
          superTypes.put(addedField.name, new ArrayBuffer[Name]())
        }
        astNames += (addedField.name ->
          new UserType(
            addedField.file,
            new Description(Comment.NoComment.get, List.empty, List.empty),
            addedField.name,
            superTypes(addedField.name).to,
            List.empty)
          )
      }
      astNames(addedField.name) =
        astNames(addedField.name) match {
          // TODO can I just have AbstactField and t.copy(body = t.body + addedField.fs)?
          // TODO should/can I use the body as mutlable List
          case t: UserType =>
            new UserType(
              t.declaredIn,
              t.description,
              t.name,
              t.superTypes,
              t.body ++ addedField.fields)
          case t: EnumDefinition =>
            new EnumDefinition(
              t.declaredIn,
              t.comment,
              t.name,
              t.instances,
              t.body ++ addedField.fields)
          case t: InterfaceDefinition =>
            new InterfaceDefinition(
              t.declaredIn,
              t.comment,
              t.name,
              t.superTypes,
              t.body ++ addedField.fields)
          case d: Typedef => d
          case _ => ParseException("TODO")
        }
    }
    astNames.values.to
  }
}

object IdlParser extends AbstractParser {

  /**
   * @return a type context containing all type information obtained from the argument file
   */
  def process(input : File,
              keepSpecificationOrder : Boolean = false,
              delimitWithUnderscore : Boolean = true,
              delimitWithCamelCase : Boolean = true,
              verboseOutput : Boolean = false) : TypeContext = {

    val p = new IdlParser(delimitWithUnderscore, delimitWithCamelCase, verboseOutput)
    IRBuilder.buildIR(p.parseAll(input).to, verboseOutput, keepSpecificationOrder)
  }
}
