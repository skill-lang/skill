/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import java.io.File
import java.io.FileNotFoundException
import java.nio.file.FileSystems

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import de.ust.skill.ir
import de.ust.skill.ir.Comment
import de.ust.skill.ir.TypeContext
import de.ust.skill.parser.Description

/**
 * The Parser does everything required for turning a set of files into a list of definitions.
 * @see #process
 * @see SKilL V1.0 Appendix A
 * @author Timm Felden
 * @param delimitWithUnderscore if true, underscores in words are used as delimiters. This will influence name
 * equivalence
 */
class Parser(
  protected val delimitWithUnderscore : Boolean = true,
  protected val delimitWithCamelCase :  Boolean = true,
  protected val verboseOutput :         Boolean = false) {

  val tc = new ir.TypeContext

  protected def processAllFiles[Decl](input : File, fileParser : AbstractFileParser[Decl]) : ArrayBuffer[Decl] = {
    val base = new File(System.getProperty("user.dir")).toURI();
    val todo = HashSet[String](base.relativize(input.toURI()).getPath());
    val done = new HashSet[String]();
    var rval = new ArrayBuffer[Decl]();
    while (!todo.isEmpty) {
      val file = todo.head
      todo -= file;
      if (!done.contains(file)) {
        done += file;

        try {
          val result = fileParser.process(new File(file))

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

  /**
   * Parses a file and all related files and passes back a List of definitions. The returned definitions are also type
   * checked.
   */
  private[parser] def parseAll(input : File) = {
    processAllFiles(input, new SkillFileParser(delimitWithUnderscore, delimitWithCamelCase))
  }
}

object Parser {

  /**
   * @return a type context containing all type information obtained from the argument file
   */
  def process(
    input :                  File,
    keepSpecificationOrder : Boolean = false,
    delimitWithUnderscore :  Boolean = true,
    delimitWithCamelCase :   Boolean = true,
    verboseOutput :          Boolean = false) : TypeContext = {

    val ast = if (input.getName.endsWith(".skill")) {
      new Parser(delimitWithUnderscore, delimitWithCamelCase, verboseOutput).parseAll(input)
    } else {
      new SIDLParser(delimitWithUnderscore, delimitWithCamelCase, verboseOutput).parseAll(input)
    }
    IRBuilder.buildIR(ast.to, verboseOutput, keepSpecificationOrder)
  }
}

/**
 * The Parser does everything required for turning a set of files into a list of definitions.
 * @see #process
 * @see SKilL V1.0 Appendix A
 * @author Timm Felden
 * @param delimitWithUnderscore if true, underscores in words are used as delimiters. This will influence name
 * equivalence
 */
final class SIDLParser(
  _delimitWithUnderscore : Boolean = true,
  _delimitWithCamelCase :  Boolean = true,
  _verboseOutput :         Boolean = false)
  extends Parser(_delimitWithUnderscore, _delimitWithCamelCase, _verboseOutput) {

  /**
   * Parses a file and all related files and passes back a List of definitions. The returned definitions are also type
   * checked.
   */
  override private[parser] def parseAll(input : File) = {
    val rval = processAllFiles(input, new SIDLFileParser(delimitWithUnderscore, delimitWithCamelCase))
    combine(rval)
  }

  private def mergeComments(c1 : Comment, c2 : Comment) = {
    new Comment(c1, c2)
  }

  private def mergeDescriptions(d1 : Description, d2 : Description) = {
    new Description(
      mergeComments(d1.comment, d2.comment),
      d1.restrictions ++ d2.restrictions,
      d1.hints ++ d2.hints)
  }

  private def combine(items : ArrayBuffer[SIDLDefinition]) : ArrayBuffer[Declaration] = {
    val defs = items.filter(!_.isInstanceOf[AddedField])
    val addedFields = items.collect { case t : AddedField ⇒ t }

    val definitionNames = new HashMap[Name, SIDLDefinition];
    val superTypes = new HashMap[Name, ArrayBuffer[Name]]()

    // merge description and find superTypes
    for (d ← defs) {
      d match {
        case e : SIDLUserType ⇒ {
          for (n ← e.subTypes) {
            superTypes.getOrElseUpdate(n, new ArrayBuffer[Name]()).append(e.name)
          }
        }
        case e : SIDLInterface ⇒ {
          for (n ← e.subTypes) {
            superTypes.getOrElseUpdate(n, new ArrayBuffer[Name]()).append(e.name)
          }
        }
        case _ ⇒ {}
      }
      if (definitionNames.contains(d.name)) {
        val old = definitionNames(d.name)
        definitionNames(d.name) = (old, d) match {
          case (p : SIDLUserType, q : SIDLUserType) ⇒ {
            SIDLUserType(
              p.declaredIn, // TODO welches File?
              mergeDescriptions(p.description, q.description),
              p.name,
              List.empty)
          }
          case (p : SIDLEnum, q : SIDLEnum) ⇒ {
            SIDLEnum(
              p.declaredIn, // TODO welches File?
              mergeComments(p.comment, q.comment),
              p.name,
              p.instances ++ q.instances)
          }
          case (p : SIDLInterface, q : SIDLInterface) ⇒ {
            SIDLInterface(
              p.declaredIn, // TODO welches File?
              mergeComments(p.comment, q.comment),
              p.name,
              List.empty)
          }
          case _ ⇒ ParseException("TODO")
        }
      } else {
        definitionNames.put(d.name, d)
      }
    }

    // add declarations for types only mentioned in subtype specifications
    for ((n, s) ← superTypes) {
      definitionNames.getOrElseUpdate(
        n,
        SIDLUserType(null, new Description(Comment.NoComment.get, List.empty, List.empty), n, List.empty))
    }

    val astNames = new HashMap[Name, Declaration];

    // convert to AST nodes
    for (d ← definitionNames.values) {
      astNames.put(d.name, d match {
        case d : SIDLUserType ⇒ {
          new UserType(
            d.declaredIn,
            d.description,
            d.name,
            superTypes.getOrElseUpdate(d.name, new ArrayBuffer[Name]()).to,
            List.empty)
        }
        case d : SIDLEnum ⇒ {
          new EnumDefinition(
            d.declaredIn,
            d.comment,
            d.name,
            d.instances,
            List.empty)
        }
        case d : SIDLInterface ⇒ {
          new InterfaceDefinition(
            d.declaredIn,
            d.comment,
            d.name,
            superTypes.getOrElseUpdate(d.name, new ArrayBuffer[Name]()).to,
            List.empty)
        }
        case d : SIDLTypedef ⇒ d.typedef
        case _               ⇒ ParseException("TODO")
      })
    }

    // merge fields into AST nodes
    for (addedField ← addedFields) {
      if (!(definitionNames contains addedField.name)) {
        astNames += (addedField.name ->
          new UserType(
            addedField.file,
            new Description(Comment.NoComment.get, List.empty, List.empty),
            addedField.name,
            superTypes.getOrElseUpdate(addedField.name, ArrayBuffer[Name]()).to,
            List.empty))
      }
      astNames(addedField.name) =
        astNames(addedField.name) match {
          // TODO can I just have AbstactField and t.copy(body = t.body + addedField.fs)?
          // TODO should/can I use the body as mutlable List
          case t : UserType ⇒
            new UserType(
              t.declaredIn,
              t.description,
              t.name,
              t.superTypes,
              t.body ++ addedField.fields)
          case t : EnumDefinition ⇒
            new EnumDefinition(
              t.declaredIn,
              t.comment,
              t.name,
              t.instances,
              t.body ++ addedField.fields)
          case t : InterfaceDefinition ⇒
            new InterfaceDefinition(
              t.declaredIn,
              t.comment,
              t.name,
              t.superTypes,
              t.body ++ addedField.fields)
          case d : Typedef ⇒ d
          case _           ⇒ ParseException("TODO")
        }
    }
    astNames.values.to
  }
}
