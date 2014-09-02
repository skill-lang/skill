/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import de.ust.skill.ir.Restriction
import de.ust.skill.ir.Hint
import scala.collection.JavaConversions._

/**
 * The AST is used to turn skill definitions into Java IR.
 *
 * @author Timm Felden
 */
sealed abstract class Node;
sealed abstract class Declaration extends Node;

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
final class Name(source : String, delimitWithUnderscores : Boolean) {
  // @note this may not be correct if more then two _ are used
  val parts : List[String] = source.split("_").to.map { s ⇒ if (s.isEmpty()) "_" else s }.to

  lazy val camelCase : String = if (delimitWithUnderscores) parts.foldLeft("")(_ + _) else source;

  val lowercase : String = if (delimitWithUnderscores) parts.foldLeft("")(_ + _.toLowerCase) else source.toLowerCase;

  lazy val CapitalCase : String = if (delimitWithUnderscores)
    parts.tail.foldLeft(parts.head.capitalize)(_ + _)
  else
    source.capitalize;

  lazy val ADA_STYLE : String = if (delimitWithUnderscores)
    parts.tail.foldLeft(parts.head.toUpperCase)(_+"_"+_.toUpperCase)
  else
    source.toUpperCase();

  def ir = new de.ust.skill.ir.Name(parts, lowercase);

  override def equals(o : Any) = o match {
    case o : Name ⇒ o.lowercase == lowercase
    case _        ⇒ false
  }

  override def hashCode = lowercase.hashCode

  override def toString = lowercase
}

final case class Definition(
    val change : Option[ChangeModifier.ChangeModifier],
    val description : Description,
    val name : Name, val parent : Option[String], val interfaces : List[String], val body : List[Field]) extends Declaration {

  override def equals(other : Any) = other match {
    case Definition(Some(ChangeModifier.set), d, n, p, i, b) if change.isDefined ⇒
      change.get == ChangeModifier.set && n == name

    case Definition(None, d, n, p, i, b) if !change.isDefined ⇒
      n == name

    case _ ⇒ false
  }
};

object ChangeModifier extends Enumeration {
  type ChangeModifier = Value
  val ++, --, set = Value
}

final case class EnumDefinition(
    val comment : Option[Comment],
    val name : String,
    val instances : List[String],
    val body : List[Field]) extends Declaration {

}

final case class InterfaceDefinition(
    val comment : Option[Comment],
    val name : String,
    val superType : List[String],
    val body : List[Field]) extends Declaration {

}