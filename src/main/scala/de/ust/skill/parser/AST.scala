/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import java.io.File

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.input.Positional

import de.ust.skill.ir.Comment
import de.ust.skill.ir.Hint
import de.ust.skill.ir.Restriction

/**
 * The AST is used to turn skill definitions into Java IR.
 *
 * @author Timm Felden
 */
sealed abstract class Node;
sealed abstract class Declaration(val name : Name, val declaredIn : File) extends Node {
  override def equals(other : Any) : Boolean = other match {
    case o : Declaration ⇒ o.getClass == getClass && o.name == name
    case _               ⇒ false
  }

  override def hashCode() : Int = name.hashCode
}
sealed abstract class DeclarationWithBody(_name : Name, _declaredIn : File, val body : List[AbstractField])
  extends Declaration(_name, _declaredIn);

final class Description(val comment : Comment, val restrictions : List[Restriction],
                        val hints : List[Hint]) extends Node;

sealed abstract class Type extends Node {
  override def equals(other : Any) : Boolean;
}

final class MapType(val baseTypes : List[BaseType]) extends Type {
  override def equals(other : Any) : Boolean = other match {
    case o : MapType ⇒ o.baseTypes.sameElements(baseTypes);
    case _           ⇒ false
  }
}

final class SetType(val baseType : BaseType) extends Type {
  override def equals(other : Any) : Boolean = other match {
    case o : SetType ⇒ o.baseType == baseType
    case _           ⇒ false
  }
}

final class ListType(val baseType : BaseType) extends Type {
  override def equals(other : Any) : Boolean = other match {
    case o : SetType ⇒ o.baseType == baseType
    case _           ⇒ false
  }
}

sealed class ArrayType(val baseType : BaseType) extends Type {
  override def equals(other : Any) : Boolean = other match {
    case o : SetType ⇒ o.baseType == baseType
    case _           ⇒ false
  }
}
final class ConstantLengthArrayType(baseType : BaseType, val length : Long) extends ArrayType(baseType);

final case class BaseType(val name : Name) extends Type {
  override def toString : String = name.source
}

sealed abstract class AbstractField(val name : Name) extends Node;

sealed abstract class Field(val t : Type, name : Name) extends AbstractField(name) {
  var description : Description = new Description(Comment.NoComment.get, List[Restriction](), List[Hint]());
}

final class Constant(t : Type, name : Name, val value : Long) extends Field(t, name);

final class Data(val isAuto : Boolean, t : Type, name : Name) extends Field(t, name);

/**
 * @param targetType if none, a type will be inserted by the type checker
 */
final class View(val comment : Comment, var targetType : Option[Name], val targetField : Name, val t : Type,
                 name : Name) extends AbstractField(name) {
  /**
   * established by type checker
   */
  var target : AbstractField = _;

  override def toString : String = s"""
  view ${targetType.map(_.CapitalCase + ".").getOrElse("")}$targetField as
  $t $name;
"""
}

final class Customization(val comment : Comment, val language : Name, val options : Map[Name, List[String]],
                          val typeImage : String, name : Name)
    extends AbstractField(name);

/**
 * Representation of skill names.
 */
final class Name(val source : String, delimitWithUnderscores : Boolean, delimitWithCamelCase : Boolean)
    extends Positional {

  def this(irSource : de.ust.skill.ir.Name) = this(irSource.cStyle, true, false);

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

  lazy val Ada_Style : String = parts.tail.foldLeft(parts.head.capitalize)(_ + "_" + _.capitalize)

  lazy val c_style : String = parts.tail.foldLeft(parts.head.toLowerCase)(_ + "_" + _.toLowerCase)

  def ir : de.ust.skill.ir.Name = new de.ust.skill.ir.Name(parts, lowercase);

  override def equals(o : Any) : Boolean = o match {
    case o : Name ⇒ o.lowercase.equals(lowercase)
    case _        ⇒ false
  }

  override def hashCode : Int = lowercase.hashCode

  override def toString : String = CapitalCase

  def <(arg : Name) : Boolean = lowercase < arg.lowercase
}

final class UserType(
  _declaredIn : File,
  val description : Description,
  _name : Name,
  val superTypes : List[Name],
  _body : List[AbstractField]) extends DeclarationWithBody(_name, _declaredIn, _body);

final class EnumDefinition(
  _declaredIn : File,
  val comment : Comment,
  _name : Name,
  val instances : List[Name],
  _body : List[AbstractField]) extends DeclarationWithBody(_name, _declaredIn, _body);

final case class InterfaceDefinition(
  _declaredIn : File,
  val comment : Comment,
  _name : Name,
  val superTypes : List[Name],
  _body : List[AbstractField]) extends DeclarationWithBody(_name, _declaredIn, _body);

final case class Typedef(
  _declaredIn : File,
  _name : Name,
  description : Description,
  target : Type) extends Declaration(_name, _declaredIn);
