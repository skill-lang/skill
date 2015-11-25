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
    private def includes = ("include" | "with") ~> rep(string);

    /**
     * Declarations add or modify user defined types.
     */
    private def declaration : Parser[Declaration] = typedef | enumType | interfaceType | fieldChange | userType

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
    private def userType = opt(changeModifier) ~ typeDescription ~ id ~ rep((":" | "with" | "extends") ~> id) ~!
      ("{" ~> rep(field) <~ "}") ^^ {
        case c ~ d ~ n ~ s ~ b ⇒ UserType(currentFile, c, d, n, s, b)
      }

    /**
     * modifier prefix to type declarations if they are meant to be modified
     */
    private def changeModifier = (
      "++" ^^ { _ ⇒ ChangeModifier.++ }
      | "--" ^^ { _ ⇒ ChangeModifier.-- }
      | "==" ^^ { _ ⇒ ChangeModifier.set }
    )

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
    private def interfaceType = opt(comment) ~ ("interface" ~> id) ~ rep((":" | "with" | "extends") ~> id) ~ ("{" ~> rep(field) <~ "}") ^^ {
      case c ~ n ~ i ~ f ⇒ new InterfaceDefinition(currentFile, c.getOrElse(Comment.NoComment.get), n, i, f)
    }

    /**
     * modify the definition of a field
     */
    private def fieldChange = changeModifier ~ fieldDescription ~ opt("auto") ~ fieldType ~ id ~ ("." ~> id <~ ";") ^^ {
      case c ~ d ~ a ~ t ~ cls ~ name ⇒ ???
    }

    /**
     * A field is either a constant or a real data field.
     */
    private def field = fieldDescription ~ ((view | constant | data) <~ ";") ^^ { case d ~ f ⇒ { f.description = d; f } }

    /**
     * View an existing view as something else.
     */
    private def view = ("view" ~> opt(id <~ ".")) ~ (id <~ "as") ~ data ^^ { case t ~ f ~ target ⇒ new View(t, f, target) }

    /**
     * Constants a recognized by the keyword "const" and are required to have a value.
     */
    private def constant = "const" ~> fieldType ~! id ~! ("=" ~> int) ^^ { case t ~ n ~ v ⇒ new Constant(t, n, v) }

    /**
     * Data may be marked to be auto and will therefore only be present at runtime.
     */
    private def data = opt("auto") ~ fieldType ~! id ^^ { case a ~ t ~ n ⇒ new Data(a.isDefined, t, n) }

    /**
     * Unfortunately, the straigth forward definition of this would lead to recursive types, thus we disallowed ADTs as
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
    val base = input.getParentFile();
    val todo = new HashSet[String]();
    todo.add(input.getName());
    val done = new HashSet[String]();
    var rval = new ArrayBuffer[Declaration]();
    while (!todo.isEmpty) {
      val file = todo.head
      todo -= file;
      if (!done.contains(file)) {
        done += file;

        try {
          val result = parser.process(new File(base, file))

          // add includes to the todo list
          result._1.foreach(todo += _)

          // add definitions
          rval = rval ++ result._2
          if (verboseOutput)
            println(s"acc: $file ⇒ ${rval.size}")
        } catch {
          case e : FileNotFoundException ⇒ ParseException(
            s"The include $file could not be resolved to an existing file: ${e.getMessage()} \nWD: ${
              FileSystems.getDefault().getPath(".").toAbsolutePath().toString()
            }"
          )
        }
      }
    }
    rval
  }

  /**
   * Turns the AST into IR.
   *
   * TODO the type checking should be separated and IR building should start over with the original AST and the
   * knowledge, that it is in fact correct
   */
  private def buildIR(defs : ArrayBuffer[Declaration]) : TypeContext = {

    // run the type checker to get information about the type hierarchy
    val (baseType, parent, superInterfaces) = TypeCheck(defs)

    // skillname ⇀ definition
    val definitionNames = new HashMap[Name, Declaration];
    for (d ← defs) definitionNames.put(d.name, d)

    // create declarations
    val toIRByName = definitionNames.map({
      case (n, f : UserType) ⇒
        (n, ir.UserType.newDeclaration(
          tc,
          n.ir,
          f.description.comment,
          f.description.restrictions,
          f.description.hints
        ))

      case (n, f : InterfaceDefinition) ⇒
        (n, ir.InterfaceType.newDeclaration(
          tc,
          n.ir,
          f.comment
        ))

      case (n, f : EnumDefinition) ⇒
        (n, ir.EnumType.newDeclaration(
          tc,
          n.ir,
          f.comment,
          f.instances.map(_.ir)
        ))

      case (n, f : Typedef) ⇒
        (n, ir.Typedef.newDeclaration(
          tc,
          n.ir,
          f.description.comment,
          f.description.restrictions,
          f.description.hints
        ))
    });
    @inline def toIR(d : Declaration) : ir.Declaration = toIRByName(d.name)

    // topological sort results into a list, in order to be able to intialize it correctly
    /**
     *  tarjan with lexical order of edges; not exactly the solution described in the TR but it works well
     */
    def topologicalSort(nodes : Iterable[Declaration]) : ListBuffer[Declaration] = {
      // create edges, subtypes in alphabetical order
      var edges = defs.map(_ -> ArrayBuffer[Declaration]()).toMap
      nodes.foreach {
        case t : UserType            ⇒ t.superTypes.map(definitionNames).foreach(edges(_) += t)
        case t : InterfaceDefinition ⇒ t.superTypes.map(definitionNames).foreach(edges(_) += t)
        case t : Typedef if (t.target.isInstanceOf[BaseType]) ⇒
          definitionNames.get(t.target.asInstanceOf[BaseType].name).foreach(edges(_) += t)
        case _ ⇒
      }
      //@note lexical order in edges
      edges = edges.map { case (k, e) ⇒ (k, e.sortWith(_.name.lowercase > _.name.lowercase)) }

      // L ← Empty list that will contain the sorted nodes
      val L = ListBuffer[Declaration]()

      val marked = HashSet[Declaration]()
      val temporary = HashSet[Declaration]()

      // function visit(node n)
      def visit(n : Declaration) {
        // if n has a temporary mark then stop (not a DAG)
        if (temporary.contains(n))
          throw ParseException(s"The type hierarchy contains a cicle involving type ${n.name}.\n See ${n.declaredIn}")

        //    if n is not marked (i.e. has not been visited yet) then
        if (marked(n)) {
          return
        }

        //        mark n temporarily
        temporary += n

        edges(n).foreach(visit)
        //        for each node m with an edge from n to m do
        //            visit(m)

        //        mark n permanently
        marked += n
        //        unmark n temporarily
        temporary -= n
        //        add n to head of L
        L.prepend(n)
      }

      //  while there are unmarked nodes do
      //    select an unmarked node n
      //    visit(n)
      // @note we do not use the unmarked set, because we known which nodes are roots of the DAG and the resulting order
      // has to be created in that way
      nodes.filter { p ⇒ p == baseType(p) }.toSeq.sortWith(_.name.lowercase > _.name.lowercase).foreach(visit)

      L
    }

    // type order initialization of types
    def mkType(t : Type) : ir.Type = t match {
      case t : ConstantLengthArrayType ⇒ ir.ConstantLengthArrayType.make(tc, mkType(t.baseType), t.length)
      case t : ArrayType               ⇒ ir.VariableLengthArrayType.make(tc, mkType(t.baseType))
      case t : ListType                ⇒ ir.ListType.make(tc, mkType(t.baseType))
      case t : SetType                 ⇒ ir.SetType.make(tc, mkType(t.baseType))
      case t : MapType                 ⇒ ir.MapType.make(tc, t.baseTypes.map { mkType(_) })

      // base types are something special, because they have already been created
      case t : BaseType                ⇒ tc.get(t.name.lowercase)
    }

    // turns all AST fields of a Declaration into ir.Fields
    def mkFields(d : Declaration, fields : List[Field]) : List[ir.Field] = try {
      // turn an AST field into an ir.Field
      def mkField(node : Field) : ir.Field = try {
        node match {
          case f : Data ⇒ new ir.Field(mkType(f.t), f.name.ir, f.isAuto,
            f.description.comment, f.description.restrictions, f.description.hints)
          case f : Constant ⇒ new ir.Field(mkType(f.t), f.name.ir, f.value,
            f.description.comment, f.description.restrictions, f.description.hints)
          case f : View ⇒
            val declaredIn : Name = f.declaredInType.getOrElse {
              // we have no explicit type so we have to do a lookup
              def find(d : ir.Declaration) : Option[ir.Declaration] = d match {
                case t : ir.UserType if t.getFields().exists(_.getSkillName == f.oldName.lowercase) ⇒ Some(d)
                case t : ir.UserType ⇒ (for (s ← t.getAllSuperTypes().map(find); if s.isDefined) yield s).headOption.flatten

                case t : ir.InterfaceType if t.getFields().exists(_.getSkillName == f.oldName.lowercase) ⇒ Some(d)
                case t : ir.InterfaceType ⇒ (for (s ← t.getAllSuperTypes().map(find); if s.isDefined) yield s).headOption.flatten

                case _ ⇒ None
              }
              (d match {
                case t : UserType ⇒
                  (for (s ← t.superTypes.map(definitionNames).map(toIR); f ← find(s)) yield f.getName).headOption
                case t : InterfaceDefinition ⇒
                  (for (s ← t.superTypes.map(definitionNames).map(toIR); f ← find(s)) yield f.getName).headOption
                case _ ⇒ ???
              }).map(new Name(_)).getOrElse(
                throw ParseException(
                  s"None of the super types of ${d.name} contains a field ${f.name} that can be used in a view.")
              )
            };
            val fs = toIR(definitionNames(declaredIn)).asInstanceOf[ir.WithFields].getFields().toList
            val target = fs.find(_.getName() == f.target.name.ir).getOrElse(
              throw ParseException(
                s"""The view of ${d.name} can not refer to a field ${declaredIn.CapitalCase}.${f.name}, because it seems not to exist:
 ${fs.map(_.getName).mkString(", ")}"""
              )
            );

            new ir.View(declaredIn.ir, target, mkType(f.t), f.name.ir, f.description.comment)
        }
      } catch {
        case e : ir.ParseException ⇒ ParseException(s"${node.name}: ${e.getMessage()}")
      }

      // sort fields before views to ensure that viewed field is already transformed
      // @note that this will also sort fields in alphabetical order; this is nice, because it stabilizes API over changes
      val fs = fields.sortWith {
        case (f : View, g : View) ⇒ f.name < g.name
        case (f : View, _)        ⇒ true
        case (_, f : View)        ⇒ false
        case (f, g)               ⇒ f.name < g.name
      }
      for (f ← fs)
        yield mkField(f)
    } catch { case e : ir.ParseException ⇒ ParseException(s"In ${d.name}.${e.getMessage}", e) }

    // initialize the arguments ir companion
    def initialize(d : Declaration) {
      d match {
        case definition : UserType ⇒ toIR(definition).asInstanceOf[ir.UserType].initialize(
          parent.get(definition).map(toIR(_).asInstanceOf[ir.UserType]).getOrElse(null),
          superInterfaces(definition).map(toIR(_).asInstanceOf[ir.InterfaceType]).to,
          mkFields(d, definition.body)
        )

        case definition : InterfaceDefinition ⇒ toIR(definition).asInstanceOf[ir.InterfaceType].initialize(
          parent.get(definition).map(toIR(_).asInstanceOf[ir.UserType]).getOrElse(tc.get("annotation")),
          superInterfaces(definition).map(toIR(_).asInstanceOf[ir.InterfaceType]).to,
          mkFields(d, definition.body)
        )

        case definition : EnumDefinition ⇒ toIR(definition).asInstanceOf[ir.EnumType].initialize(
          mkFields(d, definition.body)
        )

        case definition : Typedef ⇒ toIR(definition).asInstanceOf[ir.Typedef].initialize(
          mkType(definition.target)
        )
      }
    }
    val ordered = topologicalSort(defs)

    for (t ← ordered) try {
      initialize(t)
    } catch { case e : Exception ⇒ throw ParseException(s"Initialization of type ${t.name} failed.\nSee ${t.declaredIn}", e) }

    val rval = ordered.map(toIR(_))

    // we initialized in type order starting at base types; if some types have not been initialized, then they are cyclic!
    if (rval.exists(!_.isInitialized))
      ParseException("there are uninitialized definitions including:\n "+rval.filter(!_.isInitialized).map(_.prettyPrint).mkString("\n "))

    assume(defs.size == rval.size, "we lost some definitions")
    assume(rval.forall { _.isInitialized }, s"we missed some initializations: ${rval.filter(!_.isInitialized).mkString(", ")}")

    if (verboseOutput) {
      println(s"types: ${ordered.size}")
      @inline def fieldCount(c : Int, d : Declaration) : Int = d match {
        case d : UserType            ⇒ c + d.body.size
        case d : InterfaceDefinition ⇒ c + d.body.size
        case _                       ⇒ c
      }
      println(s"fields: ${ordered.foldLeft(0)(fieldCount)}")
    }

    tc.setDefs(ordered.map(toIR).to)

    // for now, sanity check typedefs by creating a typedef projection
    try {
      tc.removeTypedefs
    } catch {
      case e : ir.ParseException ⇒
        throw new ir.ParseException("Failed to project away typedefs, see argument exception for a reason", e)
    }

    tc
  }
}
object Parser {
  import Parser._

  /**
   * @return a type context containing all type information obtained from the argument file
   */
  def process(input : File, delimitWithUnderscore : Boolean = true, delimitWithCamelCase : Boolean = true, verboseOutput : Boolean = false) : TypeContext = {
    val p = new Parser(delimitWithUnderscore, delimitWithCamelCase)
    p.buildIR(p.parseAll(input).to)
  }

}
