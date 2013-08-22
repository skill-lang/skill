package de.ust.skill.parser

/**
 * The AST is used to turn skill definitions into source code.
 *
 * @author Timm Felden
 */
sealed abstract class Node {
  override def equals(obj: Any) = ASTEqualityChecker.check(this, obj)
}

final class Restriction(val name: String, val args: List[Any]) extends Node {
}

final class Hint(val name: String) extends Node {
}

final class Description(val comment: Option[String], val restrictions: List[Restriction],
  val hints: List[Hint]) extends Node {
}

sealed abstract class Type extends Node {
}

final class MapType(val args: List[BaseType]) extends Type {
}

final class SetType(val baseType: BaseType) extends Type {
}

final class ListType(val baseType: BaseType) extends Type {
}

sealed class ArrayType(val baseType: BaseType) extends Type {
}
final class ConstantArrayType(baseType: BaseType, val length: Long) extends ArrayType(baseType) {
}

final class BaseType(val name: String) extends Type {
  override def toString = name
}

sealed abstract class Field(val t: Type, val name: String) extends Node {
  var description: Description = new Description(None, List[Restriction](), List[Hint]());
}

final class Constant(t: Type, name: String, val value: Long) extends Field(t, name) {
}

final class Data(val isAuto: Boolean, t: Type, name: String) extends Field(t, name) {
}

final class Definition(
  val description: Description,
  val name: String, val parent: Option[String], val body: List[Field]) extends Node {
}
