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
import de.ust.skill.ir.restriction.SingletonRestriction

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
import scala.reflect.ClassTag

import ${packagePrefix}api._

/**
 * @note representation of the type system relies on invariants and heavy abuse of type erasure
 *
 * @author Timm Felden
 */
sealed abstract class FieldType(val typeID : Long) {
  def toString() : String

  override def equals(obj : Any) = obj match {
    case o : FieldType ⇒ o.typeID == typeID
    case _             ⇒ false
  }
  override def hashCode = typeID.toInt
}

sealed abstract class ConstantInteger[T](typeID : Long) extends FieldType(typeID) {
  def value : T

  def expect(arg : T) = assert(arg == value)
}

case class ConstantI8(value : Byte) extends ConstantInteger[Byte](0) {
  override def toString() : String = "const i8 = "+("%02X" format value)
  override def equals(obj : Any) = obj match {
    case ConstantI8(v) ⇒ v == value
    case _             ⇒ false
  }
}
case class ConstantI16(value : Short) extends ConstantInteger[Short](1) {
  override def toString() : String = "const i16 = "+("%04X" format value)
  override def equals(obj : Any) = obj match {
    case ConstantI16(v) ⇒ v == value
    case _              ⇒ false
  }
}
case class ConstantI32(value : Int) extends ConstantInteger[Int](2) {
  override def toString() : String = "const i32 = "+value.toHexString
  override def equals(obj : Any) = obj match {
    case ConstantI32(v) ⇒ v == value
    case _              ⇒ false
  }
}
case class ConstantI64(value : Long) extends ConstantInteger[Long](3) {
  override def toString() : String = "const i64 = "+value.toHexString
  override def equals(obj : Any) = obj match {
    case ConstantI64(v) ⇒ v == value
    case _              ⇒ false
  }
}
case class ConstantV64(value : Long) extends ConstantInteger[Long](4) {
  override def toString() : String = "const v64 = "+value.toHexString
  override def equals(obj : Any) = obj match {
    case ConstantV64(v) ⇒ v == value
    case _              ⇒ false
  }
}

sealed abstract class IntegerType(typeID : Long) extends FieldType(typeID);

case object I8 extends IntegerType(7) {
  override def toString() : String = "i8"
}
case object I16 extends IntegerType(8) {
  override def toString() : String = "i16"
}
case object I32 extends IntegerType(9) {
  override def toString() : String = "i32"
}
case object I64 extends IntegerType(10) {
  override def toString() : String = "i64"
}
case object V64 extends IntegerType(11) {
  override def toString() : String = "v64"
}

case object Annotation extends FieldType(5) {
  override def toString() : String = "annotation"
}

case object BoolType extends FieldType(6) {
  override def toString() : String = "bool"
}
case object F32 extends FieldType(12) {
  override def toString() : String = "f32"
}
case object F64 extends FieldType(13) {
  override def toString() : String = "f64"
}

case object StringType extends FieldType(14) {
  override def toString = "string"
}

sealed abstract class CompoundType(typeID : Long) extends FieldType(typeID);

case class ConstantLengthArray(val length : Long, val groundType : FieldType) extends CompoundType(15) {
  override def toString() : String = groundType+"["+length+"]"
  override def equals(obj : Any) = obj match {
    case ConstantLengthArray(l, g) ⇒ l == length && g == groundType
    case _                         ⇒ false
  }
}

case class VariableLengthArray(val groundType : FieldType) extends CompoundType(17) {
  override def toString() : String = groundType+"[]"
  override def equals(obj : Any) = obj match {
    case VariableLengthArray(g) ⇒ g == groundType
    case _                      ⇒ false
  }
}

case class ListType(val groundType : FieldType) extends CompoundType(18) {
  override def toString() : String = "list<"+groundType+">"
  override def equals(obj : Any) = obj match {
    case ListType(g) ⇒ g == groundType
    case _           ⇒ false
  }
}
case class SetType(val groundType : FieldType) extends CompoundType(19) {
  override def toString() : String = "set<"+groundType+">"
  override def equals(obj : Any) = obj match {
    case SetType(g) ⇒ g == groundType
    case _          ⇒ false
  }
}
case class MapType(val groundType : Seq[FieldType]) extends CompoundType(20) {
  override def toString() : String = groundType.mkString("map<", ", ", ">")
  override def equals(obj : Any) = obj match {
    case MapType(g : Seq[_]) ⇒ groundType.sameElements(g)
    case _                   ⇒ false
  }
}

case class TypeDefinitionIndex(index : Long) extends FieldType(32 + index) {
  override def toString() : String = s"<type definition index: $$index>"
}

case class TypeDefinitionName(name : String) extends FieldType(-1) {
  override def toString() : String = s"<type definition name: $$name>"
  override def equals(obj : Any) = obj match {
    case TypeDefinitionName(n) ⇒ n.equals(name)
    case t : StoragePool[_, _] ⇒ t.name.equals(name)
    case _                     ⇒ false
  }
}

/**
 * Contains type information and instances of user defined types.
 */
sealed abstract class StoragePool[T <: B : ClassTag, B <: SkillType](
  private[internal] val poolIndex : Long,
  val name : String,
  private[internal] val knownFields : HashMap[String, FieldType],
  private[internal] val superPool : Option[StoragePool[_ <: B, B]])
    extends FieldType(32 + poolIndex)
    with Access[T] {

  val superName = superPool.map(_.name)

  /**
   * insert a new T with default values and the given skillID into the pool
   *
   * @note implementation relies on an ascending order of insertion
   */
  private[internal] def insertInstance(skillID : Long) : Boolean

  private[internal] val basePool : BasePool[B]

  /**
   * the sub pools are constructed during construction of all storage pools of a state
   */
  private[internal] val subPools = new ArrayBuffer[SubPool[_ <: T, B]];
  // update sub-pool relation
  this match {
    case t : SubPool[T, B] ⇒
      // @note: we drop the super type, because we can not know what it is in general
      superPool.get.subPools.asInstanceOf[ArrayBuffer[SubPool[_, B]]] += t
    case _ ⇒
  }

  /**
   * adds a sub pool of the correct type; required for unknown generic sub types
   */
  def makeSubPool(poolIndex : Long, name : String) : SubPool[_ <: T, B] = new SubPool[T, B](poolIndex, name, knownFields, this)

  /**
   * the actual field data; contains fields by index
   */
  private[internal] val fields = new ArrayBuffer[FieldDeclaration](knownFields.size);

  /**
   * Adds a new field, checks consistency and updates field data if required.
   */
  final private[internal] def addField(f : FieldDeclaration) {
    if (knownFields.contains(f.name)) {
      // type-check
      if (!f.t.equals(knownFields(f.name)))
        throw TypeMissmatchError(f.t, knownFields(f.name).toString, f.name, name);

    } else {
      // add generic pool
      putFieldMap(f, new HashMap[SkillType, Any])
    }

    fields += f
  }
  private def putFieldMap(f : FieldDeclaration, m : HashMap[SkillType, Any]) {
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
  protected def newDynamicInstances : Iterator[T] = subPools.foldLeft(newObjects.iterator)(_ ++ _.newDynamicInstances)

  /**
   * called after a compress operation to write empty the new objects buffer and to set blocks correctly
   */
  protected def updateAfterCompress : Unit;
  /**
   * called after a prepare append operation to write empty the new objects buffer and to set blocks correctly
   */
  final protected def updateAfterPrepareAppend(chunkMap : HashMap[FieldDeclaration, ChunkInfo]) : Unit = {
    val newInstances = !newDynamicInstances.isEmpty
    val newPool = blockInfos.isEmpty
    val newField = fields.forall(!_.dataChunks.isEmpty)
    if (newPool || newInstances || newField) {

      //build block chunk
      val lcount = newDynamicInstances.size
      //@ note this is the index into the data array and NOT the written lbpsi
      val lbpsi = if (0 == lcount) 0L
      else newDynamicInstances.next.getSkillID

      blockInfos += new BlockInfo(lbpsi, lcount)

      //@note: if this does not hold for p; then it will not hold for p.subPools either!
      if (newInstances || !newPool) {
        //build field chunks
        for (f ← fields) {
          if (f.dataChunks.isEmpty) {
            val c = new BulkChunkInfo(-1, -1, dynamicSize)
            f.dataChunks += c
            chunkMap.put(f, c)
          } else if (newInstances) {
            val c = new SimpleChunkInfo(-1, -1, lbpsi, lcount)
            f.dataChunks += c
            chunkMap.put(f, c)
          }
        }
      }
    }
    //notify sub pools
    for (p ← subPools)
      p.updateAfterPrepareAppend(chunkMap)

    //remove new objects, because they are regular objects by now
    staticData ++= newObjects
    newObjects.clear
  }

  /**
   * returns the skill object at position index
   */
  def getByID(index : Long) : T;

  /**
   * the number of instances of exactly this type, excluding sub-types
   */
  final def staticSize : Long = staticData.size + newObjects.length
  final def staticInstances : Iterator[T] = staticData.iterator ++ newObjects.iterator
  /**
   * the number of static instances loaded from the file
   */
  final protected val staticData = new ArrayBuffer[T]

  /**
   * the number of instances of this type, including sub-types
   * @note this is an O(t) operation, where t is the number of sub-types
   */
  final def dynamicSize : Long = subPools.map(_.dynamicSize).fold(staticSize)(_ + _);
  final override def size = dynamicSize.toInt

  /**
   * The block layout of data.
   */
  private[internal] val blockInfos = new ListBuffer[BlockInfo]()

  final override def toString = name
  final override def equals(obj : Any) = obj match {
    case TypeDefinitionName(n) ⇒ n == name
    case t : FieldType         ⇒ t.typeID == typeID
    case _                     ⇒ false
  }
}

sealed class BasePool[T <: SkillType : ClassTag](poolIndex : Long, name : String, knownFields : HashMap[String, FieldType])
    extends StoragePool[T, T](poolIndex, name, knownFields, None) {

  /**
   * We are the base pool.
   */
  override val basePool = this

  /**
   * increase size of data array
   */
  def resizeData(increase : Int) {
    val d = data
    data = new Array[T](d.length + increase)
    d.copyToArray(data)
  }
  override def insertInstance(skillID : Long) = {
    val i = skillID.toInt - 1
    if (null != data(i))
      false
    else {
      val r = (new SkillType(skillID)).asInstanceOf[T]
      data(i) = r
      staticData += r
      true
    }
  }

  /**
   * compress new instances into the data array and update skillIDs
   */
  def compress {
    val d = new Array[T](dynamicSize.toInt)
    var p = 0;
    for (i ← allInTypeOrder) {
      d(p) = i;
      p += 1;
      i.setSkillID(p);
    }
    data = d
    updateAfterCompress
  }
  final override def updateAfterCompress {
    blockInfos.clear
    blockInfos += BlockInfo(0, data.length)
    newObjects.clear
    for (p ← subPools)
      p.updateAfterCompress
  }

  /**
   * prepare an append operation by moving all new instances into the data array.
   * @param chunkMap field data that has to be written to the file is appended here
   */
  def prepareAppend(chunkMap : HashMap[FieldDeclaration, ChunkInfo]) {
    val newInstances = !newDynamicInstances.isEmpty

    // check if we have to append at all
    if (!fields.forall(_.dataChunks.isEmpty) && !newInstances)
      return ;

    if (newInstances) {
      // we have to resize
      val d = new Array[T](dynamicSize.toInt)
      data.copyToArray(d)
      var i = data.length
      for (instance ← newDynamicInstances) {
        d(i) = instance
        i += 1
        instance.setSkillID(i)
      }
      data = d
    }
    updateAfterPrepareAppend(chunkMap)
  }

  /**
   * the base type data store; use SkillType arrays, because we do not like the type system anyway:)
   */
  private[internal] var data = new Array[T](0)

  override def all : Iterator[T] = data.iterator.asInstanceOf[Iterator[T]] ++ newDynamicInstances
  override def iterator : Iterator[T] = data.iterator.asInstanceOf[Iterator[T]] ++ newDynamicInstances

  override def allInTypeOrder : Iterator[T] = subPools.foldLeft(staticInstances)(_ ++ _.staticInstances)

  /**
   * returns instances directly from the data store
   *
   * @note base pool data access can not fail, because this would yeald an arary store exception at an earlier stage
   */
  override def getByID(index : Long) : T = data(index.toInt - 1).asInstanceOf[T]

  final override def foreach[U](f : T ⇒ U) {
    for (i ← 0 until data.length)
      f(data(i))
    for (i ← newDynamicInstances)
      f(i)
  }

  final override def toArray[B >: T : ClassTag] = {
    val r = new Array[B](size)
    data.copyToArray(r);
    if (data.length != r.length)
      newDynamicInstances.copyToArray(r, data.length)
    r
  }
}

sealed class SubPool[T <: B : ClassTag, B <: SkillType](poolIndex : Long, name : String, knownFields : HashMap[String, FieldType], superPool : StoragePool[_ <: B, B])
    extends StoragePool[T, B](poolIndex, name, knownFields, Some(superPool)) {

  override val basePool = superPool.basePool

  override def insertInstance(skillID : Long) = {
    val i = skillID.toInt - 1
    if (null != data(i))
      false
    else {
      val r = (new SkillType(skillID)).asInstanceOf[T]
      data(i) = r
      staticData += r
      true
    }
  }
  /**
   * the base type data store
   */
  private[internal] def data = basePool.data

  override def all : Iterator[T] = blockInfos.foldRight(newDynamicInstances) { (block, iter) ⇒ basePool.data.view(block.bpsi.toInt, (block.bpsi + block.count).toInt).asInstanceOf[Iterable[T]].iterator ++ iter }
  override def iterator : Iterator[T] = blockInfos.foldRight(newDynamicInstances) { (block, iter) ⇒ basePool.data.view(block.bpsi.toInt, (block.bpsi + block.count).toInt).asInstanceOf[Iterable[T]].iterator ++ iter }

  override def allInTypeOrder : Iterator[T] = subPools.foldLeft(staticInstances)(_ ++ _.staticInstances)

  override def getByID(index : Long) : T = basePool.data(index.toInt - 1).asInstanceOf[T]

  final override def updateAfterCompress {
    blockInfos.clear
    blockInfos += BlockInfo(staticData(0).getSkillID, data.length)
    newObjects.clear
    for (p ← subPools)
      p.updateAfterCompress
  }
}
""")

    for (t ← IR) {
      val typeName = "_root_."+packagePrefix + t.getCapitalName
      val isSingleton = !t.getRestrictions.collect { case r : SingletonRestriction ⇒ r }.isEmpty

      out.write(s"""
final class ${t.getCapitalName}StoragePool(poolIndex : Long${
        if (t.getSuperType == null) ""
        else s",\nsuperPool: ${t.getSuperType.getCapitalName}StoragePool"
      })
    extends ${
        if (t.getSuperType == null) s"BasePool[$typeName]"
        else s"SubPool[$typeName, ${packagePrefix}${t.getBaseType.getCapitalName}]"
      }(
      poolIndex,
      "${t.getSkillName}",
      HashMap[String, FieldType](${
        if (t.getFields.isEmpty) ""
        else (for (f ← t.getFields) yield s"""\n        "${f.getSkillName}" -> ${mapToFieldType(f.getType)}""").mkString("", ",", "\n      ")
      })${
        if (t.getSuperType == null) ""
        else ",\nsuperPool"
      }
    )
    with ${t.getCapitalName}Access {

  override def makeSubPool(poolIndex : Long, name : String) = ${
        if (isSingleton) s"""throw new SkillException("${t.getCapitalName} is a Singleton and can therefore not have any subtypes.")"""
        else s"new ${t.getCapitalName}SubPool(poolIndex, name, this)"
      }

  override def insertInstance(skillID : Long) = {
    val i = skillID.toInt - 1
    if (null != data(i))
      false
    else {
      val r = new $typeName(skillID)
      data(i) = r
      staticData += r
      true
    }
  }

${
        if (isSingleton)
          """  override def get = ???"""
        else
          s"""  override def apply(${makeConstructorArguments(t)}) = {
    val r = new $typeName(-1L${appendConstructorArguments(t)})
    newObjects.append(r)
    r
  }"""
      }
}
""")

      if (!isSingleton) {
        // create a sub pool
        out.write(s"""
final class ${t.getCapitalName}SubPool(poolIndex : Long, name : String, superPool : StoragePool[_ <: $typeName, ${packagePrefix}${t.getBaseType.getCapitalName}])
    extends SubPool[$typeName.SubType, ${packagePrefix}${t.getBaseType.getCapitalName}](
      poolIndex,
      "node",
      HashMap[String, FieldType](
        "color" -> StringType,
        "edges" -> SetType(TypeDefinitionName("node"))
      ),
      superPool
    ) {

  override def makeSubPool(poolIndex : Long, name : String) = new ${t.getCapitalName}SubPool(poolIndex, name, this)

  override def insertInstance(skillID : Long) = {
    val i = skillID.toInt - 1
    if (null != data(i))
      false
    else {
      val r = new ${typeName}.SubType(name, skillID)
      data(i) = r
      staticData += r
      true
    }
  }
}
""")
      }
    }

    //class prefix
    out.close()
  }

  private def mapToFieldType(t : Type) : String = {
    def mapGroundType(t : Type) = t.getSkillName match {
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
      case t : GroundType              ⇒ mapGroundType(t)
      case t : ConstantLengthArrayType ⇒ s"ConstantLengthArray(${t.getLength}, ${mapGroundType(t.getBaseType)})"
      case t : VariableLengthArrayType ⇒ s"VariableLengthArray(${mapGroundType(t.getBaseType)})"
      case t : ListType                ⇒ s"ListType(${mapGroundType(t.getBaseType)})"
      case t : SetType                 ⇒ s"SetType(${mapGroundType(t.getBaseType)})"
      case t : MapType                 ⇒ s"MapType(Seq[FieldType](${t.getBaseTypes.map(mapGroundType).mkString(", ")}))"
      case t : Declaration             ⇒ s"""TypeDefinitionName("${t.getSkillName}")"""
    }
  }
}
