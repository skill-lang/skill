/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait TypeInfoMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/TypeInfo.scala")
    //package
    out.write(s"""package ${packagePrefix}internal""")
    out.write(s"""

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import ${packagePrefix}internal.pool.AbstractPool
""")
    out.write("""
/**
 * The type info objects are used to reflect the type information stored in a skill file. They are required for
 *  the deserialization of objects into storage pools.
 */
abstract class TypeInfo {
  def toString(): String

  /**
   * serializable type ID according to skill §App.F
   */
  def typeId: Long

  override def equals(obj: Any) = if (obj.isInstanceOf[TypeInfo]) typeId == obj.asInstanceOf[TypeInfo].typeId else false
  override def hashCode = typeId.toInt
}

sealed abstract class ConstantIntegerInfo[T] extends TypeInfo {
  def value: T

  def expect(arg: T) = assert(arg == value)
}

case class ConstantI8Info(value: Byte) extends ConstantIntegerInfo[Byte] {
  override def toString(): String = "const i8 = "+("%02X" format value)

  def typeId: Long = 0
}
case class ConstantI16Info(value: Short) extends ConstantIntegerInfo[Short] {
  override def toString(): String = "const i16 = "+("%04X" format value)

  def typeId: Long = 1
}
case class ConstantI32Info(value: Int) extends ConstantIntegerInfo[Int] {
  override def toString(): String = "const i32 = "+value.toHexString

  def typeId: Long = 2
}
case class ConstantI64Info(value: Long) extends ConstantIntegerInfo[Long] {
  override def toString(): String = "const i64 = "+value.toHexString

  def typeId: Long = 3
}
case class ConstantV64Info(value: Long) extends ConstantIntegerInfo[Long] {
  override def toString(): String = "const v64 = "+value.toHexString

  def typeId: Long = 4
}

sealed abstract class IntegerInfo extends TypeInfo {}

case object I8Info extends IntegerInfo {
  override def toString(): String = "i8"

  def typeId: Long = 7
}
case object I16Info extends IntegerInfo {
  override def toString(): String = "i16"

  def typeId: Long = 8
}
case object I32Info extends IntegerInfo {
  override def toString(): String = "i32"

  def typeId: Long = 9
}
case object I64Info extends IntegerInfo {
  override def toString(): String = "i64"

  def typeId: Long = 10
}
case object V64Info extends IntegerInfo {
  override def toString(): String = "v64"

  def typeId: Long = 11
}

case object AnnotationInfo extends TypeInfo {
  override def toString(): String = "annotation"

  def typeId: Long = 5
}

case object BoolInfo extends TypeInfo {
  override def toString(): String = "bool"

  def typeId: Long = 6
}
case object F32Info extends TypeInfo {
  override def toString(): String = "f32"

  def typeId: Long = 12
}
case object F64Info extends TypeInfo {
  override def toString(): String = "f64"

  def typeId: Long = 13
}

case object StringInfo extends TypeInfo {
  override def toString = "string"

  def typeId: Long = 14
}

sealed abstract class CompoundTypeInfo extends TypeInfo {}

class ConstantLengthArrayInfo(val length: Int, var groundType: TypeInfo) extends CompoundTypeInfo {

  override def toString(): String = groundType+"["+length+"]"

  def typeId: Long = 15
}
class VariableLengthArrayInfo(var groundType: TypeInfo) extends CompoundTypeInfo {

  override def toString(): String = groundType+"[]"

  def typeId: Long = 17
}

class ListInfo(var groundType: TypeInfo) extends CompoundTypeInfo {

  override def toString(): String = "list<"+groundType+">"

  def typeId: Long = 18
}
class SetInfo(var groundType: TypeInfo) extends CompoundTypeInfo {

  override def toString(): String = "set<"+groundType+">"

  def typeId: Long = 19
}
class MapInfo(var groundType: List[TypeInfo]) extends CompoundTypeInfo {

  override def toString(): String = groundType.mkString("map<", ", ", ">")

  def typeId: Long = 20
}

/**
 * This type is used to ease deserialization a lot. There must not be any instances of this class in a usable reflection
 *  pool.
 */
case class PreliminaryUserType(index: Long) extends TypeInfo {
  override def toString(): String = "<preliminary usertype: "+index+">"

  def typeId: Long = 32 + index

  /**
   * there is no hashcode for preliminary usertypes. They must not be used as keys!
   */
  override def hashCode = ???
  override def equals(obj: Any) = obj match {
    case PreliminaryUserType(idx) ⇒ idx == index
    case t: UserType              ⇒ typeId == t.typeId
    case t: AbstractPool          ⇒ t.typeId == index
    case _                        ⇒ false
  }
}

case class NamedUserType(name: String) extends TypeInfo {
  override def toString(): String = "<named usertype: "+name+">"

  def typeId: Long = -1L

  override def hashCode = name.hashCode
  override def equals(obj: Any) = obj match {
    case NamedUserType(n) ⇒ n == name
    case t: UserType      ⇒ t.name == name
    case t: AbstractPool  ⇒ t.name == name
    case _ ⇒ false
  }
}

/**
 * Representation of user types in the *read* world. The modify/write world will use storage pools.
 */
class UserType(
  val index: Long,
  val name: String,
  val superName: Option[String])
    extends TypeInfo {

  // Total number of instances seen until now.
  var instanceCount = 0L;

  private final var _fields = new HashMap[String, FieldDeclaration]
  private final var _fieldByIndex = new ArrayBuffer[FieldDeclaration]
  def fields = _fields
  def fieldByIndex(i: Int) = _fieldByIndex(i)
  def addField(f: FieldDeclaration) {
    _fields.put(f.name, f)
    _fieldByIndex += f
  }

  private[internal] var blockInfos = new ListBuffer[BlockInfo]()
  def addBlockInfo(b: BlockInfo) {
    instanceCount += b.count
    blockInfos.append(b)
  }

  // various links between types
  var baseType: UserType = superName match { case None ⇒ this; case _ ⇒ null }
  var superType: UserType = null
  var subTypes = new ArrayBuffer[UserType]

  /**
   * Pretty printed skill declaration of this user type.
   */
  def getDeclaration(): String = {
    var r = new StringBuilder(name)
    if (superName.isDefined)
      r ++= ":" ++= superName.get

    r ++= "{"
    fields.values.foreach({ f ⇒ r.append(s" ${f.t.toString} ${f.name};") });
    r ++= "}"

    return r.toString
  }

  override def toString = name

  def typeId: Long = 32 + index

  override def hashCode = name.hashCode
  override def equals(obj: Any) = obj match {
    case NamedUserType(n) ⇒ n == name
    case t: UserType      ⇒ t.name == name
    case t: AbstractPool  ⇒ t.name == name
    case _ ⇒ false
  }
}
""")

    //class prefix
    out.close()
  }
}
