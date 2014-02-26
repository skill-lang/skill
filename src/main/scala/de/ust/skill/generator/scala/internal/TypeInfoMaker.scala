/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import scala.collection.JavaConversions._

import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.VariableLengthArrayType

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

import ${packagePrefix}api._

/**
 * @note representation of the type system relies on invariants and heavy abuse of type erasure
 *
 * @author Timm Felden
 */
sealed abstract class FieldType(val typeID: Long) {
  def toString(): String

  override def equals(obj: Any) = obj match {
    case o: FieldType ⇒ o.typeID == typeID
    case _            ⇒ false
  }
  override def hashCode = typeID.toInt
}

sealed abstract class ConstantInteger[T](typeID: Long) extends FieldType(typeID) {
  def value: T

  def expect(arg: T) = assert(arg == value)
}

case class ConstantI8(value: Byte) extends ConstantInteger[Byte](0) {
  override def toString(): String = "const i8 = "+("%02X" format value)
  override def equals(obj: Any) = obj match {
    case ConstantI8(v) ⇒ v == value
    case _             ⇒ false
  }
}
case class ConstantI16(value: Short) extends ConstantInteger[Short](1) {
  override def toString(): String = "const i16 = "+("%04X" format value)
  override def equals(obj: Any) = obj match {
    case ConstantI16(v) ⇒ v == value
    case _              ⇒ false
  }
}
case class ConstantI32(value: Int) extends ConstantInteger[Int](2) {
  override def toString(): String = "const i32 = "+value.toHexString
  override def equals(obj: Any) = obj match {
    case ConstantI32(v) ⇒ v == value
    case _              ⇒ false
  }
}
case class ConstantI64(value: Long) extends ConstantInteger[Long](3) {
  override def toString(): String = "const i64 = "+value.toHexString
  override def equals(obj: Any) = obj match {
    case ConstantI64(v) ⇒ v == value
    case _              ⇒ false
  }
}
case class ConstantV64(value: Long) extends ConstantInteger[Long](4) {
  override def toString(): String = "const v64 = "+value.toHexString
  override def equals(obj: Any) = obj match {
    case ConstantV64(v) ⇒ v == value
    case _              ⇒ false
  }
}

sealed abstract class IntegerType(typeID: Long) extends FieldType(typeID);

case object I8 extends IntegerType(7) {
  override def toString(): String = "i8"
}
case object I16 extends IntegerType(8) {
  override def toString(): String = "i16"
}
case object I32 extends IntegerType(9) {
  override def toString(): String = "i32"
}
case object I64 extends IntegerType(10) {
  override def toString(): String = "i64"
}
case object V64 extends IntegerType(11) {
  override def toString(): String = "v64"
}

case object Annotation extends FieldType(5) {
  override def toString(): String = "annotation"
}

case object BoolType extends FieldType(6) {
  override def toString(): String = "bool"
}
case object F32 extends FieldType(12) {
  override def toString(): String = "f32"
}
case object F64 extends FieldType(13) {
  override def toString(): String = "f64"
}

case object StringType extends FieldType(14) {
  override def toString = "string"
}

sealed abstract class CompoundType(typeID: Long) extends FieldType(typeID);

case class ConstantLengthArray(val length: Long, val groundType: FieldType) extends CompoundType(15) {
  override def toString(): String = groundType+"["+length+"]"
  override def equals(obj: Any) = obj match {
    case ConstantLengthArray(l, g) ⇒ l == length && g == groundType
    case _                         ⇒ false
  }
}

case class VariableLengthArray(val groundType: FieldType) extends CompoundType(17) {
  override def toString(): String = groundType+"[]"
  override def equals(obj: Any) = obj match {
    case VariableLengthArray(g) ⇒ g == groundType
    case _                      ⇒ false
  }
}

case class ListType(val groundType: FieldType) extends CompoundType(18) {
  override def toString(): String = "list<"+groundType+">"
  override def equals(obj: Any) = obj match {
    case ListType(g) ⇒ g == groundType
    case _           ⇒ false
  }
}
case class SetType(val groundType: FieldType) extends CompoundType(19) {
  override def toString(): String = "set<"+groundType+">"
  override def equals(obj: Any) = obj match {
    case SetType(g) ⇒ g == groundType
    case _          ⇒ false
  }
}
case class MapType(val groundType: Seq[FieldType]) extends CompoundType(20) {
  override def toString(): String = groundType.mkString("map<", ", ", ">")
  override def equals(obj: Any) = obj match {
    case MapType(g: Seq[_]) ⇒ groundType.sameElements(g)
    case _                  ⇒ false
  }
}

case class TypeDefinitionIndex(index: Long) extends FieldType(32 + index) {
  override def toString(): String = s"<type definition index: $$index>"
}

case class TypeDefinitionName(name: String) extends FieldType(-1) {
  override def toString(): String = s"<type definition name: $$name>"
  override def equals(obj: Any) = obj match {
    case TypeDefinitionName(n) ⇒ n.equals(name)
    case _                     ⇒ false
  }
}

/**
 * Contains type information and instances of user defined types.
 */
sealed abstract class StoragePool[T <: SkillType](
    private[internal] val poolIndex: Long,
    val name: String,
    private[internal] val knownFields: HashMap[String, FieldType],
    private[internal] val superPool: Option[StoragePool[_ <: SkillType]]) extends FieldType(32 + poolIndex) {

  val superName = superPool.map(_.name)

  /**
   * insert a new T with default values and the given skillID into the pool
   *
   * @note implementation relies on an ascending order of insertion
   */
  private[internal] def insertInstance(skillID: Long): Boolean

  private[internal] val basePool: BasePool[_ <: SkillType]

  /**
   * the sub pools are constructed during construction of all storage pools of a state
   */
  private[internal] val subPools = new ArrayBuffer[StoragePool[_ <: T]];
  // update sub-pool relation
  if (superPool.isDefined) {
    // @note: we drop the super type, because we can not know what it is in general
    superPool.get.subPools.asInstanceOf[ArrayBuffer[StoragePool[_]]] += this
  }

  /**
   * the actual field data; contains fields by index
   */
  private[internal] val fields = new ArrayBuffer[FieldDeclaration](knownFields.size);

  /**
   * Adds a new field, checks consistency and updates field data if required.
   */
  private[internal] def addField(f: FieldDeclaration) {
    if (knownFields.contains(f.name)) {
      if (!f.equals(knownFields(f.name)))
        ???

    } else {
      putFieldMap(f, new HashMap[SkillType, Any])
    }

    fields += f
  }
  private def putFieldMap(f: FieldDeclaration, m: HashMap[SkillType, Any]) {
    unknownFieldData.put(f, m)
    subPools.foreach(_.putFieldMap(f, m))
  }

  final def allFields = fields.iterator

  /**
   * this map is used to store deserialized field data for each field and instance
   */
  private[internal] val unknownFieldData = new HashMap[FieldDeclaration, HashMap[SkillType, Any]];

  /**
   * All stored objects, which have exactly the type T. Objects are stored as arrays of field entries. The types of the
   *  respective fields can be retrieved using the fieldTypes map.
   */
  private[internal] val newObjects = ArrayBuffer[T]()

  /**
   * returns the skill object at position index
   */
  def getByID(index: Long): SkillType;

  /**
   * the number of instances of exactly this type, excluding sub-types
   */
  def staticSize: Long;
  def staticInstances: Iterator[T]

  /**
   * the number of instances of this type, including sub-types
   * @note this is an O(t) operation, where t is the number of sub-types
   */
  final def dynamicSize: Long = subPools.map(_.dynamicSize).fold(staticSize)(_ + _);

  /**
   * The block layout of data.
   */
  private[internal] val blockInfos = new ListBuffer[BlockInfo]()

  final override def toString = name
  final override def equals(obj: Any) = obj match {
    case TypeDefinitionName(n) ⇒ n == name
    case t: FieldType          ⇒ t.typeID == typeID
    case _                     ⇒ false
  }
}

class BasePool[T <: SkillType](poolIndex: Long, name: String, knownFields: HashMap[String, FieldType])
  extends StoragePool[T](poolIndex, name, knownFields, None)
  with Access[T] {

  /**
   * We are the base pool.
   */
  override val basePool = this

  /**
   * increase size of data array
   */
  def resizeData(increase: Int) {
    val d = data
    data = new Array[SkillType](d.length + increase)
    d.copyToArray(data)
  }
  override def insertInstance(skillID: Long) = {
    val i = skillID.toInt - 1
    if (null != data(i))
      false
    else {
      val r = (new FullyGenericInstance(name, skillID)).asInstanceOf[T]
      data(i) = r
      staticData += r
      true
    }
  }

  /**
   * the base type data store; use SkillType arrays, because we do not like the type system anyway:)
   */
  private[internal] var data = new Array[SkillType](0)

  /**
   * the number of static instances loaded from the file
   */
  protected val staticData = new ArrayBuffer[T]

  override def all: Iterator[T] = data.iterator.asInstanceOf[Iterator[T]] ++ newDynamicInstances
  def newDynamicInstances: Iterator[T] = subPools.foldLeft(newObjects.iterator)(_ ++ _.newObjects.iterator)

  override def allInTypeOrder: Iterator[T] = subPools.foldLeft(staticInstances)(_ ++ _.staticInstances)
  override def staticInstances: Iterator[T] = staticData.iterator ++ newObjects.iterator

  /**
   * returns instances directly from the data store
   *
   * @note base pool data access can not fail, because this would yeald an arary store exception at an earlier stage
   */
  override def getByID(index: Long): T = try { data(index.toInt - 1).asInstanceOf[T] } catch { case e: ArrayIndexOutOfBoundsException ⇒ throw new InvalidPoolIndex(index, data.size, name) }

  override def staticSize: Long = staticData.size + newObjects.length
}

class SubPool[T <: SkillType](poolIndex: Long, name: String, knownFields: HashMap[String, FieldType], superPool: StoragePool[_ <: SkillType])
  extends StoragePool[T](poolIndex, name, knownFields, Some(superPool))
  with Access[T] {

  override val basePool = superPool.basePool

  override def insertInstance(skillID: Long) = {
    val i = skillID.toInt - 1
    if (null != data(i))
      false
    else {
      val r = (new FullyGenericInstance(name, skillID)).asInstanceOf[T]
      data(i) = r
      staticData += r
      true
    }
  }
  /**
   * the base type data store
   */
  private[internal] def data = basePool.data
  /**
   * the number of static instances loaded from the file
   */
  protected val staticData = new ArrayBuffer[T]

  override def all: Iterator[T] = blockInfos.foldRight(newDynamicInstances) { (block, iter) ⇒ basePool.data.view(block.bpsi.toInt, (block.bpsi + block.count).toInt).asInstanceOf[Iterable[T]].iterator ++ iter }
  def newDynamicInstances: Iterator[T] = subPools.foldLeft(newObjects.iterator)(_ ++ _.newObjects.iterator)

  override def allInTypeOrder: Iterator[T] = subPools.foldLeft(staticInstances)(_ ++ _.staticInstances)
  override def staticInstances = staticData.iterator ++ newObjects.iterator

  override def getByID(index: Long): T = try { basePool.getByID(index).asInstanceOf[T] } catch {
    case e: ClassCastException ⇒ throw new SkillException(
      s""${""}"tried to access a "$$name" at index $$index, but it was actually a $${
        basePool.getByID(index).getClass().getName()
      }""${""}", e
    )
    case e: ArrayIndexOutOfBoundsException ⇒ throw new InvalidPoolIndex(index, basePool.dynamicSize, name)
  }

  override def staticSize: Long = staticData.size + newObjects.length
}
""")


    for (t ← IR)
      out.write(s"""
final class ${t.getCapitalName}StoragePool(poolIndex: Long${if(t.getSuperType==null) ""
        else s",\nsuperPool: ${t.getSuperType.getCapitalName}StoragePool"})
    extends ${if(t.getSuperType==null)"Base"else"Sub"}Pool[${packagePrefix}${t.getCapitalName}](
      poolIndex,
      "${t.getSkillName}",
      HashMap[String, FieldType](
${(for(f <- t.getFields) yield s"        ${f.getSkillName} -> ${mapToFieldType(f.getType)}").mkString(",\n")}
      )${
        if(t.getSuperType==null) ""
        else ",\nsuperPool"
      }
    )
    with ${t.getCapitalName}Access {

	override def insertInstance(skillID: Long) = {
    val i = skillID.toInt - 1
    if (null != data(i))
      false
    else {
      val r = new types.${t.getCapitalName}(skillID)
      data(i) = r
      staticData += r
      true
    }
  }

  override def apply(args: Any*) = ???

}
""")

    //class prefix
    out.close()
  }

  private def mapToFieldType(t: Type):String = {
    def mapGroundType(t: Type) = t.getSkillName match {
      case "annotation" ⇒ "Annotation"
      case "bool"       ⇒ "BoolType"
      case "i8"         ⇒ "I8"
      case "i16"        ⇒ "I16"
      case "i32"        ⇒ "I32"
      case "i64"        ⇒ "I64"
      case "v64"        ⇒ "V64"
      case "f32"        ⇒ "F32"
      case "f64"        ⇒ "F64"
      case "string"     ⇒ "StringType"

      case s            ⇒ s"""TypeDefinitionName("$s")"""
    }

    t match {
      case t: GroundType              ⇒ mapGroundType(t)
      case t: ConstantLengthArrayType ⇒ s"ConstantLengthArrayInfo(${t.getLength}, ${mapGroundType(t.getBaseType)})"
      case t: VariableLengthArrayType ⇒ s"VariableLengthArrayInfo(${mapGroundType(t.getBaseType)})"
      case t: ListType                ⇒ s"ListInfo(${mapGroundType(t.getBaseType)})"
      case t: SetType                 ⇒ s"SetInfo(${mapGroundType(t.getBaseType)})"
      case t: MapType                 ⇒ s"MapInfo(List[TypeInfo](${t.getBaseTypes.map(mapGroundType).mkString(",")}))"
      case t: Declaration             ⇒ s"""TypeDefinitionName("${t.getSkillName}")"""
    }
  }
}
