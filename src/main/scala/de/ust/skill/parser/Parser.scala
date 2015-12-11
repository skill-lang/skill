/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import java.io.File
import java.io.FileNotFoundException
import java.lang.Long
import java.nio.file.FileSystems
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.util.parsing.combinator.RegexParsers
import de.ust.skill.ir
import de.ust.skill.ir.Hint
import de.ust.skill.ir.Restriction
import de.ust.skill.ir.restriction.ConstantLengthPointerRestriction
import de.ust.skill.ir.restriction.FloatRangeRestriction
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.MonotoneRestriction
import de.ust.skill.ir.restriction.NonNullRestriction
import de.ust.skill.ir.restriction.SingletonRestriction
import de.ust.skill.ir.restriction.UniqueRestriction
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Stack
import de.ust.skill.ir.Comment
import de.ust.skill.ir.TypeContext
import scala.collection.JavaConversions._
import de.ust.skill.ir.restriction.AbstractRestriction

/**
 * The Parser does everything required for turning a set of files into a list of definitions.
 * @see #process
 * @see SKilL V1.0 Appendix A
 * @author Timm Felden
 * @param delimitWithUnderscore if true, underscores in words are used as delimiters. This will influence name
 * equivalence
 */
final class Parser(delimitWithUnderscore : Boolean = true, delimitWithCamelCase : Boolean = true, verboseOutput : Boolean = false) {
  def stringToName(name : String) : Name = new Name(name, delimitWithUnderscore, delimitWithCamelCase)

  val tc = new ir.TypeContext

  /**
   * Converts a character stream into an AST using parser combinators.
   *
   * Grammar as explained in the paper.
   */
  final class FileParser extends RegexParsers {
    var currentFile : File = _

    /**
     * Usual identifiers including arbitrary unicode characters.
     */
    private def id = positioned[Name]("""[a-zA-Z_\u007f-\uffff][\w\u007f-\uffff]*""".r ^^ stringToName)
    /**
     * Skill integer literals
     */
    private def int : Parser[Long] = hexInt | generalInt
    private def hexInt : Parser[Long] = "0x" ~> ("""[0-9a-fA-F]*""".r ^^ { i ⇒ Long.parseLong(i, 16) })
    private def generalInt : Parser[Long] = """-?[0-9]*\.*""".r >> { i ⇒
      try {
        success(Long.parseLong(i))
      } catch {
        case e : Exception ⇒ failure("not an int")
      }
    }

    /**
     * Floating point literal, as taken from the JavaTokenParsers definition.
     *
     * @note if the target can be an integer as well, the integer check has to come first
     */
    def floatingPointNumber : Parser[Double] = """-?(\d+(\.\d*)?|\d*\.\d+)([eE][+-]?\d+)?[fFdD]?""".r ^^ { _.toDouble }

    /**
     * We use string literals to encode paths. If someone really calls a file ", someone should beat him hard.
     */
    private def string = "\"" ~> """[^"]*""".r <~ "\""

    /**
     * A file is a list of includes followed by a list of declarations.
     */
    private def file = headComment ~> rep(includes) ~! rep(declaration) ^^ {
      case i ~ d ⇒ (i.fold(List[String]())(_ ++ _), d)
    }

    /**
     * Files may start with an arbitrary of lines starting with '#'
     * Theses lines sereve as true comments and do not affect the specification.
     */
    private def headComment = rep("""^#[^\r\n]*[\r\n]*""".r)

    /**
     * Includes are just strings containing relative paths to *our* path.
     */
    private def includes = ("include" | "with") ~> rep(
      string ^^ { s ⇒ new File(currentFile.getParentFile, s).getAbsolutePath }
    );

    /**
     * Declarations add or modify user defined types.
     */
    private def declaration : Parser[Declaration] = typedef | enumType | interfaceType | userType

    /**
     * creates a shorthand for a more complex type
     */
    private def typedef = opt(comment) ~ ("typedef" ~> id) ~ rep(fieldRestriction | hint) ~ fieldType <~ ";" ^^ {
      case c ~ name ~ specs ~ target ⇒ Typedef(
        currentFile,
        name,
        new Description(
          c.getOrElse(Comment.NoComment.get),
          specs.collect { case r : Restriction ⇒ r },
          specs.collect { case h : Hint ⇒ h }
        ),
        target)
    };

    /**
     * A declaration may start with a description, is followed by modifiers and a name, might have a super class and has
     * a body.
     */
    private def userType = typeDescription ~ id ~ rep((":" | "with" | "extends") ~> id) ~!
      ("{" ~> rep(field) <~ "}") ^^ {
        case d ~ n ~ s ~ b ⇒ new UserType(currentFile, d, n, s, b)
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
        case c ~ n ~ i ~ f ⇒ new InterfaceDefinition(currentFile, c.getOrElse(Comment.NoComment.get), n, i, f)
      }

    /**
     * A field is either a constant or a real data field.
     */
    private def field = (
      (opt(comment) ^^ { c ⇒ c.getOrElse(Comment.NoComment.get) }) >> { c ⇒ view(c) | customField(c) } <~ ";"
      | fieldDescription ~ ((constant | data) <~ ";") ^^ { case d ~ f ⇒ { f.description = d; f } }
    )

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
     * A field with language custom properties. This field will almost behave like an auto field.
     */
    private def customField(c : Comment) = ("custom" ~> id) ~ (rep(
      ("!" ~> id ~ (opt(string) ^^ { s ⇒ s.toList } | ("(" ~> rep(string) <~ ")"))) ^^ {
        case n ~ args ⇒ n -> args
      }) ^^ { s ⇒ s.toMap }) ~ string ~! id <~ ";" ^^ {
        case lang ~ opts ~ t ~ n ⇒ new Customization(c, lang, opts, t, n)
      }

    /**
     * Unfortunately, the straight forward definition of this would lead to recursive types, thus we disallowed ADTs as
     * arguments to maps. Please note that this does not prohibit formulation of any structure, although it might
     * require the introduction of declarations, which essentially rename another more complex type. This has also an
     * impact on the way, data is and can be stored.
     */
    private def fieldType = ((("map" | "set" | "list") ~! ("<" ~> repsep(baseType, ",") <~ ">")) ^^ {
      case "map" ~ l ⇒ {
        if (1 >= l.size)
          throw ParseException(s"Did you mean set<${l.mkString}> instead of map?")
        else
          new de.ust.skill.parser.MapType(l)
      }
      case "set" ~ l ⇒ {
        if (1 != l.size)
          throw ParseException(s"Did you mean map<${l.mkString}> instead of set?")
        else
          new de.ust.skill.parser.SetType(l.head)
      }
      case "list" ~ l ⇒ {
        if (1 != l.size)
          throw ParseException(s"Did you mean map<${l.mkString}> instead of list?")
        else
          new de.ust.skill.parser.ListType(l.head)
      }
    }
      // we use a backtracking approach here, because it simplifies the AST generation
      | arrayType
      | baseType)

    private def arrayType = ((baseType ~ ("[" ~> int <~ "]")) ^^ { case n ~ arr ⇒ new ConstantLengthArrayType(n, arr) }
      | (baseType <~ ("[" ~ "]")) ^^ { n ⇒ new ArrayType(n) })

    private def baseType = id ^^ { new BaseType(_) }

    /**
     * Comments are first class citizens of our language, because we want to emit them in the output binding.
     *
     * The intermediate representation is without the leading "/°" and trailing "°/" (where °=*)
     */
    private def comment : Parser[Comment] = """/\*+""".r ~> ("""([^\*/]|/|\*+[^\*/])*\*+/""".r) ^^ { s ⇒
      // scan s to split it into pieces
      @inline def scan(last : Int) : ListBuffer[String] = {
        var begin = 0;
        var next = 0;
        // we have to insert a line break, because the whitespace handling may have removed one
        var r = ListBuffer[String]("\n")
        while (next < last) {
          s.charAt(next) match {
            case ' ' | '\t' | 0x0B | '\f' | '\r' ⇒
              if (begin != next)
                r.append(s.substring(begin, next))
              begin = next + 1;
            case '\n' ⇒
              if (begin != next)
                r.append(s.substring(begin, next))
              r.append("\n")
              begin = next + 1;
            case _ ⇒
          }
          next += 1
        }
        if (begin != last) r.append(s.substring(begin, last))
        r
      }

      val ws = scan(s.size - 2)

      val r = new Comment

      @tailrec def parse(ws : ListBuffer[String], text : ListBuffer[String]) : Unit =
        if (ws.isEmpty) r.init(text)
        else (ws.head, ws.tail) match {
          case ("\n", ws) if (ws.isEmpty)     ⇒ r.init(text)
          case ("\n", ws) if (ws.head == "*") ⇒ parse(ws.tail, text)
          case ("\n", ws)                     ⇒ parse(ws, text)
          case (w, ws) if w.matches("""\*?@.+""") ⇒
            val end = if (w.contains(":")) w.lastIndexOf(':') else w.size
            val tag = w.substring(w.indexOf('@') + 1, end).toLowerCase
            r.init(text, tag); parse(ws, ListBuffer[String]())
          case (w, ws) ⇒ text.append(w); parse(ws, text)
        }

      parse(ws, ListBuffer[String]())

      r
    }

    /**
     * restrictions as defined in the paper.
     *
     * @note the implementation is more liberal then the specification of the specification language, because some illegal arguments are dropped
     */
    private def typeRestriction : Parser[Restriction] = "@" ~> id >> {
      _.lowercase match {
        case "unique"    ⇒ opt("(" ~ ")") ^^ { _ ⇒ new UniqueRestriction }

        case "singleton" ⇒ opt("(" ~ ")") ^^ { _ ⇒ new SingletonRestriction }

        case "monotone"  ⇒ opt("(" ~ ")") ^^ { _ ⇒ new MonotoneRestriction }

        case "abstract"  ⇒ opt("(" ~ ")") ^^ { _ ⇒ new AbstractRestriction }

        case "default"   ⇒ "(" ~> defaultRestrictionParameter <~ ")" ^^ { _ ⇒ null }

        case unknown ⇒ opt("(" ~> repsep((int | string | floatingPointNumber), ",") <~ ")") ^^ { arg ⇒
          ParseException(s"$unknown${
            arg.mkString("(", ", ", ")")
          } is either not supported or an invalid restriction name")
        }
      }
    }
    private def fieldRestriction : Parser[Restriction] = "@" ~> id >> {
      _.lowercase match {

        case "nonnull" ⇒ opt("(" ~ ")") ^^ { _ ⇒ new NonNullRestriction }

        case "default" ⇒ "(" ~> defaultRestrictionParameter <~ ")" ^^ { _ ⇒ null }

        case "min" ⇒ "(" ~> (
          int ~ opt("," ~> string) ^^ {
            case low ~ None              ⇒ new IntRangeRestriction(low, Long.MAX_VALUE, true, true)
            case low ~ Some("inclusive") ⇒ new IntRangeRestriction(low, Long.MAX_VALUE, true, true)
            case low ~ Some("exclusive") ⇒ new IntRangeRestriction(low, Long.MAX_VALUE, false, true)
          }
          |
          floatingPointNumber ~ opt("," ~> string) ^^ {
            case low ~ None              ⇒ new FloatRangeRestriction(low, Double.MaxValue, true, true)
            case low ~ Some("inclusive") ⇒ new FloatRangeRestriction(low, Double.MaxValue, true, true)
            case low ~ Some("exclusive") ⇒ new FloatRangeRestriction(low, Double.MaxValue, false, true)
          }
        ) <~ ")"

        case "max" ⇒ "(" ~> (
          int ~ opt("," ~> string) ^^ {
            case high ~ None              ⇒ new IntRangeRestriction(Long.MIN_VALUE, high, true, true)
            case high ~ Some("inclusive") ⇒ new IntRangeRestriction(Long.MIN_VALUE, high, true, true)
            case high ~ Some("exclusive") ⇒ new IntRangeRestriction(Long.MIN_VALUE, high, true, false)
          }
          |
          floatingPointNumber ~ opt("," ~> string) ^^ {
            case high ~ None              ⇒ new FloatRangeRestriction(Double.MinValue, high, true, true)
            case high ~ Some("inclusive") ⇒ new FloatRangeRestriction(Double.MinValue, high, true, true)
            case high ~ Some("exclusive") ⇒ new FloatRangeRestriction(Double.MinValue, high, true, false)
          }
        ) <~ ")"

        case "range" ⇒ "(" ~> (
          int ~ ("," ~> int) ~ opt("," ~> string ~ ("," ~> string)) ^^ {
            case low ~ high ~ None        ⇒ new IntRangeRestriction(low, high, true, true)
            case low ~ high ~ Some(l ~ h) ⇒ new IntRangeRestriction(low, high, "inclusive" == l, "inclusive" == h)
          }
          |
          floatingPointNumber ~ ("," ~> floatingPointNumber) ~ opt("," ~> string ~ ("," ~> string)) ^^ {
            case low ~ high ~ None        ⇒ new FloatRangeRestriction(low, high, true, true)
            case low ~ high ~ Some(l ~ h) ⇒ new FloatRangeRestriction(low, high, "inclusive" == l, "inclusive" == h)
          }
        ) <~ ")"

        case "coding"                ⇒ ("(" ~> string <~ ")") ^^ { _ ⇒ null }

        case "constantlengthpointer" ⇒ opt("(" ~ ")") ^^ { _ ⇒ new ConstantLengthPointerRestriction }

        case "oneof"                 ⇒ ("(" ~> repsep(id, ",") <~ ")") ^^ { case types ⇒ null }

        case unknown ⇒ opt("(" ~> repsep((int | string | floatingPointNumber), ",") <~ ")") ^^ { arg ⇒
          ParseException(s"$unknown${
            arg.mkString("(", ", ", ")")
          } is either not supported or an invalid restriction name")
        }
      }
    }

    private def defaultRestrictionParameter = int | string | floatingPointNumber | repsep(id, "." | "::")

    /**
     * hints as defined in the paper. Because hints can be ignored by the generator, it is safe to allow arbitrary
     * identifiers and to warn if the identifier is not a known hint.
     */
    private def hint = "!" ~> id >> { name ⇒
      name.lowercase match {
        case "constantmutator" ⇒ success(name) ~ (("(" ~> int ~ ("," ~> int <~ ")")) ^^ {
          case min ~ max ⇒ List(stringToName(min.toString), stringToName(max.toString))
        })
        case "provider" | "owner" ⇒ success(name) ~ ("(" ~> repsep(id, ",") <~ ")")
        case "pragma" ⇒ success(name) ~ ((id ~ opt("(" ~> repsep(id, ",") <~ ")")) ^^ {
          case f ~ fs ⇒ List(f) ++ fs.getOrElse(Nil)
        })
        case _ ⇒ success(name) ~ success(List[Name]())
      }
    } ^^ {
      case n ~ args ⇒
        try {
          Hint.get(Hint.Type.valueOf(n.lowercase), args.map(_.ir))
        } catch { case e : IllegalArgumentException ⇒ throw ParseException(s"$n is not the name of a hint.") }
    }

    /**
     * Description of a field.
     */
    private def fieldDescription = opt(comment) ~ rep(fieldRestriction | hint) ^^ {
      case c ~ specs ⇒ new Description(c.getOrElse(Comment.NoComment.get), specs.collect { case r : Restriction ⇒ r }, specs.collect { case h : Hint ⇒ h })
    }
    /**
     * Description of a declration.
     */
    private def typeDescription = opt(comment) ~ rep(typeRestriction | hint) ^^ {
      case c ~ specs ⇒ new Description(c.getOrElse(Comment.NoComment.get), specs.collect { case r : Restriction ⇒ r }, specs.collect { case h : Hint ⇒ h })
    }

    /**
     * The <b>main</b> function of the parser, which turn a string into a list of includes and declarations.
     */
    def process(in : File) : (List[String], List[Declaration]) = {
      currentFile = in;
      val lines = scala.io.Source.fromFile(in, "utf-8").getLines.mkString("\n")

      parseAll(file, lines) match {
        case Success(rval, _) ⇒ rval
        case f                ⇒ ParseException(s"parsing failed in ${in.getName}: $f");
      }
    }
  }

  /**
   * Parses a file and all related files and passes back a List of definitions. The returned definitions are also type
   * checked.
   */
  private def parseAll(input : File) = {
    val parser = new FileParser();
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

object Parser {

  /**
   * @return a type context containing all type information obtained from the argument file
   */
  def process(input : File, delimitWithUnderscore : Boolean = true, delimitWithCamelCase : Boolean = true, verboseOutput : Boolean = false) : TypeContext = {
    val p = new Parser(delimitWithUnderscore, delimitWithCamelCase, verboseOutput)
    IRBuilder.buildIR(p.parseAll(input).to, verboseOutput)
  }
}
