/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 06.12.2016                               *
 * |___/_|\_\_|_|____|    by: feldentm                                        *
\*                                                                            */
package de.ust.skill.sir

import de.ust.skill.common.scala.SkillID
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.UnknownObject

/**
 *  The root of the type hierarchy.
 */
sealed class Type (_skillID : SkillID) extends SkillObject(_skillID) {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  override def prettyString : String = s"Type(#$skillID)"

  override def getTypeName : String = "type"

  override def toString = "Type#"+skillID
}

object Type {

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: Type])
      extends Type(_skillID) with UnknownObject[Type] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

/**
 *  All types defined by skill.
 */
sealed class BuiltinType (_skillID : SkillID) extends Type(_skillID) {
  override def prettyString : String = s"BuiltinType(#$skillID)"

  override def getTypeName : String = "builtintype"

  override def toString = "BuiltinType#"+skillID
}

object BuiltinType {

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: BuiltinType])
      extends BuiltinType(_skillID) with UnknownObject[BuiltinType] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

/**
 *  a map type
 */
sealed class MapType (_skillID : SkillID) extends BuiltinType(_skillID) {
  private[sir] def this(_skillID : SkillID, base : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType]) {
    this(_skillID)
    _base = base
  }

  final protected var _base : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType]()
  /**
   *  base types of the map in order of appearance
   */
  def base : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType] = _base
  final private[sir] def Internal_base = _base
  /**
   *  base types of the map in order of appearance
   */
  def `base_=`(base : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType]) : scala.Unit = { _base = base }
  final private[sir] def `Internal_base_=`(v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType]) = _base = v

  override def prettyString : String = s"MapType(#$skillID, base: ${base})"

  override def getTypeName : String = "maptype"

  override def toString = "MapType#"+skillID
}

object MapType {
  def unapply(self : MapType) = Some(self.base)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: MapType])
      extends MapType(_skillID) with UnknownObject[MapType] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

/**
 *  containers with a single base type
 */
sealed class SingleBaseTypeContainer (_skillID : SkillID) extends BuiltinType(_skillID) {
  private[sir] def this(_skillID : SkillID, base : _root_.de.ust.skill.sir.GroundType, kind : java.lang.String) {
    this(_skillID)
    _base = base
    _kind = kind
  }

  final protected var _base : _root_.de.ust.skill.sir.GroundType = null
  /**
   *  the base type of this container
   */
  def base : _root_.de.ust.skill.sir.GroundType = _base
  final private[sir] def Internal_base = _base
  /**
   *  the base type of this container
   */
  def `base_=`(base : _root_.de.ust.skill.sir.GroundType) : scala.Unit = { _base = base }
  final private[sir] def `Internal_base_=`(v : _root_.de.ust.skill.sir.GroundType) = _base = v

  final protected var _kind : java.lang.String = null
  /**
   *  can be one of: "set", "array", "list"
   */
  def kind : java.lang.String = _kind
  final private[sir] def Internal_kind = _kind
  /**
   *  can be one of: "set", "array", "list"
   */
  def `kind_=`(kind : java.lang.String) : scala.Unit = { _kind = kind }
  final private[sir] def `Internal_kind_=`(v : java.lang.String) = _kind = v

  override def prettyString : String = s"SingleBaseTypeContainer(#$skillID, base: ${base}, kind: ${kind})"

  override def getTypeName : String = "singlebasetypecontainer"

  override def toString = "SingleBaseTypeContainer#"+skillID
}

object SingleBaseTypeContainer {
  def unapply(self : SingleBaseTypeContainer) = Some(self.base, self.kind)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: SingleBaseTypeContainer])
      extends SingleBaseTypeContainer(_skillID) with UnknownObject[SingleBaseTypeContainer] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

/**
 * 
 *  @note  kind is always "array"
 */
sealed class ConstantLengthArrayType (_skillID : SkillID) extends SingleBaseTypeContainer(_skillID) {
  private[sir] def this(_skillID : SkillID, length : Long, base : _root_.de.ust.skill.sir.GroundType, kind : java.lang.String) {
    this(_skillID)
    _length = length
    _base = base
    _kind = kind
  }

  final protected var _length : Long = 0
  /**
   *  the constant length of this array
   */
  def length : Long = _length
  final private[sir] def Internal_length = _length
  /**
   *  the constant length of this array
   */
  def `length_=`(length : Long) : scala.Unit = { _length = length }
  final private[sir] def `Internal_length_=`(v : Long) = _length = v

  override def prettyString : String = s"ConstantLengthArrayType(#$skillID, length: ${length}, base: ${base}, kind: ${kind})"

  override def getTypeName : String = "constantlengtharraytype"

  override def toString = "ConstantLengthArrayType#"+skillID
}

object ConstantLengthArrayType {
  def unapply(self : ConstantLengthArrayType) = Some(self.length, self.base, self.kind)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: ConstantLengthArrayType])
      extends ConstantLengthArrayType(_skillID) with UnknownObject[ConstantLengthArrayType] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

/**
 *  simple predefined types, such as i32
 */
sealed class SimpleType (_skillID : SkillID) extends BuiltinType(_skillID) with GroundType {
  private[sir] def this(_skillID : SkillID, name : _root_.de.ust.skill.sir.Identifier) {
    this(_skillID)
    _name = name
  }

  override def prettyString : String = s"SimpleType(#$skillID, name: ${name})"

  override def getTypeName : String = "simpletype"

  override def toString = "SimpleType#"+skillID
}

object SimpleType {
  def unapply(self : SimpleType) = Some(self.name)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: SimpleType])
      extends SimpleType(_skillID) with UnknownObject[SimpleType] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

/**
 *  representation of constant integers
 */
sealed class ConstantInteger (_skillID : SkillID) extends SimpleType(_skillID) {
  private[sir] def this(_skillID : SkillID, value : Long, name : _root_.de.ust.skill.sir.Identifier) {
    this(_skillID)
    _value = value
    _name = name
  }

  final protected var _value : Long = 0
  def value : Long = _value
  final private[sir] def Internal_value = _value
  def `value_=`(value : Long) : scala.Unit = { _value = value }
  final private[sir] def `Internal_value_=`(v : Long) = _value = v

  override def prettyString : String = s"ConstantInteger(#$skillID, value: ${value}, name: ${name})"

  override def getTypeName : String = "constantinteger"

  override def toString = "ConstantInteger#"+skillID
}

object ConstantInteger {
  def unapply(self : ConstantInteger) = Some(self.value, self.name)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: ConstantInteger])
      extends ConstantInteger(_skillID) with UnknownObject[ConstantInteger] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

sealed class UserdefinedType (_skillID : SkillID) extends Type(_skillID) with GroundType with Annotations {
  private[sir] def this(_skillID : SkillID, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) {
    this(_skillID)
    _comment = comment
    _name = name
    _hints = hints
    _restrictions = restrictions
  }

  final protected var _comment : _root_.de.ust.skill.sir.Comment = null
  /**
   *  the comment provided by the user or null
   */
  def comment : _root_.de.ust.skill.sir.Comment = _comment
  final private[sir] def Internal_comment = _comment
  /**
   *  the comment provided by the user or null
   */
  def `comment_=`(comment : _root_.de.ust.skill.sir.Comment) : scala.Unit = { _comment = comment }
  final private[sir] def `Internal_comment_=`(v : _root_.de.ust.skill.sir.Comment) = _comment = v

  override def prettyString : String = s"UserdefinedType(#$skillID, comment: ${comment}, name: ${name}, hints: ${hints}, restrictions: ${restrictions})"

  override def getTypeName : String = "userdefinedtype"

  override def toString = "UserdefinedType#"+skillID
}

object UserdefinedType {
  def unapply(self : UserdefinedType) = Some(self.comment, self.name, self.hints, self.restrictions)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: UserdefinedType])
      extends UserdefinedType(_skillID) with UnknownObject[UserdefinedType] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

/**
 *  A regular type definition
 */
sealed class ClassType (_skillID : SkillID) extends UserdefinedType(_skillID) {
  private[sir] def this(_skillID : SkillID, fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike], interfaces : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType], `super` : _root_.de.ust.skill.sir.ClassType, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) {
    this(_skillID)
    _fields = fields
    _interfaces = interfaces
    _super = `super`
    _comment = comment
    _name = name
    _hints = hints
    _restrictions = restrictions
  }

  final protected var _fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike]()
  /**
   *  there might be multiple fields under the same name in this type, as the class can be a) part of configuration b)
   *  contain custom fields
   */
  def fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike] = _fields
  final private[sir] def Internal_fields = _fields
  /**
   *  there might be multiple fields under the same name in this type, as the class can be a) part of configuration b)
   *  contain custom fields
   */
  def `fields_=`(fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike]) : scala.Unit = { _fields = fields }
  final private[sir] def `Internal_fields_=`(v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike]) = _fields = v

  final protected var _interfaces : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType] = scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType]()
  def interfaces : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType] = _interfaces
  final private[sir] def Internal_interfaces = _interfaces
  def `interfaces_=`(interfaces : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType]) : scala.Unit = { _interfaces = interfaces }
  final private[sir] def `Internal_interfaces_=`(v : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType]) = _interfaces = v

  final protected var _super : _root_.de.ust.skill.sir.ClassType = null
  def `super` : _root_.de.ust.skill.sir.ClassType = _super
  final private[sir] def Internal_super = _super
  def `super_=`(`super` : _root_.de.ust.skill.sir.ClassType) : scala.Unit = { _super = `super` }
  final private[sir] def `Internal_super_=`(v : _root_.de.ust.skill.sir.ClassType) = _super = v

  override def prettyString : String = s"ClassType(#$skillID, fields: ${fields}, interfaces: ${interfaces}, super: ${`super`}, comment: ${comment}, name: ${name}, hints: ${hints}, restrictions: ${restrictions})"

  override def getTypeName : String = "classtype"

  override def toString = "ClassType#"+skillID
}

object ClassType {
  def unapply(self : ClassType) = Some(self.fields, self.interfaces, self.`super`, self.comment, self.name, self.hints, self.restrictions)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: ClassType])
      extends ClassType(_skillID) with UnknownObject[ClassType] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

sealed class EnumType (_skillID : SkillID) extends UserdefinedType(_skillID) {
  private[sir] def this(_skillID : SkillID, fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike], instances : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Identifier], comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) {
    this(_skillID)
    _fields = fields
    _instances = instances
    _comment = comment
    _name = name
    _hints = hints
    _restrictions = restrictions
  }

  final protected var _fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike]()
  /**
   *  there might be multiple fields under the same name in this type, as the class can be a) part of configuration b)
   *  contain custom fields
   */
  def fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike] = _fields
  final private[sir] def Internal_fields = _fields
  /**
   *  there might be multiple fields under the same name in this type, as the class can be a) part of configuration b)
   *  contain custom fields
   */
  def `fields_=`(fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike]) : scala.Unit = { _fields = fields }
  final private[sir] def `Internal_fields_=`(v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike]) = _fields = v

  final protected var _instances : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Identifier] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Identifier]()
  def instances : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Identifier] = _instances
  final private[sir] def Internal_instances = _instances
  def `instances_=`(instances : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Identifier]) : scala.Unit = { _instances = instances }
  final private[sir] def `Internal_instances_=`(v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Identifier]) = _instances = v

  override def prettyString : String = s"EnumType(#$skillID, fields: ${fields}, instances: ${instances}, comment: ${comment}, name: ${name}, hints: ${hints}, restrictions: ${restrictions})"

  override def getTypeName : String = "enumtype"

  override def toString = "EnumType#"+skillID
}

object EnumType {
  def unapply(self : EnumType) = Some(self.fields, self.instances, self.comment, self.name, self.hints, self.restrictions)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: EnumType])
      extends EnumType(_skillID) with UnknownObject[EnumType] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

sealed class InterfaceType (_skillID : SkillID) extends UserdefinedType(_skillID) {
  private[sir] def this(_skillID : SkillID, fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike], interfaces : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType], `super` : _root_.de.ust.skill.sir.ClassType, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) {
    this(_skillID)
    _fields = fields
    _interfaces = interfaces
    _super = `super`
    _comment = comment
    _name = name
    _hints = hints
    _restrictions = restrictions
  }

  final protected var _fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike]()
  /**
   *  there might be multiple fields under the same name in this type, as the class can be a) part of configuration b)
   *  contain custom fields
   */
  def fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike] = _fields
  final private[sir] def Internal_fields = _fields
  /**
   *  there might be multiple fields under the same name in this type, as the class can be a) part of configuration b)
   *  contain custom fields
   */
  def `fields_=`(fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike]) : scala.Unit = { _fields = fields }
  final private[sir] def `Internal_fields_=`(v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike]) = _fields = v

  final protected var _interfaces : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType] = scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType]()
  def interfaces : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType] = _interfaces
  final private[sir] def Internal_interfaces = _interfaces
  def `interfaces_=`(interfaces : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType]) : scala.Unit = { _interfaces = interfaces }
  final private[sir] def `Internal_interfaces_=`(v : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType]) = _interfaces = v

  final protected var _super : _root_.de.ust.skill.sir.ClassType = null
  def `super` : _root_.de.ust.skill.sir.ClassType = _super
  final private[sir] def Internal_super = _super
  def `super_=`(`super` : _root_.de.ust.skill.sir.ClassType) : scala.Unit = { _super = `super` }
  final private[sir] def `Internal_super_=`(v : _root_.de.ust.skill.sir.ClassType) = _super = v

  override def prettyString : String = s"InterfaceType(#$skillID, fields: ${fields}, interfaces: ${interfaces}, super: ${`super`}, comment: ${comment}, name: ${name}, hints: ${hints}, restrictions: ${restrictions})"

  override def getTypeName : String = "interfacetype"

  override def toString = "InterfaceType#"+skillID
}

object InterfaceType {
  def unapply(self : InterfaceType) = Some(self.fields, self.interfaces, self.`super`, self.comment, self.name, self.hints, self.restrictions)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: InterfaceType])
      extends InterfaceType(_skillID) with UnknownObject[InterfaceType] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

sealed class TypeDefinition (_skillID : SkillID) extends UserdefinedType(_skillID) {
  private[sir] def this(_skillID : SkillID, target : _root_.de.ust.skill.sir.Type, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) {
    this(_skillID)
    _target = target
    _comment = comment
    _name = name
    _hints = hints
    _restrictions = restrictions
  }

  final protected var _target : _root_.de.ust.skill.sir.Type = null
  /**
   *  the target of this definition
   */
  def target : _root_.de.ust.skill.sir.Type = _target
  final private[sir] def Internal_target = _target
  /**
   *  the target of this definition
   */
  def `target_=`(target : _root_.de.ust.skill.sir.Type) : scala.Unit = { _target = target }
  final private[sir] def `Internal_target_=`(v : _root_.de.ust.skill.sir.Type) = _target = v

  override def prettyString : String = s"TypeDefinition(#$skillID, target: ${target}, comment: ${comment}, name: ${name}, hints: ${hints}, restrictions: ${restrictions})"

  override def getTypeName : String = "typedefinition"

  override def toString = "TypeDefinition#"+skillID
}

object TypeDefinition {
  def unapply(self : TypeDefinition) = Some(self.target, self.comment, self.name, self.hints, self.restrictions)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: TypeDefinition])
      extends TypeDefinition(_skillID) with UnknownObject[TypeDefinition] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

/**
 *  Types without type arguments.
 */
sealed trait GroundType extends Type {
  final protected var _name : _root_.de.ust.skill.sir.Identifier = null
  /**
   *  the skill name used to identify this type, e.g. i32.
   */
  def name : _root_.de.ust.skill.sir.Identifier = _name
  final private[sir] def Internal_name = _name
  /**
   *  the skill name used to identify this type, e.g. i32.
   */
  def `name_=`(name : _root_.de.ust.skill.sir.Identifier) : scala.Unit = { _name = name }
  final private[sir] def `Internal_name_=`(v : _root_.de.ust.skill.sir.Identifier) = _name = v

}
