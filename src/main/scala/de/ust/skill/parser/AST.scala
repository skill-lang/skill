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
import scala.util.parsing.input.Positional
import java.io.File
import scala.collection.mutable.ListBuffer
import de.ust.skill.ir.Comment

/**
 * The AST is used to turn skill definitions into Java IR.
 *
 * @author Timm Felden
 */
sealed abstract class Node;
sealed abstract class Declaration(val name : Name, val declaredIn : File) extends Node;

final class Description(val comment : Comment, val restrictions : List[Restriction],
                        val hints : List[Hint]) extends Node;

sealed abstract class Type extends Node;

final class MapType(val baseTypes : List[BaseType]) extends Type;

final class SetType(val baseType : BaseType) extends Type;

final class ListType(val baseType : BaseType) extends Type;

sealed class ArrayType(val baseType : BaseType) extends Type;
final class ConstantLengthArrayType(baseType : BaseType, val length : Long) extends ArrayType(baseType);

final case class BaseType(val name : Name) extends Type {
  override def toString : String = name.source
}

sealed abstract class Field(val t : Type, val name : Name) extends Node {
  var description : Description = new Description(Comment.NoComment.get, List[Restriction](), List[Hint]());
}

final class Constant(t : Type, name : Name, val value : Long) extends Field(t, name);

final class Data(val isAuto : Boolean, t : Type, name : Name) extends Field(t, name);

final case class View(val declaredInType : Option[Name], val oldName : Name, val target : Field) extends Field(target.t, target.name);

/**
 * Representation of skill names.
 */
final class Name(val source : String, delimitWithUnderscores : Boolean, delimitWithCamelCase : Boolean) extends Positional {
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

  lazy val c_style : String = parts.tail.foldLeft(parts.head.toUpperCase)(_+"_"+_.toLowerCase)

  def ir = new de.ust.skill.ir.Name(parts, lowercase);

  override def equals(o : Any) = o match {
    case o : Name ⇒ o.lowercase == lowercase
    case _        ⇒ false
  }

  override def hashCode = lowercase.hashCode

  override def toString = CapitalCase

  def <(arg : Name) = lowercase < arg.lowercase
}

object ChangeModifier extends Enumeration {
  type ChangeModifier = Value
  val ++, --, set = Value
}

final case class UserType(
    _declaredIn : File,
    change : Option[ChangeModifier.ChangeModifier],
    description : Description,
    _name : Name, superTypes : List[Name], body : List[Field]) extends Declaration(_name, _declaredIn) {

  override def equals(other : Any) = other match {
    case UserType(_, Some(ChangeModifier.set), _, n, _, _) if change.isDefined ⇒
      change.get == ChangeModifier.set && n == name

    case UserType(_, None, _, n, _, _) if !change.isDefined ⇒
      n == name

    case _ ⇒ false
  }
};

final case class EnumDefinition(
  _declaredIn : File,
  comment : Comment,
  _name : Name,
  instances : List[Name],
  body : List[Field]) extends Declaration(_name, _declaredIn);

final case class InterfaceDefinition(
  _declaredIn : File,
  comment : Comment,
  _name : Name,
  superTypes : List[Name],
  body : List[Field]) extends Declaration(_name, _declaredIn);

final case class Typedef(
  _declaredIn : File,
  _name : Name,
  description : Description,
  target : Type) extends Declaration(_name, _declaredIn);