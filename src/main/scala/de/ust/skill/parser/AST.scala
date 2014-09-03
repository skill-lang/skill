/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import de.ust.skill.ir.Restriction
import de.ust.skill.ir.Hint
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
 * The AST is used to turn skill definitions into Java IR.
 *
 * @author Timm Felden
 */
sealed abstract class Node;
sealed abstract class Declaration(val name : Name) extends Node;

final class Description(val comment : Option[Comment], val restrictions : List[Restriction],
                        val hints : List[Hint]) extends Node;

final class Comment(val text : List[String], val tags : List[CommentTag]) extends Node;
final class CommentTag(val name : String, val text : List[String]) extends Node;

sealed abstract class Type extends Node;

final class MapType(val baseTypes : List[BaseType]) extends Type;

final class SetType(val baseType : BaseType) extends Type;

final class ListType(val baseType : BaseType) extends Type;

sealed class ArrayType(val baseType : BaseType) extends Type;
final class ConstantLengthArrayType(baseType : BaseType, val length : Long) extends ArrayType(baseType);

final class BaseType(val name : String) extends Type {
  override def toString : String = name
}

sealed abstract class Field(val t : Type, val name : String) extends Node {
  var description : Description = new Description(None, List[Restriction](), List[Hint]());
}

final class Constant(t : Type, name : String, val value : Long) extends Field(t, name);

final class Data(val isAuto : Boolean, t : Type, name : String) extends Field(t, name);

final class View(val declaredInType : Option[String], val oldName : String, val target : Field) extends Field(target.t, target.name);

/**
 * Representation of skill names.
 *
 * TODO propper treatment of parts
 */
final class Name(source : String, delimitWithUnderscores : Boolean, delimitWithCamelCase : Boolean) {
  // @note this may not be correct if more then two _ are used
  val parts : List[String] = {
    var rval = ArrayBuffer(source)

    if (delimitWithUnderscores)
      rval = rval.flatMap(_.split("_").map { s ⇒ if (s.isEmpty()) "_" else s }.to)

    if (delimitWithCamelCase)
      rval = rval.flatMap { s ⇒
        var parts = ArrayBuffer[String]()
        var last = 0
        for (i ← 1 until s.length - 1) {
          if (s.charAt(i).isUpper && s.charAt(i + 1).isLower) {
            parts += s.substring(last, i)
            last = i
          }
        }
        parts += s.substring(last)
        parts
      }

    rval.to
  }

  lazy val camelCase : String = parts.tail.foldLeft(parts.head)(_ + _.capitalize)

  val lowercase : String = parts.foldLeft("")(_ + _.toLowerCase)

  lazy val CapitalCase : String = parts.tail.foldLeft(parts.head.capitalize)(_ + _.capitalize)

  lazy val ADA_STYLE : String = parts.tail.foldLeft(parts.head.toUpperCase)(_+"_"+_.toUpperCase)

  def ir = new de.ust.skill.ir.Name(parts, lowercase);

  override def equals(o : Any) = o match {
    case o : Name ⇒ o.lowercase == lowercase
    case _        ⇒ false
  }

  override def hashCode = lowercase.hashCode

  override def toString = CapitalCase
}

object ChangeModifier extends Enumeration {
  type ChangeModifier = Value
  val ++, --, set = Value
}

final case class UserType(
    change : Option[ChangeModifier.ChangeModifier],
    description : Description,
    _name : Name, superTypes : List[Name], body : List[Field]) extends Declaration(_name) {

  override def equals(other : Any) = other match {
    case UserType(Some(ChangeModifier.set), _, n, _, _) if change.isDefined ⇒
      change.get == ChangeModifier.set && n == name

    case UserType(None, _, n, _, _) if !change.isDefined ⇒
      n == name

    case _ ⇒ false
  }
};

final case class EnumDefinition(
  comment : Option[Comment],
  _name : Name,
  instances : List[Name],
  body : List[Field]) extends Declaration(_name);

final case class InterfaceDefinition(
  comment : Option[Comment],
  _name : Name,
  superTypes : List[Name],
  body : List[Field]) extends Declaration(_name);

final case class Typedef(
  _name : Name,
  description : Description,
  target : Type) extends Declaration(_name);