package de.ust.skill.parser

import java.io.File
import scala.annotation.elidable
import scala.annotation.migration
import scala.collection.mutable.HashSet
import scala.collection.mutable.LinkedList
import scala.util.parsing.combinator.JavaTokenParsers
import annotation.elidable.ASSERTION
import java.io.FileNotFoundException

/**
 * The Parser does all stuff that is required for turning a set of files into a list of definitions.
 * @see #process
 * @author Timm Felden
 */
class Parser {

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
     * Skill only has hex literals.
     */
    def int = "0x" ~> """[0-9a-fA-F]*""".r ^^ { i => Integer.parseInt(i, 16) }
    /**
     * We use string literals to encode paths. If someone really calls a file ", someone should beat him hard.
     */
    def string = "\"" ~> """[^"]*""".r <~ "\""

    /**
     * A file is a list of includes followed by a list of declarations.
     */
    def file = rep(includes) ~! rep(declaration) ^^ { case i ~ d => (i, d) }

    /**
     * Includes are just strings containing relative paths to *our* path.
     */
    def includes = ("include" | "with") ~> string <~ opt(";");

    /**
     * Comments are first class citizens of our language, because we want to emit them in the output binding.
     */
    def comment = """(\s|//.*|(?m)/\*(\*(?!/)|[^*])*\*/)*""".r

    /**
     * restrictions as defined in the paper.
     */
    def restriction = "@" ~> id ~ opt("(" ~> repsep((int | "%"), ",") <~ ")") ^^ {
      case s ~ arg => new Restriction(s, arg.getOrElse(List[Any]()))
    }
    /**
     * hints as defined in the paper. Because hints can be ignored by the generator, it is safe to allow arbitrary
     * identifiers and to warn if the identifier is not a known hint.
     */
    def hint = "!" ~> id ^^ { case s => new Hint(s) }

    /**
     * Description of a declration or field.
     */
    def description = rep(restriction | hint) ~ opt(comment) ~ rep(restriction | hint) ^^ {
      case l1 ~ c ~ l2 => {
        val l = l1 ++ l2;
        new Description(c, l.filter(p => p.isInstanceOf[Restriction]).asInstanceOf[List[Restriction]],
          l.filter(p => p.isInstanceOf[Hint]).asInstanceOf[List[Hint]])
      }
    }

    /**
     * A declaration may start with a description, is followed by modifiers and a name, might have a super class and has
     * a body.
     */
    def declaration = description ~ modifier ~ id ~! opt((":" | "with") ~> id) ~! body ^^
      { case c ~ m ~ n ~ s ~ b => new Definition(c, m, n, s, b) }

    /**
     * Tells us whether a declaratino is a class, an annotation or just plain data.
     */
    def modifier = rep("tagged" | "class" | "annotation") ^^
      { m => (m.contains("class") || m.contains("tagged"), m.contains("annotation")) }

    /**
     * A body consist of a nonempty list of fields.
     */
    def body = "{" ~> rep(field) <~ "}"

    /**
     * A field is either a constant or a real data field.
     */
    def field = description ~! (constant | data) ^^ { case d ~ f => { f.description = d; f } }

    /**
     * Constants a recognized by the keyword "const" and are required to have a value.
     */
    def constant = "const" ~> Type ~! id ~! ("=" ~> int) <~ opt(";") ^^ { case t ~ n ~ v => new Constant(t, n, v) }

    /**
     * Data may be marked to be auto and will therefore only be present at runtime.
     */
    def data = opt("auto") ~! Type ~! id <~ opt(";") ^^ { case a ~ t ~ n => new Data(a.isDefined, t, n) }

    /**
     * Unfortunately, the straigth forward definition of this would lead to recursive types, thus we disallowed ADTs as
     * arguments to maps. Please note that this does not prohibit formulation of any structure, although it might
     * require the introduction of declarations, which essentially rename another more complex type. This has also an
     * impact on the way, data is and can be stored.
     */
    def Type = ((("map" | "set" | "list") ~! ("<" ~> repsep(id, ",") <~ ">")) ^^ {
      case "map" ~ l => new de.ust.skill.parser.MapType(l)
      case "set" ~ l => { assert(1 == l.size); new de.ust.skill.parser.SetType(l.head) }
      case "list" ~ l => { assert(1 == l.size); new de.ust.skill.parser.ListType(l.head) }
    }
      // we use a backtracking approach here, because it simplifies the AST generation
      | ((id ~ ("[" ~> int <~ "]")) ^^ { case n ~ arr => new ConstantArrayType(n, arr) }
        | (id ~ ("[" ~> id <~ "]")) ^^ { case n ~ arr => new DependentArrayType(n, arr) }
        | (id <~ ("[" ~ "]")) ^^ { n => new ArrayType(n) }
        | (id) ^^ { case n => new GroundType(n) }))

    /**
     * The <b>main</b> function of the parser, which turn a string into a list of includes and declarations.
     */
    def process(in: String) = parseAll(phrase(file), in) match {
      case Success(rval, _) => rval
      case f => println(f); throw new Exception("parsing failed: " + f);
    }
  }

  /**
   * Parses a file and all related files and passes back a List of definitions. The returned definitions are also type
   * checked.
   */
  def process(input: File): LinkedList[Definition] = {
    val p = new FileParser();
    val base = input.getParentFile();
    val todo = new HashSet[String]();
    todo.add(input.getName());
    val done = new HashSet[String]();
    var rval = new LinkedList[Definition]();
    while (!todo.isEmpty) {
      val f = todo.head
      todo -= f;
      if (!done.contains(f)) {
        done += f;

        try {
          val lines = scala.io.Source.fromFile(new File(base, f), "utf-8").getLines.mkString(" ")

          val result = p.process(lines)

          // add includes to the todo list
          result._1.foreach(todo += _)

          // add definitions
          rval = rval ++ result._2
        } catch {
          case e: FileNotFoundException => assert(false, "The include " + f + "could not be resolved to an existing file: " + e.getMessage())
        }
      }
    }
    (new TypeChecker).check(rval.toList)
    return rval;
  }
}