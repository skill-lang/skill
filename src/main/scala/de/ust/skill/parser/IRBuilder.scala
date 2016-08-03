package de.ust.skill.parser;

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer

import de.ust.skill.ir
import de.ust.skill.ir.TypeContext

object IRBuilder {
  /**
   * Turns the AST into IR.
   *
   * TODO the type checking should be separated and IR building should start over with the original AST and the
   * knowledge, that it is in fact correct
   */
  def buildIR(defs : ArrayBuffer[Declaration], verboseOutput : Boolean) : TypeContext = {
    val tc = new ir.TypeContext

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
        }
      } catch {
        case e : ir.ParseException ⇒ ParseException(s"${node.name}: ${e.getMessage()}")
      }

      // sort fields in alphabetical order, because it stabilizes API over changes
      val fs = fields.sortWith {
        case (f, g) ⇒ f.name < g.name
      }
      for (f ← fs)
        yield mkField(f)
    } catch { case e : ir.ParseException ⇒ ParseException(s"In ${d.name}.${e.getMessage}", e) }

    // turns AST language customizations to IR
    def mkCustomFields(fields : List[Customization]) : List[ir.LanguageCustomization] = for (f ← fields) yield {
      var options = new java.util.HashMap[String, java.util.List[String]]()
      for ((k, v) ← f.options)
        options.put(k.lowercase, v.to)
      new ir.LanguageCustomization(f.name.ir, f.comment, f.language.ir, f.typeImage, options)
    }

    // turns AST view to IR
    def mkFieldViews(d : Declaration, fields : List[View]) : List[ir.View] = for (f ← fields) yield {
      new ir.View(
        d.name.ir,
        mkType(f.t),
        f.name.ir,
        f.comment
      )
    }

    // initialize the arguments ir companion
    def initialize(d : Declaration) {
      d match {
        case definition : UserType ⇒ toIR(definition).asInstanceOf[ir.UserType].initialize(
          parent.get(definition).map(toIR(_).asInstanceOf[ir.UserType]).getOrElse(null),
          superInterfaces(definition).map(toIR(_).asInstanceOf[ir.InterfaceType]).to,
          mkFields(d, definition.body.collect { case f : Field ⇒ f }),
          mkFieldViews(d, definition.body.collect { case f : View ⇒ f }),
          mkCustomFields(definition.body.collect { case f : Customization ⇒ f })
        )

        case definition : InterfaceDefinition ⇒ toIR(definition).asInstanceOf[ir.InterfaceType].initialize(
          parent.get(definition).map(toIR(_).asInstanceOf[ir.UserType]).getOrElse(tc.get("annotation")),
          superInterfaces(definition).map(toIR(_).asInstanceOf[ir.InterfaceType]).to,
          mkFields(d, definition.body.collect { case f : Field ⇒ f }),
          mkFieldViews(d, definition.body.collect { case f : View ⇒ f }),
          mkCustomFields(definition.body.collect { case f : Customization ⇒ f })
        )

        case definition : EnumDefinition ⇒ toIR(definition).asInstanceOf[ir.EnumType].initialize(
          mkFields(d, definition.body.collect { case f : Field ⇒ f }),
          mkFieldViews(d, definition.body.collect { case f : View ⇒ f }),
          mkCustomFields(definition.body.collect { case f : Customization ⇒ f })
        )

        case definition : Typedef ⇒ toIR(definition).asInstanceOf[ir.Typedef].initialize(
          mkType(definition.target)
        )
      }
    }

    // initialize the views of the argument declaration
    def initializeViews(d : Declaration) {
      d match {
        case definition : DeclarationWithBody ⇒ definition.body.collect {
          case v : View ⇒
            val targetName = v.targetField.lowercase
            val targetType = toIRByName(v.targetType.getOrElse(definition.name)).asInstanceOf[ir.WithFields]

            val target = targetType.getFields.find(_.getSkillName.equals(targetName)).getOrElse(
              targetType.getViews.find(_.getSkillName.equals(targetName)).getOrElse(
                throw new ir.ParseException(s"$v has no valid target")
              ))

            toIR(definition).asInstanceOf[ir.WithFields].getViews.find {
              case f ⇒ f.getName == v.name.ir
            }.get.initialize(target)
        }

        case definition : Typedef ⇒ // no action required
      }
    }
    val ordered = topologicalSort(defs)

    // initialize types
    for (t ← ordered) try {
      initialize(t)
    } catch { case e : Exception ⇒ throw ParseException(s"Initialization of type ${t.name} failed.\nSee ${t.declaredIn}", e) }

    // initialize views (requires second pass, as they can refer to other views)
    for (t ← ordered)
      initializeViews(t)

    val rval = ordered.map(toIR(_))

    // we initialized in type order starting at base types; if some types have not been initialized, then they are cyclic!
    if (rval.exists(!_.isInitialized))
      ParseException("there are uninitialized definitions including:\n " + rval.filter(!_.isInitialized).map(_.prettyPrint).mkString("\n "))

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