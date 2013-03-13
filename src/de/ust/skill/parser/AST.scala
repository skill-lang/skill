package de.ust.skill.parser

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

final class MapType(val args: List[String]) extends Type {
}

final class SetType(val baseType: String) extends Type {
}

final class ListType(val baseType: String) extends Type {
}

sealed class ArrayType(val baseType: String) extends Type {
}
final class ConstantArrayType(baseType: String, val length: Int) extends ArrayType(baseType) {
}
final class DependentArrayType(baseType: String, val lengthFieldName: String) extends ArrayType(baseType) {
}

final class GroundType(val name: String) extends Type {
}

sealed abstract class Field(val t: Type, val name: String) extends Node {
  var description: Description = new Description(None, List[Restriction](), List[Hint]());
}

final class Constant(t: Type, name: String, val value: Int) extends Field(t, name) {
}

final class Data(val isAuto: Boolean, t: Type, name: String) extends Field(t, name) {
}

final class Definition(
  val description: Description, mod: (Boolean, Boolean),
  val name: String, val parent: Option[String], val body: List[Field]) extends Node {
  val isClass = mod._1
  val isAnnotation = mod._2
}