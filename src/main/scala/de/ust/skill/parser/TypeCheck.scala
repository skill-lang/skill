package de.ust.skill.parser

import scala.collection.mutable.ArrayBuffer
import scala.annotation.tailrec
import scala.collection.mutable.HashMap

object TypeCheck {

  def apply(defs : ArrayBuffer[Declaration]) = {
    // split types by kind
    val userTypes = defs.collect { case t : UserType ⇒ t }
    val enums = defs.collect { case t : EnumDefinition ⇒ t }
    val interfaces = defs.collect { case t : InterfaceDefinition ⇒ t }
    val typedefs = defs.collect { case t : Typedef ⇒ t }

    // TODO what about typedefs???
    // TODO typedefs have to be eliminated here, at least for the purpose of checking the hierarchy
    // TODO the implementation assumes that there is no typedef in the middle of a type hierarchy!

    // create declarations
    // skillname ⇀ subtypes
    // may contain user types and interfaces
    var subtypes = new HashMap[Name, List[Declaration]];

    // the direct super type that is a user type and not an interface
    // in fact they type may not exist for interfaces, in that case an annotation will be used for representation
    var parent = new HashMap[Declaration, UserType];

    // skillname ⇀ definition
    val definitionNames = new HashMap[Name, Declaration];
    for (d ← defs) definitionNames.put(d.name, d)

    if (defs.size != definitionNames.size) {
      val duplicates = defs.groupBy(l ⇒ l.name).collect {
        case l if l._2.size > 1 ⇒ (l._1, l._2.map {
          d ⇒ s"${d.name}@${d.name.pos} in ${d.declaredIn.getName}"
        }.mkString("\n    ", "\n    ", ""))
      }
      ParseException(s"I got ${defs.size - definitionNames.size} duplicate definition${
        if (1 == defs.size - definitionNames.size) ""
        else"s"
      }.\n${duplicates.mkString("\n")}")
    }

    // build sub-type relation
    for (d ← userTypes; parent ← d.superTypes) {
      val p = definitionNames.get(parent).getOrElse(
        ParseException(s"""The type "${parent}" parent of ${d.name} is unknown!
Did you forget to include ${parent}.skill?
Known types are: ${definitionNames.keySet.mkString(", ")}""")
      );

      if (!subtypes.contains(parent)) {
        subtypes.put(parent, List[Declaration]())
      }
      subtypes(parent) ++= List[Declaration](d)
    }
    for (d ← interfaces; parent ← d.superTypes) {
      val p = definitionNames.get(parent).getOrElse(
        ParseException(s"""The type "${parent}" parent of ${d.name} is unknown!
Did you forget to include ${parent}.skill?
Known types are: ${definitionNames.keySet.mkString(", ")}""")
      );

      if (!subtypes.contains(parent)) {
        subtypes.put(parent, List[Declaration]())
      }
      subtypes(parent) ++= List[Declaration](d)
    }

    // build and check parent relation
    // TODO should also work with multiple steps of interface inheritance
    for (
      (pn, subs) ← subtypes;
      p ← definitionNames.get(pn);
      if p.isInstanceOf[UserType];
      s ← subs
    ) {
      if (parent.contains(s))
        throw ParseException(s"Type ${s.name} has at least two regular super types: ${parent(s).name} and ${p.name}")
      parent(s) = p.asInstanceOf[UserType]
    }

    // build base type relation
    val baseType = HashMap[Declaration, Declaration]()
    for (d ← defs) {
      @tailrec
      def base(d : Declaration) : Declaration = parent.get(d) match {
        case Some(p) ⇒ base(p)
        case None    ⇒ d
      }
      baseType(d) = base(d)
    }

    // build and check super interface relation
    val superInterfaces = HashMap[Declaration, List[InterfaceDefinition]]()
    for (d ← userTypes) {
      val is = d.superTypes.map(definitionNames(_)).collect { case d : InterfaceDefinition ⇒ d }
      superInterfaces(d) = is
      if (d.superTypes.size != is.size + parent.get(d).size)
        throw ParseException(s"Type ${d.name} inherits something thats neither a user type nor an interface.")
    }
    for (d ← interfaces) {
      val is = d.superTypes.map(definitionNames(_)).collect { case d : InterfaceDefinition ⇒ d }
      superInterfaces(d) = is
      if (d.superTypes.size != is.size + parent.get(d).size)
        throw ParseException(s"Type ${d.name} inherits something thats neither a user type nor an interface.")
    }

    (defs, baseType, parent, superInterfaces)
  }

}