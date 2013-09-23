package de.ust.skill.parser

import java.io.File
import java.io.FileNotFoundException
import java.lang.Long
import java.nio.file.FileSystems

import scala.collection.JavaConverters.bufferAsJavaListConverter
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable.HashSet
import scala.collection.mutable.LinkedList
import scala.collection.mutable.ListBuffer
import scala.util.parsing.combinator.RegexParsers

import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.VariableLengthArrayType

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
  class FileParser extends RegexParsers {
    /**
     * Usual identifiers including arbitrary unicode characters.
     */
    def id = """[a-zA-Z_\u007f-\uffff][\w\u007f-\uffff]*""".r
    /**
     * Skill only has hex literals.
     */
    def int = "0x" ~> ("""[0-9a-fA-F]*""".r ^^ { i ⇒ Long.parseLong(i, 16) })
    /**
     * We use string literals to encode paths. If someone really calls a file ", someone should beat him hard.
     */
    def string = "\"" ~> """[^"]*""".r <~ "\""

    /**
     * A file is a list of includes followed by a list of declarations.
     */
    def file = rep(includes) ~! rep(declaration) ^^ {
      case i ~ d ⇒ (i, d)
    }

    /**
     * Includes are just strings containing relative paths to *our* path.
     */
    def includes = ("include" | "with") ~> string <~ opt(";");

    /**
     * Comments are first class citizens of our language, because we want to emit them in the output binding.
     */
    def comment = """/\*([^\*/]|/|\*+[^\*/])*\*+/""".r

    /**
     * restrictions as defined in the paper.
     */
    def restriction = "@" ~> id ~ opt("(" ~> repsep((int | "%"), ",") <~ ")") <~ opt(";") ^^ {
      case s ~ arg ⇒ new Restriction(s, arg.getOrElse(List[Any]()))
    }
    /**
     * hints as defined in the paper. Because hints can be ignored by the generator, it is safe to allow arbitrary
     * identifiers and to warn if the identifier is not a known hint.
     */
    def hint = "!" ~> (id <~ opt(";")) ^^ { new Hint(_) }

    /**
     * Description of a declration or field.
     */
    def description = rep(restriction | hint) ~ opt(comment) ~ rep(restriction | hint) ^^ {
      case l1 ~ c ~ l2 ⇒ {
        val l = l1 ++ l2;
        new Description(c, l.filter(p ⇒ p.isInstanceOf[Restriction]).asInstanceOf[List[Restriction]],
          l.filter(p ⇒ p.isInstanceOf[Hint]).asInstanceOf[List[Hint]])
      }
    }

    /**
     * A declaration may start with a description, is followed by modifiers and a name, might have a super class and has
     * a body.
     */
    def declaration = description ~ id ~ opt((":" | "with" | "extends") ~> id) ~! body ^^ {
      case c ~ n ~ s ~ b ⇒ new Definition(c, n, s, b)
    }

    /**
     * A body consist of a list of fields.
     */
    def body = "{" ~> rep(field) <~ "}"

    /**
     * A field is either a constant or a real data field.
     */
    def field = description ~ (constant | data) ^^ { case d ~ f ⇒ { f.description = d; f } }

    /**
     * Constants a recognized by the keyword "const" and are required to have a value.
     */
    def constant = "const" ~> Type ~! id ~! ("=" ~> int) <~ opt(";") ^^ { case t ~ n ~ v ⇒ new Constant(t, n, v) }

    /**
     * Data may be marked to be auto and will therefore only be present at runtime.
     */
    def data = opt("auto") ~ Type ~! id <~ opt(";") ^^ { case a ~ t ~ n ⇒ new Data(a.isDefined, t, n) }

    /**
     * Unfortunately, the straigth forward definition of this would lead to recursive types, thus we disallowed ADTs as
     * arguments to maps. Please note that this does not prohibit formulation of any structure, although it might
     * require the introduction of declarations, which essentially rename another more complex type. This has also an
     * impact on the way, data is and can be stored.
     */
    def Type = ((("map" | "set" | "list") ~! ("<" ~> repsep(baseType, ",") <~ ">")) ^^ {
      case "map" ~ l  ⇒ new de.ust.skill.parser.MapType(l)
      case "set" ~ l  ⇒ { assert(1 == l.size); new de.ust.skill.parser.SetType(l.head) }
      case "list" ~ l ⇒ { assert(1 == l.size); new de.ust.skill.parser.ListType(l.head) }
    }
      // we use a backtracking approach here, because it simplifies the AST generation
      | arrayType
      | baseType)

    def arrayType = ((baseType ~ ("[" ~> int <~ "]")) ^^ { case n ~ arr ⇒ new ConstantArrayType(n, arr) }
      | (baseType <~ ("[" ~ "]")) ^^ { n ⇒ new ArrayType(n) })

    def baseType = id ^^ { new BaseType(_) }

    /**
     * The <b>main</b> function of the parser, which turn a string into a list of includes and declarations.
     */
    def process(in: String) = parseAll(file, in) match {
      case Success(rval, _) ⇒ rval
      case f                ⇒ println(f); throw new Exception("parsing failed: "+f);
    }
  }

  /**
   * returns an unsorted list of declarations
   */
  def process(input: File) = buildIR(parseAll(input))

  /**
   * Parses a file and all related files and passes back a List of definitions. The returned definitions are also type
   * checked.
   */
  private[parser] def parseAll(input: File): LinkedList[Definition] = {
    val parser = new FileParser();
    val base = input.getParentFile();
    val todo = new HashSet[String]();
    todo.add(input.getName());
    val done = new HashSet[String]();
    var rval = new LinkedList[Definition]();
    while (!todo.isEmpty) {
      val file = todo.head
      todo -= file;
      if (!done.contains(file)) {
        done += file;

        try {
          val lines = scala.io.Source.fromFile(new File(base, file), "utf-8").getLines.mkString(" ")

          val result = parser.process(lines)

          // add includes to the todo list
          result._1.foreach(todo += _)

          // add definitions
          rval = rval ++ result._2
        } catch {
          case e: FileNotFoundException ⇒ assert(false, "The include "+file+
            "could not be resolved to an existing file: "+e.getMessage()+"\nWD:"+FileSystems.getDefault().getPath(".").toAbsolutePath().toString())
        }
      }
    }
    TypeChecker.check(rval.toList)
    rval;
  }

  private def mkType(s: Type, decls: Map[String, Declaration]): de.ust.skill.ir.Type = s match {
    case t: ListType          ⇒ new de.ust.skill.ir.ListType(mkType(t.baseType, decls))
    case t: SetType           ⇒ new de.ust.skill.ir.SetType(mkType(t.baseType, decls))
    case t: MapType           ⇒ new de.ust.skill.ir.MapType(t.args.map(mkType(_, decls)).asJava)

    case t: ConstantArrayType ⇒ new ConstantLengthArrayType(mkType(t.baseType, decls), t.length)
    case t: ArrayType         ⇒ new VariableLengthArrayType(mkType(t.baseType, decls))

    case t: BaseType ⇒ decls.get(t.name).getOrElse(GroundType.get(t.name)).ensuring({
      r ⇒ if (null == r) throw new IllegalStateException("unknown declaration name "+t.name); true
    })
  }

  /**
   * Turns the AST into IR.
   */
  private def buildIR(defs: LinkedList[Definition]): java.util.List[Declaration] = {
    // create declarations
    var parents = defs.map(f ⇒ (f.name, f)).toMap
    val rval = parents.map({ case (n, f) ⇒ (n, new Declaration(n)) })
    rval.foreach({
      case (n, f) ⇒ f.setParentType(
        rval.get(parents.get(n).get.parent.getOrElse(null)).getOrElse(null))
    })

    // fill field information into declarations
    parents.foreach({
      case (n, f) ⇒
        val fields = new ListBuffer[de.ust.skill.ir.Field]()
        f.body.foreach({
          case x: Data     ⇒ fields += new de.ust.skill.ir.Data(x.isAuto, mkType(x.t, rval), x.name)
          case x: Constant ⇒ fields += new de.ust.skill.ir.Constant(mkType(x.t, rval), x.name, x.value)
        })
        rval.get(n).get.setFields(fields.asJava)
    })

    rval.values.toList.asJava
  }
}