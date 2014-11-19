/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 19.11.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.WrappedArray
import scala.reflect.ClassTag

import _root_.de.ust.skill.generator.genericBinding.api._
import _root_.de.ust.skill.generator.genericBinding.internal.restrictions._
import _root_.de.ust.skill.generator.genericBinding.internal.streams.FileInputStream
import _root_.de.ust.skill.generator.genericBinding.internal.streams.InStream

/**
 * @param typeID the skill type ID as obtained from the read file or as it would appear in the to be written file
 * @param T the scala type to represent instances of this field type
 *
 * @note representation of the type system relies on invariants and heavy abuse of type erasure
 *
 * @author Timm Felden
 */
sealed abstract class FieldType[T : Manifest](val typeID : Long) {
  private[internal] val m = manifest
  def toString() : String

  override def equals(obj : Any) = obj match {
    case o : FieldType[_] ⇒ o.typeID == typeID
    case _                ⇒ false
  }
  override def hashCode = typeID.toInt

  /**
   * Takes one T out of the stream.
   *
   * @note this function has to be implemented by FieldTypes because of limits of the scala type system
   * (and any other sane type system)
   */
  def readSingleField(in : InStream) : T;
}

sealed abstract class ConstantInteger[T : Manifest](typeID : Long) extends FieldType[T](typeID) {
  def value : T

  def expect(arg : T) = assert(arg == value)

  final override def readSingleField(in : InStream) = value
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

sealed abstract class IntegerType[T : Manifest](typeID : Long) extends FieldType[T](typeID);

case object I8 extends IntegerType[Byte](7) {
  override def readSingleField(in : InStream) = in.i8

  override def toString() : String = "i8"
}
case object I16 extends IntegerType[Short](8) {
  override def readSingleField(in : InStream) = in.i16

  override def toString() : String = "i16"
}
case object I32 extends IntegerType[Int](9) {
  override def readSingleField(in : InStream) = in.i32

  override def toString() : String = "i32"
}
case object I64 extends IntegerType[Long](10) {
  override def readSingleField(in : InStream) = in.i64

  override def toString() : String = "i64"
}
case object V64 extends IntegerType[Long](11) {
  override def readSingleField(in : InStream) = in.v64

  override def toString() : String = "v64"
}

case class Annotation(types : ArrayBuffer[StoragePool[_ <: SkillType, _ <: SkillType]]) extends FieldType[SkillType](5) {
  override def readSingleField(in : InStream) : SkillType = (in.v64, in.v64) match {
    case (0, _) ⇒ null
    case (t, f) ⇒ types(t.toInt - 1).getByID(f)
  }

  override def toString() : String = "annotation"
}

case object BoolType extends FieldType[Boolean](6) {
  override def readSingleField(in : InStream) = in.i8 != 0

  override def toString() : String = "bool"
}
case object F32 extends FieldType[Float](12) {
  override def readSingleField(in : InStream) = in.f32

  override def toString() : String = "f32"
}
case object F64 extends FieldType[Double](13) {
  override def readSingleField(in : InStream) = in.f64

  override def toString() : String = "f64"
}

case class StringType(strings : StringPool) extends FieldType[String](14) {
  override def readSingleField(in : InStream) = strings.get(in.v64)

  override def toString = "string"
}

sealed abstract class CompoundType[T : Manifest](typeID : Long) extends FieldType[T](typeID);

case class ConstantLengthArray[T : Manifest](val length : Long, val groundType : FieldType[T]) extends CompoundType[ArrayBuffer[T]](15) {
  override def readSingleField(in : InStream) = (for (i ← 0 until length.toInt) yield groundType.readSingleField(in)).to

  override def toString() : String = groundType+"["+length+"]"
  override def equals(obj : Any) = obj match {
    case ConstantLengthArray(l, g) ⇒ l == length && g == groundType
    case _                         ⇒ false
  }
}

case class VariableLengthArray[T : Manifest](val groundType : FieldType[T]) extends CompoundType[ArrayBuffer[T]](17) {
  override def readSingleField(in : InStream) = (for (i ← 0 until in.v64.toInt) yield groundType.readSingleField(in)).to

  override def toString() : String = groundType+"[]"
  override def equals(obj : Any) = obj match {
    case VariableLengthArray(g) ⇒ g == groundType
    case _                      ⇒ false
  }
}

case class ListType[T : Manifest](val groundType : FieldType[T]) extends CompoundType[ListBuffer[T]](18) {
  override def readSingleField(in : InStream) = (for (i ← 0 until in.v64.toInt) yield groundType.readSingleField(in)).to

  override def toString() : String = "list<"+groundType+">"
  override def equals(obj : Any) = obj match {
    case ListType(g) ⇒ g == groundType
    case _           ⇒ false
  }
}
case class SetType[T : Manifest](val groundType : FieldType[T]) extends CompoundType[HashSet[T]](19) {
  override def readSingleField(in : InStream) = (for (i ← 0 until in.v64.toInt) yield groundType.readSingleField(in)).to

  override def toString() : String = "set<"+groundType+">"
  override def equals(obj : Any) = obj match {
    case SetType(g) ⇒ g == groundType
    case _          ⇒ false
  }
}
case class MapType[K : Manifest, V : Manifest](val keyType : FieldType[K], val valueType : FieldType[V]) extends CompoundType[HashMap[K, V]](20) {
  override def readSingleField(in : InStream) = (0 until in.v64.toInt).foldLeft(HashMap[K, V]()) {
    case (m, i) ⇒
      m(keyType.readSingleField(in)) = valueType.readSingleField(in); m
  }

  override def toString() : String = s"map<$keyType, $valueType>"
  override def equals(obj : Any) = obj match {
    case MapType(k, v) ⇒ k.equals(keyType) && v.equals(valueType)
    case _             ⇒ false
  }
}

/**
 * This is an intermediate representation of a type until the header is processed completely.
 */
case class TypeDefinitionIndex[T : Manifest](index : Long) extends FieldType[T](32 + index) {
  override def readSingleField(in : InStream) = throw new Error("intended to be dead code")

  override def toString() : String = s"<type definition index: $index>"
  override def equals(obj : Any) = throw new Error("intended to be dead code")
}

case class TypeDefinitionName[T : Manifest](name : String) extends FieldType[T](-1) {
  override def readSingleField(in : InStream) = ???

  override def toString() : String = s"<type definition name: $name>"
  override def equals(obj : Any) = obj match {
    case TypeDefinitionName(n) ⇒ n.equals(name)
    case t : StoragePool[_, _] ⇒ t.name.equals(name)
    case _                     ⇒ false
  }
}

/**
 * Contains type information and instances of user defined types.
 */
sealed abstract class StoragePool[T <: B : Manifest, B <: SkillType](
  private[internal] val poolIndex : Long,
  val name : String,
  private[internal] val knownFields : HashMap[String, (FieldType[_], HashSet[FieldRestriction[_]])],
  private[internal] val superPool : Option[StoragePool[_ <: B, B]])
    extends FieldType[T](32 + poolIndex)
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
  private[internal] val fields = new ArrayBuffer[FieldDeclaration[_]](knownFields.size);
  //! each pool has a magic field 0: v64 skillID
  locally {
    fields += new KnownField[Long](V64, "skillid", 0L, this)
  }

  /**
   * Adds a new field, checks consistency and updates field data if required.
   */
  final private[internal] def addField[T](ID : Int, t : FieldType[T], name : String,
                                          restrictions : HashSet[FieldRestriction[_]]) : FieldDeclaration[T] = {
    if (knownFields.contains(name)) {
      // type-check
      val (knownType, knownRs) = knownFields(name)
      if (!t.equals(knownType))
        throw TypeMissmatchError(t, knownFields(name).toString, name, this.name);

      val f = new KnownField[T](t.asInstanceOf[FieldType[T]], name, ID, this)
      restrictions.foreach(f.addRestriction(_))
      knownRs.foreach(f.addRestriction(_))
      fields += f
      return f

    } else {
      val f = new DistributedField[T](t.asInstanceOf[FieldType[T]], name, ID, this)(t.m)
      restrictions.foreach(f.addRestriction(_))
      fields += f
      return f
    }
  }

  final def allFields = fields.iterator

  /**
   * All stored objects, which have exactly the type T. Objects are stored as arrays of field entries. The types of the
   *  respective fields can be retrieved using the fieldTypes map.
   */
  private[internal] val newObjects = ArrayBuffer[T]()
  protected def newDynamicInstances : Iterator[T] = subPools.foldLeft(newObjects.iterator)(_ ++ _.newDynamicInstances)

  /**
   * called after a compress operation to write empty the new objects buffer and to set blocks correctly
   */
  protected def updateAfterCompress(LBPSIs : Array[Long]) : Unit;
  /**
   * called after a prepare append operation to write empty the new objects buffer and to set blocks correctly
   */
  final protected def updateAfterPrepareAppend(chunkMap : HashMap[FieldDeclaration[_], ChunkInfo]) : Unit = {
    val newInstances = !newDynamicInstances.isEmpty
    val newPool = blockInfos.isEmpty
    val newField = fields.exists(_.noDataChunk)
    if (newPool || newInstances || newField) {

      //build block chunk
      val lcount = newDynamicInstances.size
      //@ note this is the index into the data array and NOT the written lbpo
      val lbpo = if (0 == lcount) 0L
      else newDynamicInstances.next.getSkillID - 1

      blockInfos += new BlockInfo(lbpo, lcount)

      //@note: if this does not hold for p; then it will not hold for p.subPools either!
      if (newInstances || !newPool) {
        //build field chunks
        for (f ← fields) {
          if (f.noDataChunk) {
            val c = new BulkChunkInfo(-1, -1, dynamicSize)
            f.addChunk(c)
            chunkMap.put(f, c)
          } else if (newInstances) {
            val c = new SimpleChunkInfo(-1, -1, lbpo, lcount)
            f.addChunk(c)
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

  /**
   * Read a single reference of this type.
   */
  final override def readSingleField(in : InStream) : T = getByID(in.v64)

  final override def toString = name
  final override def equals(obj : Any) = obj match {
    case TypeDefinitionName(n) ⇒ n == name
    case t : FieldType[_]      ⇒ t.typeID == typeID
    case _                     ⇒ false
  }
}

sealed class BasePool[T <: SkillType : Manifest](
  poolIndex : Long,
  name : String,
  knownFields : HashMap[String, (FieldType[_], HashSet[FieldRestriction[_]])])
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
      val r = (new SkillType.SubType(this, skillID)).asInstanceOf[T]
      data(i) = r
      staticData += r
      true
    }
  }

  /**
   * compress new instances into the data array and update skillIDs
   */
  def compress(LBPSIs : Array[Long]) {
    val d = new Array[T](dynamicSize.toInt)
    var p = 0;
    for (i ← allInTypeOrder) {
      d(p) = i;
      p += 1;
      i.setSkillID(p);
    }
    data = d
    updateAfterCompress(LBPSIs)
  }
  final override def updateAfterCompress(LBPSIs : Array[Long]) {
    blockInfos.clear
    blockInfos += BlockInfo(0, data.length)
    newObjects.clear
    for (p ← subPools)
      p.updateAfterCompress(LBPSIs)
  }

  /**
   * prepare an append operation by moving all new instances into the data array.
   * @param chunkMap field data that has to be written to the file is appended here
   */
  def prepareAppend(chunkMap : HashMap[FieldDeclaration[_], ChunkInfo]) {
    val newInstances = !newDynamicInstances.isEmpty

    // check if we have to append at all
    if (!fields.exists(_.noDataChunk) && !newInstances)
      if (!fields.isEmpty && !blockInfos.isEmpty)
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
  override def getByID(index : Long) : T = (if (0 == index) null else data(index.toInt - 1)).asInstanceOf[T]

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

sealed class SubPool[T <: B : Manifest, B <: SkillType](
  poolIndex : Long,
  name : String,
  knownFields : HashMap[String, (FieldType[_], HashSet[FieldRestriction[_]])],
  superPool : StoragePool[_ <: B, B])
    extends StoragePool[T, B](poolIndex, name, knownFields, Some(superPool)) {

  override val basePool = superPool.basePool

  override def insertInstance(skillID : Long) = {
    val i = skillID.toInt - 1
    if (null != data(i))
      false
    else {
      val r = (new SkillType.SubType(this, skillID)).asInstanceOf[T]
      data(i) = r
      staticData += r
      true
    }
  }

  /**
   * the base type data store
   * @note that type checks are deferred to the wrapped array, because it is assumed that they wont ever fail and the
   * jvm will realize that fact
   */
  private[internal] def data = WrappedArray.make[T](basePool.data)

  override def all : Iterator[T] = blockInfos.foldRight(newDynamicInstances) { (block, iter) ⇒ basePool.data.view(block.bpo.toInt, (block.bpo + block.count).toInt).asInstanceOf[Iterable[T]].iterator ++ iter }
  override def iterator : Iterator[T] = blockInfos.foldRight(newDynamicInstances) { (block, iter) ⇒ basePool.data.view(block.bpo.toInt, (block.bpo + block.count).toInt).asInstanceOf[Iterable[T]].iterator ++ iter }

  override def allInTypeOrder : Iterator[T] = subPools.foldLeft(staticInstances)(_ ++ _.staticInstances)

  override def getByID(index : Long) : T = (if (0 == index) null else basePool.data(index.toInt - 1)).asInstanceOf[T]

  final override def updateAfterCompress(LBPSIs : Array[Long]) {
    blockInfos.clear
    blockInfos += BlockInfo(LBPSIs(poolIndex.toInt), dynamicSize)
    newObjects.clear
    for (p ← subPools)
      p.updateAfterCompress(LBPSIs)
  }
}
