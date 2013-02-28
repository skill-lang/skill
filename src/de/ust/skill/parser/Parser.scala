package de.ust.skill.parser

import java.io.File
import de.ust.skill.ast.Definition
import scala.util.parsing.combinator.RegexParsers
import scala.collection.mutable.HashSet
import scala.collection.mutable.LinkedList
import scala.util.parsing.combinator.JavaTokenParsers

/**
 * The Parser does all stuff that is required for turning a set of files into a list of definitions.
 * @see #process
 * @author Timm Felden
 */
class Parser {
  sealed abstract class TypeNode;
  case class MapType(dom: TypeNode, range: TypeNode) extends TypeNode;
  case class SetType(dom: TypeNode) extends TypeNode;
  case class ListType(dom: TypeNode) extends TypeNode;
  case class GroundType(name: String) extends TypeNode;

  /**
   * Converts a character stream into an AST using parser combinators.
   *
   * Grammar as explained in the paper.
   */
  class FileParser extends JavaTokenParsers {
    /**
     * Usual identifiers including arbitrary unicode characters.
     */
    def id = """[a-zA-Z_\u00ff-\uffff][\w\u00ff-\uffff]*""".r
    /**
     * Hex literals.
     */
    def int = "0x" ~> """[0-9a-fA-F]*""".r ^^ { i => Integer.parseInt(i,16)}
    def string = stringLiteral

    def file = rep(includes) ~! rep(declaration) ^^ { case i ~ d => (i, d) }

    def includes = ("include" | "with") ~> string <~ opt(";");

    def comment = """(\s|//.*|(?m)/\*(\*(?!/)|[^*])*\*/)*""".r

    def declaration = opt(comment) ~! modifier ~! id ~! opt((":"|"with") ~> id) ~! body ^^ { case c ~ m ~ n ~ s ~ b => new Definition(c, m, n, s, b) }

    def modifier = rep("tagged" | "class" | "annotation") ^^ { m => (m.contains("class") || m.contains("tagged"), m.contains("annotation")) }

    def body = "{" ~> rep(field) <~ "}"

    def field = opt(comment) ~! (constant | data)

    def constant = "const" ~> Type ~! id ~! ("=" ~> int) <~ opt(";")
    def data = opt("auto") ~! Type ~! id <~ opt(";")

    def Type = ((("map" | "set" | "list") ~! ("<" ~> repsep(id, ",") <~ ">"))
      | (id ~! opt("[" ~> opt(id | int) <~ "]")))

    def process(in: String) = parseAll(phrase(file), in) match {
      case Success(rval, _) => rval
      case f => println(f); throw new Exception("parsing failed: " + f);
    }
  }

  /**
   * Parses a file and all related files and passes back a List of definitions. The returned definitions are also type checked.
   */
  def process(input: File): LinkedList[Definition] = {
    val p = new FileParser();
    val todo = new HashSet[File]();
    todo.add(input);
    val done = new HashSet[File]();
    var rval = new LinkedList[Definition]();
    while (!todo.isEmpty) {
      val f = todo.first
      todo -= f;
      val lines = scala.io.Source.fromFile(f.getAbsolutePath(), "utf-8").getLines.mkString

      val result = p.process(lines)

      // add includes to the todo list

      // add definitions
      rval = rval ++ result._2
    }
    return rval;
  }
}