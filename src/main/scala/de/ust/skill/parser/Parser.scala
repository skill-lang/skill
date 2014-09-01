package de.ust.skill.parser

import java.io.File
import java.io.FileNotFoundException
import java.lang.Long
import java.nio.file.FileSystems
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.LinkedList
import scala.util.parsing.combinator.RegexParsers
import de.ust.skill.ir
import de.ust.skill.ir.Hint
import de.ust.skill.ir.Restriction
import de.ust.skill.ir.restriction.ConstantLengthPointerRestriction
import de.ust.skill.ir.restriction.FloatRangeRestriction
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.MonotoneRestriction
import de.ust.skill.ir.restriction.NullableRestriction
import de.ust.skill.ir.restriction.SingletonRestriction
import de.ust.skill.ir.restriction.UniqueRestriction
import scala.collection.mutable.ArrayBuffer

/**
 * The Parser does everything required for turning a set of files into a list of definitions.
 * @see #process
 * @see SKilL V1.0 Appendix A
 * @author Timm Felden
 */
final class Parser {
  val tc = new ir.TypeContext

  /**
   * Converts a character stream into an AST using parser combinators.
   *
   * Grammar as explained in the paper.
   */
  final class FileParser extends RegexParsers {
    /**
     * Usual identifiers including arbitrary unicode characters.
     */
    private def id = """[a-zA-Z_\u007f-\uffff][\w\u007f-\uffff]*""".r
    /**
     * Skill integer literals
     */
    private def int:Parser[Long] = HexInt | GeneralInt
    private def HexInt:Parser[Long] = "0x" ~> ("""[0-9a-fA-F]*""".r ^^ { i ⇒ Long.parseLong(i, 16) })
    private def GeneralInt:Parser[Long] = """[0-9]*\.*""".r >> { i ⇒
              try{
                success(Long.parseLong(i, 10))
              }catch{
                case e:Exception ⇒ failure("not an int")
              }
            }

    /**
     * Floating point literal, as taken from the JavaTokenParsers definition.
     *
     * @note if the target can be an integer as well, the integer check has to come first
     */
    def floatingPointNumber: Parser[Double] = """-?(\d+(\.\d*)?|\d*\.\d+)([eE][+-]?\d+)?[fFdD]?""".r ^^ {_.toDouble}

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
    private def headComment = rep("""#[^\n]*\n""".r)


    /**
     * Includes are just strings containing relative paths to *our* path.
     */
    private def includes = ("include" | "with") ~> rep(string);

    /**
     * Declarations add or modify user defined types.
     */
    private def declaration:Parser[Declaration] = namespace | typedef | enumType | interfaceType | fieldChange | userType

    /**
     * Creates a new name space.
     */
    private def namespace = typeDescription ~ ("namespace" ~> id) ~ ("{" ~> declaration <~ "}") ^^ {
      case desc ~ name ~ decl ⇒
       ???
    }

    /**
     * creates a shorthand for a more complex type
     */
    private def typedef = opt(comment) ~ ("typedef" ~> id) ~ rep(fieldRestriction|hint) ~ fieldType <~ ";" ^^ {
      case c ~ name ~ desc ~ target ⇒ null
    };

    /**
     * A declaration may start with a description, is followed by modifiers and a name, might have a super class and has
     * a body.
     */
    private def userType = opt(changeModifier) ~ typeDescription ~ id ~ opt(rep((":" | "with" | "extends") ~> id)) ~!
      ("{" ~> rep(field) <~ "}") ^^ {
        case c ~ d ~ n ~ s ~ b ⇒ Definition(c, d, n, s.flatMap(_.headOption), s.map(_.drop(1)).getOrElse(List()), b)
    }

    /**
     * modifier prefix to type declarations if they are meant to be modified
     */
    private def changeModifier = (
      "++"^^{_⇒ChangeModifier.++}
    | "--"^^{_⇒ChangeModifier.--}
    | "=="^^{_⇒ChangeModifier.set}
    )

    /**
     * creates an enum definition
     */
    private def enumType = opt(comment) ~ ("enum" ~> id) ~ ("{" ~> repsep(id, ",") <~ ";") ~ (rep(field) <~ "}") ^^ {
      case c ~ n ~ i ~ f ⇒ new EnumDefinition(c, n, i, f)
    }

    /**
     * creates an interface definition
     */
    private def interfaceType = opt(comment) ~ ("interface" ~> id) ~ rep((":" | "with" | "extends") ~> id) ~ ("{" ~> rep(field) <~ "}") ^^ {
      case c ~ n ~ i ~ f ⇒ new InterfaceDefinition(c, n, i, f)
    }

    /**
     * modify the definition of a field
     */
    private def fieldChange = changeModifier ~ fieldDescription ~ opt("auto") ~ fieldType ~ id ~ ("." ~> id <~";") ^^ {
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
      case "map" ~ l  ⇒ new de.ust.skill.parser.MapType(l)
      case "set" ~ l  ⇒ { assert(1 == l.size); new de.ust.skill.parser.SetType(l.head) }
      case "list" ~ l ⇒ { assert(1 == l.size); new de.ust.skill.parser.ListType(l.head) }
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
     * 
     * TODO
     */
    private def comment = """/\*([^\*/]|/|\*+[^\*/])*\*+/""".r ^^ { s ⇒ new Comment(List(s.substring(2, s.size-2)),
        List())
    }

    /**
     * restrictions as defined in the paper.
     * 
     * @note the implementation is more liberal then the specification of the specification language, because some illegal arguments are dropped
     */
    private def typeRestriction:Parser[Restriction] = "@" ~> id >> { _.toLowerCase match {
      case "unique" ⇒ opt("(" ~ ")") ^^{_ ⇒ new UniqueRestriction}

      case "singleton" ⇒ opt("(" ~ ")") ^^{_ ⇒ new SingletonRestriction}

      case "monotone" ⇒ opt("(" ~ ")") ^^{_ ⇒ new MonotoneRestriction}

      case "abstract" ⇒ opt("(" ~ ")") ^^{_ ⇒ null}

      case "default" ⇒ "(" ~> defaultRestrictionParameter <~ ")" ^^{_ ⇒ null}

      case unknown ⇒ opt("(" ~> repsep((int | string | floatingPointNumber), ",") <~ ")") ^^ {arg ⇒
        ParseException(s"$unknown${
          arg.mkString("(", ", ", ")")
        } is either not supported or an invalid restriction name")
      }
    }}
    private def fieldRestriction:Parser[Restriction] = "@" ~> id >> { _.toLowerCase match {

      case "nonnull" ⇒ opt("(" ~ ")") ^^{_ ⇒ new NullableRestriction}

      case "default" ⇒ "(" ~> defaultRestrictionParameter <~ ")" ^^{_ ⇒ null}

      case "min" ⇒ "(" ~> (
          int ~ opt("," ~> string) ^^ {
            case low ~ None ⇒ new IntRangeRestriction(low, Long.MAX_VALUE, true, true)
            case low ~ Some("inclusive") ⇒ new IntRangeRestriction(low, Long.MAX_VALUE, true, true)
            case low ~ Some("exclusive") ⇒ new IntRangeRestriction(low, Long.MAX_VALUE, false, true)
            }
          |
          floatingPointNumber ~ opt("," ~> string) ^^ {
            case low ~ None ⇒ new FloatRangeRestriction(low, Double.MaxValue, true, true)
            case low ~ Some("inclusive") ⇒ new FloatRangeRestriction(low, Double.MaxValue, true, true)
            case low ~ Some("exclusive") ⇒ new FloatRangeRestriction(low, Double.MaxValue, false, true)
            }
          ) <~ ")"

      case "max" ⇒ "(" ~> (
          int ~ opt("," ~> string) ^^ {
            case high ~ None ⇒ new IntRangeRestriction(Long.MIN_VALUE, high, true, true)
            case high ~ Some("inclusive") ⇒ new IntRangeRestriction(Long.MIN_VALUE, high, true, true)
            case high ~ Some("exclusive") ⇒ new IntRangeRestriction(Long.MIN_VALUE, high, true, false)
            }
          |
          floatingPointNumber ~ opt("," ~> string) ^^ {
            case high ~ None ⇒ new FloatRangeRestriction(Double.MinValue, high, true, true)
            case high ~ Some("inclusive") ⇒ new FloatRangeRestriction(Double.MinValue, high, true, true)
            case high ~ Some("exclusive") ⇒ new FloatRangeRestriction(Double.MinValue, high, true, false)
            }
          ) <~ ")"

      case "range" ⇒ "(" ~> (
          int ~ ("," ~> int) ~ opt("," ~> string ~ ("," ~> string)) ^^ {
            case low ~ high ~ None ⇒ new IntRangeRestriction(low, high, true, true)
            case low ~ high ~ Some(l ~ h) ⇒ new IntRangeRestriction(low, high, "inclusive"==l, "inclusive"==h)
          }
          |
          floatingPointNumber ~ ("," ~> floatingPointNumber) ~ opt("," ~> string ~ ("," ~> string)) ^^ {
            case low ~ high ~ None ⇒ new FloatRangeRestriction(low, high, true, true)
            case low ~ high ~ Some(l ~ h) ⇒ new FloatRangeRestriction(low, high, "inclusive"==l, "inclusive"==h)
          }
          ) <~ ")"

      case "coding" ⇒ ("(" ~> string <~ ")") ^^{_ ⇒ null}

      case "constantLengthPointer" ⇒ opt("(" ~ ")") ^^{_ ⇒ new ConstantLengthPointerRestriction}

      case "oneof" ⇒ opt("(" ~ ")") ^^{_ ⇒ null}

      case unknown ⇒ opt("(" ~> repsep((int | string | floatingPointNumber), ",") <~ ")") ^^ {arg ⇒
        ParseException(s"$unknown${
          arg.mkString("(", ", ", ")")
        } is either not supported or an invalid restriction name")
      }
    }}

    private def defaultRestrictionParameter = int | string | floatingPointNumber | repsep(id, "."|"::")

    /**
     * hints as defined in the paper. Because hints can be ignored by the generator, it is safe to allow arbitrary
     * identifiers and to warn if the identifier is not a known hint.
     */
    private def hint = "!" ~> id ^^ { n ⇒ Hint.valueOf(n.toLowerCase) }

    /**
     * Description of a field.
     */
    private def fieldDescription = opt(comment) ~ rep(fieldRestriction | hint) ^^ {
      case c ~ specs ⇒ new Description(c, specs.collect{case r:Restriction⇒r}, specs.collect{case h:Hint⇒h})
    }
    /**
     * Description of a declration.
     */
    private def typeDescription = opt(comment) ~ rep(typeRestriction | hint) ^^ {
      case c ~ specs ⇒ new Description(c, specs.collect{case r:Restriction⇒r}, specs.collect{case h:Hint⇒h})
    }



    /**
     * The <b>main</b> function of the parser, which turn a string into a list of includes and declarations.
     */
    def process(in: File): (List[String], List[Declaration]) = {
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
  private def parseAll(input: File) = {
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
        } catch {
          case e: FileNotFoundException ⇒ ParseException(
            s"The include $file could not be resolved to an existing file: ${e.getMessage()} \nWD: ${
              FileSystems.getDefault().getPath(".").toAbsolutePath().toString()
            }"
          )
        }
      }
    }
    rval.collect{case i:Definition⇒i};
  }

  /**
   * Turns the AST into IR.
   */
  private def buildIR(defs: LinkedList[Definition]): java.util.List[ir.Declaration] = {
    // create declarations
    // skillname ⇀ subtypes
    var subtypes = new HashMap[String, LinkedList[Definition]]
    // skillname ⇀ definition
    val definitionNames = new HashMap[String, Definition];
    for(d <- defs) definitionNames.put(d.name.toLowerCase, d);
    if (defs.size != definitionNames.size) {
      ParseException(s"I got ${defs.size - definitionNames.size} duplicate definition${
        if(1==defs.size - definitionNames.size)""else"s"
          }.")
    }

    // build sub-type relation
    for(d <- defs if d.parent.isDefined) {
      val p = definitionNames.get(d.parent.get.toLowerCase).getOrElse(ParseException(s"""The type "${d.parent.get}" parent of ${d.name} is unknown!
Did you forget to include ${d.parent.get}.skill?
Known types are: ${definitionNames.keySet.mkString(", ")}"""))
      val parent = p.name.toLowerCase
      if (!subtypes.contains(parent)) {
        subtypes.put(parent, LinkedList[Definition]())
      }
      subtypes(parent) ++=  LinkedList[Definition](d)
    }

    // create declarations
    val rval = definitionNames.map({ case (n, f) ⇒ (f, ir.Declaration.newDeclaration(
        tc,
        f.name,
        f.description.comment.map(_.text.head).getOrElse(""),
        f.description.restrictions,
        f.description.hints
        )) })

    // type order initialization of types
    def mkType(t: Type): ir.Type = t match {
      case t: ConstantLengthArrayType ⇒ ir.ConstantLengthArrayType.make(tc, mkType(t.baseType), t.length)
      case t: ArrayType               ⇒ ir.VariableLengthArrayType.make(tc, mkType(t.baseType))
      case t: ListType                ⇒ ir.ListType.make(tc, mkType(t.baseType))
      case t: SetType                 ⇒ ir.SetType.make(tc, mkType(t.baseType))
      case t: MapType                 ⇒ ir.MapType.make(tc, t.baseTypes.map { mkType(_) })

      // base types are something special, because they have already been created
      case t: BaseType                ⇒ tc.get(t.name.toLowerCase)
    }
    def mkField(node: Field): ir.Field = try {
      node match {
        case f: Data     ⇒ new ir.Field(mkType(f.t), f.name, f.isAuto, 
            f.description.comment.map(_.text.head).getOrElse(""), f.description.restrictions, f.description.hints)
        case f: Constant ⇒ new ir.Field(mkType(f.t), f.name, f.value,
            f.description.comment.map(_.text.head).getOrElse(""), f.description.restrictions, f.description.hints)
      }
    } catch {
      case e: ir.ParseException ⇒ ParseException(s"${node.name}: ${e.getMessage()}")
    }
    def initialize(name: String) {
      val definition = definitionNames(name)
      val superDecl = if(definition.parent.isEmpty) null
      else rval(definitionNames(definition.parent.get.toLowerCase))
      rval(definition).initialize(
        superDecl,
        try { definition.body.map(mkField(_)) } catch {
          case e: ir.ParseException ⇒ ParseException(s"In $name.${e.getMessage}")
        }
      )
      //initialize children
      subtypes.getOrElse(name.toLowerCase, LinkedList()).foreach { d ⇒ initialize(d.name.toLowerCase) }
    }
    definitionNames.values.filter(_.parent.isEmpty).foreach { d ⇒ initialize(d.name.toLowerCase) }

    // we initialized in type order starting at base types; if some types have not been initialized, then they are cyclic!
    if(rval.values.exists(!_.isInitialized))
      ParseException("there are cyclic type definitions including: " + rval.values.filter(!_.isInitialized).mkString(", "))

    assume(defs.size == rval.values.size, "we lost some definitions")
    assume(rval.values.forall{_.isInitialized}, s"we missed some initializations: ${rval.values.filter(!_.isInitialized).mkString(", ")}")

    // create type ordered sequence
    def getInTypeOrder(d:ir.Declaration):Seq[ir.Declaration] = if(subtypes.contains(d.getSkillName)){
      (for(sub <- subtypes(d.getSkillName))
        yield getInTypeOrder(rval(sub))).foldLeft(Seq(d))(_ ++ _)
    }else{ 
      Seq(d)
    }

    (for(d <- rval.values if null == d.getSuperType)
      yield getInTypeOrder(d)).toSeq.foldLeft(Seq[ir.Declaration]())(_ ++ _)
  }
}

object Parser {
  import Parser._

  /**
   * returns an unsorted list of declarations
   */
  def process(input: File): java.util.List[ir.Declaration] = {
    val p = new Parser
    p.buildIR(p.parseAll(input).to)
  }

}
