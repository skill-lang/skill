/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import scala.collection.mutable.ArrayBuffer
import scala.annotation.tailrec
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

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
Declaration in ${d.declaredIn}.
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
    // step 1: invert sub-type relation
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
    // step 2: closure over super-interface relation
    var typesVisited = Set[Declaration]()
    def moreSpecific(l : UserType, r : UserType) : UserType = {
      // find the more specific type

      // is l a super type of r? 
      var t = parent(r)
      while (null != t) {
        if (t == l)
          return r
        t = parent.get(t).getOrElse(null)
      }

      // is r a super type of l? 
      t = parent(l)
      while (null != t) {
        if (t == r)
          return l
        t = parent.get(t).getOrElse(null)
      }

      throw new Exception();
    }
    def recursiveSuperType(d : Declaration) : UserType = {
      typesVisited += d
      var r = parent.get(d).getOrElse(null)
      d match {
        case i : UserType ⇒
          for (
            s ← i.superTypes if s != "annotation"
              && definitionNames(s).isInstanceOf[InterfaceDefinition]
              && !typesVisited(definitionNames(s)
              )
          ) {
            var t = recursiveSuperType(definitionNames(s))
            if (null != r && null != t && t != r) try {
              r = moreSpecific(t, r)
            } catch {
              case e : Exception ⇒
                throw ParseException(s"Type ${d.name} has at least two regular super types: ${r.name} and ${t.name}")
            }
            else if (null == r && t != null)
              r = t
          }
        case i : InterfaceDefinition ⇒
          for (
            s ← i.superTypes if s != "annotation"
              && definitionNames(s).isInstanceOf[InterfaceDefinition]
              && !typesVisited(definitionNames(s))
          ) {
            var t = recursiveSuperType(definitionNames(s))
            if (null != r && null != t && t != r) try {
              r = moreSpecific(t, r)
            } catch {
              case e : Exception ⇒
                throw ParseException(s"Type ${d.name} has at least two regular super types: ${r.name} and ${t.name}")
            }
            else if (null == r && t != null)
              r = t
          }

        case _ ⇒ ??? // can not happen?
      }
      r
    }
    for (t ← userTypes) {
      // reset visited types
      typesVisited = Set[Declaration]()

      // visit types
      val r = recursiveSuperType(t)

      if (parent.contains(t) && r != parent(t))
        throw new Error(s"$r!=${parent(t)}");

      // set direct super type, if any
      if (null != r)
        parent(t) = r
    }

    // build base type relation
    val baseType = HashMap[Declaration, Declaration]()
    for (d ← defs) {
      // collect seen types for cycle detection
      val seen = HashSet[Declaration]()

      @tailrec
      def base(d : Declaration) : Declaration = {
        if (seen.contains(d))
          throw ParseException(s"The super type relation contains a cycle involving regular type ${d.name.CapitalCase}")
        seen += d
        parent.get(d) match {
          case Some(p) ⇒ base(p)
          case None    ⇒ d
        }
      }
      baseType(d) = base(d)
    }

    // build and check super interface relation
    val superInterfaces = HashMap[Declaration, List[InterfaceDefinition]]()
    for (d ← userTypes) {
      val is = d.superTypes.map(definitionNames(_)).collect { case d : InterfaceDefinition ⇒ d }
      superInterfaces(d) = is
      // this check is so complicated, because the super type can be inherited implicitly
      if (d.superTypes.size != is.size + parent.get(d).map {
        x ⇒ if (d.superTypes.contains(x.name)) 1 else 0
      }.getOrElse(0))
        throw ParseException(s"Type ${d.name} inherits something thats neither a user type nor an interface.")
    }
    for (d ← interfaces) {
      val is = d.superTypes.map(definitionNames(_)).collect { case d : InterfaceDefinition ⇒ d }
      superInterfaces(d) = is
      // this check is so complicated, because the super type can be inherited implicitly
      if (d.superTypes.size != is.size + parent.get(d).map {
        x ⇒ if (d.superTypes.contains(x.name)) 1 else 0
      }.getOrElse(0))
        throw ParseException(s"Type ${d.name} inherits something thats neither a user type nor an interface.")
    }

    for (t ← defs.collect { case c : DeclarationWithBody ⇒ c }.par) {
      // check duplicate abstract field names?
      val (custom : List[Customization], regular) = t.body.partition(_.isInstanceOf[Customization])
      if (regular.size != regular.map(_.name).toSet.size)
        throw ParseException(s"Type ${t.name} uses the same name for multiple field like declarations.")

      if (custom.map { x ⇒ (x.name, x.language) }.toSet.size != custom.size)
        throw ParseException(s"Type ${t.name} uses the same name for multiple custom field declarations in the same language.")

      // check views
      for (v ← t.body.collect { case v : View ⇒ v }) {
        // ensure target type
        val targetDef = try {
          definitionNames.get(v.targetType.getOrElse {
            def find(decl : Declaration) : Name = decl match {
              case d : UserType ⇒
                if (d.body.exists(f ⇒ f != v && f.name == v.targetField)) return d.name
                else for (
                  s ← d.superTypes; r = find(definitionNames(s))
                ) if (null != r) return r;
                return null;
              case d : InterfaceDefinition ⇒
                if (d.body.exists(f ⇒ f != v && f.name == v.targetField)) return d.name
                else for (
                  s ← d.superTypes; r = find(definitionNames(s))
                ) if (null != r) return r;
                return null;
              case d : EnumDefinition if (d.body.exists(_.name == v.targetField)) ⇒ return d.name
              case d : Typedef ⇒ return find(definitionNames(d.target.asInstanceOf[BaseType].name))
              case _ ⇒ return null
            }
            val r = find(t)
            if (null == r) { throw ParseException(s"View ${t.name}.${v.name} could not find source type.") }
            v.targetType = Some(r)
            r
          }).getOrElse(
            throw ParseException(s"View ${t.name}.${v.name} refers to unknown source type ${v.targetType.get}.")
          ).asInstanceOf[DeclarationWithBody]
        } catch {
          case e : ClassCastException ⇒
            throw ParseException(s"View ${t.name}.${v.name} cannot use source type ${v.targetType.get}.")
        }

        // check that target type contains the argument field
        val targetField = targetDef.body.filter(_.name == v.targetField).headOption.getOrElse(
          throw ParseException(s"View ${t.name}.${v.name} cannot use unknown source field ${v.targetType.get}.${v.targetField}.")
        )

        // get the views super type
        val superType = targetField match {
          case f : Field ⇒ f.t
          case v : View  ⇒ v.t
          case _         ⇒ throw ParseException(s"View ${t.name}.${v.name} cannot use invalid view target ${v.targetType.get}.${v.targetField}.")
        }

        def subtype(decl : Declaration, superDecl : Declaration) : Boolean = {
          if (decl == superDecl) true
          else decl match {
            case d : UserType                  ⇒ d.superTypes.exists { x ⇒ subtype(definitionNames(x), superDecl) }
            case d : InterfaceDefinition       ⇒ d.superTypes.exists { x ⇒ subtype(definitionNames(x), superDecl) }
            case Typedef(_, _, _, BaseType(x)) ⇒ subtype(definitionNames(x), superDecl)
            case _                             ⇒ false
          }
        }
        // check that target type is in fact a super type
        if (!subtype(t, targetDef))
          throw ParseException(s"View ${t.name}.${v.name} cannot view a field of type $targetDef. Only super types can be used.")

        // check type compatibility
        if (v.t != superType) {
          val decl = (v.t match {
            case BaseType(n) ⇒ definitionNames.get(n)
            case _           ⇒ None
          }).getOrElse {
            throw ParseException(s"View ${t.name}.${v.name} only user types can be retyped.")
          }
          val superDecl = (superType match {
            case BaseType(n) ⇒ definitionNames.get(n)
            case _           ⇒ None
          }).getOrElse {
            throw ParseException(s"View ${t.name}.${v.name} only user types can be retyped.")
          }

          if (!subtype(decl, superDecl))
            throw ParseException(s"View ${t.name}.${v.name} illformed retype $decl is no subtype of $superDecl.")
        }
      }
    }

    (baseType, parent, superInterfaces)
  }

}
