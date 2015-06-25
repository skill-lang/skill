/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import scala.collection.JavaConversions._
import scala.annotation.tailrec
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.Field
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
import scala.annotation.tailrec

import ${packagePrefix}api._
import ${packagePrefix}internal.LongConverter._

/**
 * @note representation of the type system relies on invariants and heavy abuse of type erasure
 *
 * @author Timm Felden
 */
sealed abstract class FieldType(val typeID : Int) {
  def toString() : String

  override def equals(obj : Any) = obj match {
    case o : FieldType ⇒ o.typeID == typeID
    case _             ⇒ false
  }
  override def hashCode = typeID.toInt
}

sealed abstract class ConstantInteger[T](typeID : Int) extends FieldType(typeID) {
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

sealed abstract class IntegerType(typeID : Int) extends FieldType(typeID);

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

sealed abstract class CompoundType(typeID : Int) extends FieldType(typeID);

case class ConstantLengthArray(val length : Int, val groundType : FieldType) extends CompoundType(15) {
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

case class TypeDefinitionIndex(index : Int) extends FieldType(32 + index) {
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
 * 
 * Contained objects are identified by their skillID. It is positive
 * for read or written data and else negative. The value 0 is reserved
 * for null-references.
 */
sealed abstract class StoragePool[T <: B : ClassTag, B <: SkillType](
  private[internal] val poolIndex : Int,
  val name : String,
  private[internal] val knownFields : HashMap[String, FieldType],
  private[internal] val superPool : Option[StoragePool[_ <: B, B]])
    extends FieldType(32 + poolIndex)
    with Access[T] {

  // pools
  val superName = superPool.map(_.name)

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
  def makeSubPool(poolIndex : Int, name : String) : SubPool[_ <: T, B] = new SubPool[T, B](poolIndex, name, knownFields, this)

  /**
   * iterates over all transitive subpools of this pool, including this pool, in pre-order
   */
  private[internal] final def allPools : Iterator[StoragePool[_, B]] = subPools.foldLeft(Iterator.single[StoragePool[_, B]](this))(_ ++ _.allPools)
  
  private[internal] def state : SerializableState

  // fields
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
      putFieldMap(f, new HashMap[Int, Any])
    }

    fields += f
  }
  final protected def putFieldMap(f : FieldDeclaration, m : HashMap[Int, Any]) {
    unknownFieldData.put(f, m)
    subPools.foreach(_.putFieldMap(f, m))
  }

  final def allFields = fields.iterator

  /**
   * this map is used to store deserialized field data for each field and instance
   */
  private[internal] val unknownFieldData = new HashMap[FieldDeclaration, HashMap[Int, Any]];


  // id mapping (new objects only)
  /**
   * returns the local index for the given skillID which points to a new object.
   * 
   * skillID is asserted to be lower than 0
   */
  @inline private[internal] final def indexOfNewID(skillID : Int) : Int = basePool.newIndexInfo(-skillID - 1)
  
  // instances by id (reference resolving)
  /**
   * returns instances directly from the data store
   */
  final def getByID(skillID : Int) =
    if (0 == skillID || basePool.isIDRemoved(skillID))
      null.asInstanceOf[T]
    else if (skillID < 0)
      basePool.resolveNew(skillID).asInstanceOf[T]
    else
      getOldByID(skillID)
  /**
   * lookup function for creating proxies for old data
   */
  protected final def getOldByID(skillID : Int) : T =
    if (isStatic(skillID))
      createProxy(skillID)
    else
      subPools.find(sub ⇒ sub.isDynamic(skillID)) match {
        case Some(pool) ⇒ pool.getOldByID(skillID).asInstanceOf[T]
        case _ ⇒ null.asInstanceOf[T]
      }
  
  /**
   * checks if the type of an old skill object is exactly the type of this storage pool
   */
  @inline private final def isStatic(skillID : Int) : Boolean = {
    val index = skillID - 1
    for (bi ← blockInfos)
      if (bi.bpsi <= index && index < bi.bpsi + bi.count)
        return true
    false
  }
  /**
   * checks if the type of an old skill object is the type of this storage pool or any subpool
   */
  @inline protected final def isDynamic(skillID : Int) : Boolean = {
    val index = skillID - 1
    for (bi ← dynamicBlockInfos)
      if (bi.bpsi <= index && index < bi.bpsi + bi.count)
        return true
    false
  }
    
  // administration of old (written/read) data
  /**
   * contains the blocks of old data with exactly this type.
   * blockInfos is assumed to be small.
   */
  private[internal] var blockInfos = LongArrayBuffer[BlockInfo](16)
  /**
   * returns the blocks of old data with this type or any subtype.
   */
  private[internal] def dynamicBlockInfos : Iterator[BlockInfo]
  /**
   * adds a new block info
   */
  private[internal] def addBlockInfo(lbpsi : Int, count : Int) = blockInfos += BlockInfo(lbpsi, count)
  /**
   * updates the count of the last block info to the static instance count in this block.
   */
  private[internal] final def updateBlockInfo {
    var min = blockInfos.last.count
    val blockStart = blockInfos.last.bpsi
    for (sub ← subPools if sub.blockInfos.length > 0; subStart = sub.blockInfos.last.bpsi)
      if (subStart > blockStart)
      min = Math.min(min, subStart - blockStart)
    blockInfos.last = BlockInfo(blockStart, min)
  }
  /**
   * updates the dynamic count of the last block. Does nothing in generic storage pools.
   */
  protected def updateDynamicBlockInfo { }

  /**
   * counts all old objects with exactly the type of this pool (including deleted)
   */
  final def oldStaticSize = blockInfos.iterator.foldLeft(0)(_ + _.count)
  /**
   * counts all old objects inside this pool or any subpool (including deleted)
   */
  def oldDynamicSize : Int
  
  /**
   * resizes all data arrays. Does nothing in a generic storage pool. Used while reading a state from file
   */
  private[internal] def resizeData(increment : Int) { }
  
  // administration of new (just created) data
  /**
   * the local pool index. Used for creation of remapping tables.
   */
  private[internal] def localPoolIndex : Int
  /**
   * the local count of new static instances.
   */
  protected var localDataLength = 0
  
  /**
   * counts all new objects with exactly the type of this pool (including deleted)
   */
  private[internal] final def newStaticSize = localDataLength
  /**
   * counts all new objects inside this pool or any subpool (including deleted)
   * 
   * @note overridden by BasePool with a O(1)-operation
   */
  private[internal] def newDynamicSize : Int = subPools.foldLeft(newStaticSize)(_ + _.newDynamicSize)
  /**
   * iterates over all new objects with exactly the type of this pool (including deleted)
   */
  protected final def newStaticInstances =
    for ((pool, index) ← basePool.newPoolInfo.iterator.zipWithIndex if pool == this)
      yield createProxy(-index - 1)
  /**
   * iterates over all new objects inside this pool or any subpool (including deleted)
   */
  protected final def newDynamicInstances : Iterator[T] =
    subPools.foldLeft(newStaticInstances)(_ ++ _.newDynamicInstances)
  
  /**
   * resizes all data arrays for new objects. Does nothing in generic storage pools. Only here
   * to reserve name. Implemented by storage pools of known type
   */
  protected def resizeNewData(increase : Int) { }
  /**
   * clears all data and administration arrays for new objects. Overriders have to call
   * this implementation.
   */
  protected def clearNew {
    // reset local size of new data fields to 0
    localDataLength = 0
  }

  // administration helpers
  /**
   * resizes the given array
   */
  protected final def resizeArray[T : ClassTag](data : Array[T], increase : Int) = {
    val res = new Array[T](data.length + increase)
    data.copyToArray(res)
    res
  }
  /**
   * resizes the given array
   */
  protected final def resizeArray(data : BooleanArray, increase : Int) = {
    val res = new BooleanArray(data.length + increase)
    data.copyToArray(res)
    res
  }
  
  // iteration helpers and proxy creation
  /**
   * creates a proxy object of the type of this storage pool
   */
  private[internal] def createProxy(skillID : Int) : T

  /**
   * counts all objects with exactly the type of this pool (including deleted)
   */
  final def staticSize = blockInfos.iterator.foldLeft(newStaticSize)(_ + _.count)
  /**
   * iterates over all objects with exactly the type of this pool (including deleted)
   */
  private final def staticInstances : Iterator[T] =
    (for (bi ← blockInfos.iterator; id ← bi.bpsi + 1 to bi.bpsi + bi.count)
      yield createProxy(id)) ++ newStaticInstances
  /**
   * iterates over all objects with exactly the type of this pool (excluding deleted)
   */
  final def validStaticInstances = staticInstances.filter(x ⇒ !basePool.isIDRemoved(x.getSkillID))
  
  /**
   * counts all objects inside this pool or any subpool (including deleted)
   */
  def dynamicSize : Int
  /**
   * counts all objects inside this pool or any subpool (including deleted)
   */
  final override def size = dynamicSize
  
  // write and append helpers
  // usual order of actions:
  // write:
  // 1. build remapping tables for objects in type order (only base pools)
  // 2a. compress all objects into only one block     (method compress)
  // 2b. remap references in known and unknown fields (method compress)
  // 3. write to file (with StateWriter)
  // append:
  // 1. build remapping tables for new objects (only base pools)
  // 2a. append new objects and fields                (method appendNew)
  // 2b. remap references in known and unknown fields (method appendNew)
  // 3. write to file (with StateAppender)
  /**
   * compresses all objects of this pool and all subpools into one large
   * block and remaps references with the given remap tables. New data
   * is cleared. start is the new bpsi of this pool. deletedCounts contains
   * for each pool the instances deleted before this pool.
   */
  protected final def doCompress(maps : Array[(LongArrayBuffer[RemappingInfo], Array[Int])], start : Int, deletedCounts : Array[Int]) : Int = {
    var next = updateData(maps(poolIndex), start, deletedCounts)
    for (sub ← subPools)
      next = sub.doCompress(maps, next, deletedCounts)
    // clear base pool data last to allow usage of newPoolInfo in updateData
    clearNew
    updateDynamicBlockInfo
    fullRemap(maps)
    
    next
  }
  /**
   * creates new data arrays which contain all valid data in type order and
   * builds a new block info. start is the new bpsi of this pool, the return
   * value is the bpsi of the next pool. deletedCounts contains for each
   * pool the instances deleted before this pool.
   */
  protected def updateData(map : (LongArrayBuffer[RemappingInfo], Array[Int]), start : Int, deletedCounts : Array[Int]) : Int
  /**
   * remaps all references in fields to the new skillIDs of the objects.
   * Overriders have to call this implementation.
   */
  protected def fullRemap(maps : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) {
    //TODO: update references in unknown field data
  }
  
  /**
   * appends all new objects and fields to the old data and creates a new
   * block info for them. New data is cleared. Assumes that the base pool
   * is not dirty (that is, no old object is deleted).
   */
  protected final def doAppendNew(maps : Array[Array[Int]], chunkMap : HashMap[FieldDeclaration, ChunkInfo], start : Int, sizes : Array[Int]) : Int = {
    @inline def lastPoolIndex(pool : StoragePool[_, B]) : Int = {
      if (pool.subPools.isEmpty) pool.localPoolIndex else lastPoolIndex(pool.subPools.last)
    }
    @inline def dynamicCount = {
      var sum = 0
      for (i ← localPoolIndex to lastPoolIndex(this))
        sum += sizes(i)
      sum
    }
    val dcount = dynamicCount
    var next = updateData(maps(poolIndex), start, dcount, sizes(localPoolIndex))
    val newInstances = dynamicCount != 0
    val newPool = blockInfos.isEmpty
    val newField = fields.exists(_.dataChunks.isEmpty)
    if (newPool || newInstances || newField) {
      blockInfos += BlockInfo(start, sizes(localPoolIndex))

      //@note: if this does not hold for p; then it will not hold for p.subPools either!
      if (newInstances || !newPool) {
        //build field chunks
        for (f ← fields) {
          if (f.dataChunks.isEmpty) {
            val c = new BulkChunkInfo(-1, -1, oldDynamicSize + dcount)
            f.dataChunks += c
            chunkMap.put(f, c)
          } else if (newInstances) {
            val c = new SimpleChunkInfo(-1, -1, start, dcount)
            f.dataChunks += c
            chunkMap.put(f, c)
          }
        }
      }
    }
    for (sub ← subPools)
      next = sub.doAppendNew(maps, chunkMap, next, sizes)
    clearNew
    updateDynamicBlockInfo
    newRemap(maps)

    next
  }
  /**
   * appends the data of new objects to the old data arrays and builds a new
   * block info for the new data
   */
  protected def updateData(map : Array[Int], start : Int, dcount : Int, scount : Int) : Int
  /**
   * remaps all references to new objects in fields to the new skillIDs of
   * the objects. Overriders have to call this implementation.
   */
  protected def newRemap(maps : Array[Array[Int]]) {
    //TODO: update references in unknown field data
  }

  // common methods
  final override def toString = name
  final override def equals(obj : Any) = obj match {
    case TypeDefinitionName(n) ⇒ n == name
    case t : FieldType         ⇒ t.typeID == typeID
    case _                     ⇒ false
  }
}

sealed class BasePool[T <: SkillType : ClassTag](poolIndex : Int, name : String, knownFields : HashMap[String, FieldType])
    extends StoragePool[T, T](poolIndex, name, knownFields, None) {

  // pools
  /**
   * We are the base pool.
   */
  override val basePool = this

  /**
   * The state that contains us (set by state constructor)
   */
  private[internal] var state : SerializableState = null
  /**
   * public version of the state
   */
  def skillState : SkillState = state
  
  // instances by id (reference resolving) infrastructure
  /**
   * creates a proxy for a new object. Assumes that it is an object of this pool or any subpool.
   */
  @inline private[internal] final def resolveNew(skillID : Int) =
    newPoolInfo(-skillID - 1).createProxy(skillID).asInstanceOf[T]
  
  // deleting instances and testing if instance is deleted
  /**
   * set if the pool contains deleted old instances. A state with dirty pools cannot be appended.
   */
  private[internal] var dirty = false
  /**
   * removes the given instance from the pool
   */
  @inline final def removeByID(skillID : Int) =
    if (0 < skillID) {
      deleted(skillID - 1) = true
      dirty = true
    }
    else if (0 > skillID)
      newDeleted(-skillID - 1) = true
  /**
   * returns if the given instance is deleted
   */
  @inline final def isIDRemoved(skillID : Int) =
    if (0 == skillID)
      false
    else if (0 < skillID) {
      if (dirty) deleted(skillID - 1) else false
    }
    else
      newDeleted(-skillID - 1)
  
  /**
   * counts all deleted objects inside this base pool and all subpools
   */
  private[internal] final def deletedCount = (if (dirty) deleted.count(del ⇒ del) else 0) + newDeleted.count(del ⇒ del)
  /**
   * counts all deleted objects inside the given subpool and its subpools
   */
  private[internal] final def deletedCount(sub : SubPool[_, T]) = 
    if (dirty)
      (for (bi ← sub.dynamicBlockInfos) yield deleted.view(bi.bpsi, bi.count).count(del ⇒ del)).foldLeft(newDeletedCount(sub))(_ + _)
    else
      newDeletedCount(sub)
  /**
   * counts all deleted new objects inside the given subpool and its subpools assuming only a few objects were deleted.
   * 
   * complexity: O(d * t) where d = count of deleted new objects, t = number of subpools of sub
   */
  @inline private final def newDeletedCount(sub : SubPool[_, T]) = {
    val del = for (index ← 0 until newDataLength if newDeleted(index)) yield newPoolInfo(index)
    @inline def countSub(pool : SubPool[_, T]) : Int = pool.subPools.foldLeft(del.count(p ⇒ p == pool))(_ + countSub(_))
    countSub(sub)
  }
  
  // administration of old (written/read) data
  /**
   * returns the blocks of old data with this type or any subtype.
   */
  private[internal] override final def dynamicBlockInfos =
    for ((start, end) ← blockInfos.iterator.map(bi ⇒ bi.bpsi).zipAll(blockInfos.iterator.drop(1).map(bi ⇒ bi.bpsi), 0, oldDynamicSize))
      yield BlockInfo(start, end - start)
  
  /**
   * resizes all data arrays. Overriders have to call this implementation. Used while reading a state from file
   */
  private[internal] override def resizeData(increment : Int) = deleted = resizeArray(deleted, increment)
  /**
   * Deleted flags for old data. Length of this array is the count of all old objects inside this pool and all subpools.
   */
  protected var deleted = new BooleanArray(0)
  
  /**
   * counts all old objects inside this pool or any subpool (including deleted)
   */
  final def oldDynamicSize = deleted.length
  
  // administration of new (just created) data
  /**
   * used length of the data arrays for new objects
   */
  protected var newDataLength = 0
  /**
   * Deleted flags for new data. Used part is given by newDataLength
   */
  private var _newDeleted = new BooleanArray(0)
  /**
   * used to iterate over new deleted flags
   */
  protected final def newDeleted = _newDeleted.view(0, newDataLength)

  /**
   * infos about the real (sub)pool of new instances. This array contains references to subpools.
   * Used part is given by newDataLength
   */
  private var _newPoolInfo = Array[StoragePool[_ <: T, T]]()
  /**
   * used to access new pool infos
   */
  private[internal] final def newPoolInfo = _newPoolInfo.view(0, newDataLength)
  /**
   * infos about the local instance index of new instances. This array is parallel to _newPoolInfo.
   * Used part is given by newDataLength
   */
  private var _newIndexInfo = Array[Int]()
  /**
   * used to access new index infos
   */
  private[internal] final def newIndexInfo = _newIndexInfo.view(0, newDataLength)
  
  /**
   * the local pool index for a base pool is always 0
   */
  private[internal] final def localPoolIndex = 0
  
  /**
   * updates the local pool indices of all transitive subpools in depth-first order.
   * Called by state constructor.
   */
  private[internal] final def updateLocalIndices = {
    var index = 1
    for (sub ← subPools)
      index = sub.updateLocalIndices(index)
  }
  
  /**
   * counts all new objects inside this pool or any subpool (including deleted)
   */
  private[internal] final override def newDynamicSize = newDataLength
  
  /**
   * resizes all administration arrays for new objects.
   */
  private final def resizeNew(increase : Int) {
      _newDeleted = resizeArray(_newDeleted, increase)
      _newPoolInfo = resizeArray(_newPoolInfo, increase)
      _newIndexInfo = resizeArray(_newIndexInfo, increase)
  }
  /**
   * clears all data arrays for new objects. Overriders have to call this implementation
   */
  protected override def clearNew {
    super.clearNew
    _newDeleted = new BooleanArray(0)
    _newPoolInfo = Array[StoragePool[_ <: T, T]]()
    _newIndexInfo = Array[Int]()
    newDataLength = 0
  }
  
  /**
   * adds a new instance to this base pool. Called by concrete storage pool classes.
   * 
   * @note concrete storage pool classes have an addInstance method which is used as constructor.
   * 
   * @param pool the real pool containing the data
   * @param localIndex the next free index in pool
   */
  private[internal] final def addPoolInstance(pool : StoragePool[_ <: T, T], localIndex : Int) = {
    if (_newDeleted.length == newDataLength)
      resizeNew(Math.max(128, _newDeleted.length)) // @note: constant can be changed if needed
    _newDeleted(newDataLength) = false
    _newPoolInfo(newDataLength) = pool
    _newIndexInfo(newDataLength) = localIndex
    newDataLength += 1
    -newDataLength // next skillID
  }
  
  // iteration and proxy creation
  /**
   * creates a proxy object of the type of this storage pool
   */
  private[internal] def createProxy(skillID : Int) : T = new UnknownSkillType(skillID, this).asInstanceOf[T]

  /**
   * counts all objects inside this pool or any subpool (including deleted)
   */
  final override def dynamicSize = deleted.length + newDataLength
  
  /**
   * iterates over all valid instances and applies the function f to each
   */
  final override def foreach[U](f : T ⇒ U) {
    for (proxy ← all)
      f(proxy)
  }
  
  /**
   * returns an iterator over all valid instances in this pool and all subpools
   */
  final override def all : Iterator[T] =
    (if (dirty)
      (for (id ← (1 to deleted.length).iterator if !isIDRemoved(id)) yield getOldByID(id))
    else
      (for (id ← (1 to deleted.length).iterator) yield getOldByID(id))
    ) ++ newDynamicInstances.filter(x ⇒ !isIDRemoved(x.getSkillID))
  /**
   * returns an iterator over all valid instances in this pool and all subpools
   */
  final override def iterator : Iterator[T] = all

  /**
   * returns an iterator over all valid instances in this pool and all subpools, ordered by type
   */
  final override def allInTypeOrder : Iterator[T] = subPools.foldLeft(validStaticInstances)(_ ++ _.allInTypeOrder)

  /**
   * copies all valid instances inside this pool to an array. Use with care, because this can cause OutOfMemoryError
   */
  final override def toArray[B >: T : ClassTag] = {
    val r = new Array[B](size - deletedCount)
    all.copyToArray(r)
    r
  }
  
  // write and append helpers
  /**
   * counts all deleted old objects inside the given subpool. Remapping helper
   */
  private[internal] final def oldDeletedCount(pool : StoragePool[_, T]) = if (dirty) 0 else {
    (for (bi ← pool.blockInfos.iterator) yield deleted.view(bi.bpsi, bi.count).count(del ⇒ del)).foldLeft(0)(_ + _)
  }
  /**
   * creates an array which contains the counts of deleted new instances for each (sub)pool.
   * This is a helper for remapping to reduce complexity to O(n) instead of O(n²). Each entry
   * in the result array contains the deleted count that belongs to the *previous* pool.
   * 
   * That means that the first entry is always 0. The last entry is unused, it contains the
   * deleted count of the last pool.
   */
  private final def newDeletedByPool = {
    // helpers for remapping. Complexity is reduced to O(n) with n number of new objects
    // calculates the needed size for the result array (count of subpools + 2)
    @inline def calculateResultSize(pool : StoragePool[_, T]) : Int = {
      if (pool.subPools.isEmpty) pool.localPoolIndex + 2 else calculateResultSize(pool.subPools.last)
    }
    // fills an array with the counts of all deleted instances of a specific pool
    @inline def fillDeletedCounts(data : Array[Int]) {
      for (index ← (0 until newDataLength).view if newDeleted(index); pool = newPoolInfo(index))
        data(pool.localPoolIndex + 1) -= 1
    }
    val result = new Array[Int](calculateResultSize(this))
    fillDeletedCounts(result)
    result
  }
  /**
   * prepares the remap tables for remapping of references. Entries are skillIDs.
   * All objects are remapped to be in type order.
   */
  def prepareFullRemap = {
    // first id of next block in each (sub)pool
    val currentIDs = newDeletedByPool
    // first skillID is 1
    currentIDs(0) = 1
    for (pool ← allPools)
      currentIDs(pool.localPoolIndex + 1) += pool.staticSize - oldDeletedCount(pool)
    // accumulate
    for (i ← 1 until currentIDs.length)
      currentIDs(i) += currentIDs(i - 1)
     
    // old data: map ids to ids
    var oldMap = LongArrayBuffer[RemappingInfo](16)
    if (dirty) // complex case: deleted objects exist ⇒ all objects have to be visited
      for (pool ← allPools; bi ← pool.blockInfos.iterator if bi.count > 0) {
        var prevDel = deleted(bi.bpsi)
        var id = currentIDs(pool.localPoolIndex)
        oldMap += { if (prevDel) RemappingInfo(bi.bpsi + 1) else RemappingInfo(bi.bpsi + 1, id - bi.bpsi - 1) }
        for (i ← (bi.bpsi + 1 until bi.count + bi.bpsi).view if prevDel != deleted(i)) {
          if (!prevDel) // count only undeleted
            id += i + 1 - oldMap.last.start
          prevDel = deleted(i)
          oldMap += { if (prevDel) RemappingInfo(i + 1) else RemappingInfo(i + 1, id - i - 1) }
        }
        if (!prevDel)
          id += bi.count + bi.bpsi + 1 - oldMap.last.start
        currentIDs(pool.localPoolIndex) = id
      }
    else // simple case: one entry per block per pool
      for (pool ← allPools; bi ← pool.blockInfos.iterator if bi.count > 0) {
        oldMap += RemappingInfo(bi.bpsi + 1, currentIDs(pool.localPoolIndex) - bi.bpsi - 1)
        currentIDs(pool.localPoolIndex) += bi.count
      }
    // sort array to make searching for ids possible
    oldMap.sort((l, r) ⇒ l.start < r.start)
    
    // newData: map indices to ids; use currentIDs as first new positions
    val newMap = new Array[Int](newPoolInfo.length)
    for (ref ← 0 until newIndexInfo.length)
      if (newDeleted(ref))
        currentIDs(newPoolInfo(ref).localPoolIndex) -= 1
      else
        newMap(ref) = newIndexInfo(ref) + currentIDs(newPoolInfo(ref).localPoolIndex)

    (oldMap, newMap)
  }
  /**
   * prepares the remap table for remapping of references to new objects. Entries are skillIDs.
   */
  def prepareNewRemap(firstID : Int) = {
    // preparation
    val minIDs = newDeletedByPool
    minIDs(0) = firstID
    for (pool ← allPools)
      minIDs(pool.localPoolIndex + 1) += pool.newStaticSize
    // accumulate to get real indices
    for (i ← 1 until minIDs.length)
      minIDs(i) += minIDs(i - 1)
    
    // add static minimum skillID (ignoring deleted objects) for each pool
    val newMap = new Array[Int](newPoolInfo.length)
    for (ref ← 0 until newIndexInfo.length)
      if (newDeleted(ref))
        minIDs(newPoolInfo(ref).localPoolIndex) -= 1
      else
        newMap(ref) = newIndexInfo(ref) + minIDs(newPoolInfo(ref).localPoolIndex)
    newMap
  }

  /**
   * compresses all objects of this pool and all subpools into one large
   * block and remaps references with the given remap tables. New data
   * is cleared.
   */
  @inline private[internal] final def compress(maps : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) {
    val deletedCounts = newDeletedByPool
    for (pool ← allPools)
      deletedCounts(pool.localPoolIndex + 1) -= oldDeletedCount(pool)
    // accumulate
    for (i ← 1 until deletedCounts.length)
      deletedCounts(i) += deletedCounts(i - 1)
    doCompress(maps, 0, deletedCounts)
  }
  /**
   * creates new data arrays which contain all valid data in type order and
   * builds a new block info. Overriders have to call this implementation.
   */
  protected def updateData(map : (LongArrayBuffer[RemappingInfo], Array[Int]), start : Int /* always 0 */, deletedCounts : Array[Int]) = {
    // deletedCounts contains at least two entries of which the last entry is the
    // negative count of all deleted instances and the second the negative count
    // of deleted base pool instances.
    val _staticSize = staticSize
    
    // clear all deleted flags and resize to new count
    deleted = new BooleanArray(dynamicSize + deletedCounts.last)
    // update block infos
    blockInfos /-= 16 // "clear" with initial capacity
    blockInfos += BlockInfo(0, _staticSize + deletedCounts(1))
    // reset dirty flag
    dirty = false
    blockInfos.last.count
  }

  /**
   * appends all new objects and fields of this pool and all subpools into a
   * new block and remaps references with the given remap tables. New data is
   * cleared. Checks that this pool is not dirty.
   */
  @inline private[internal] def appendNew(maps : Array[Array[Int]], chunkMap : HashMap[FieldDeclaration, ChunkInfo]) {
    if (dirty)
      throw new IllegalStateException("The storage pool is dirty, thus append is not possible!")
    val sizes = newDeletedByPool
    for ((i, pool) ← (0 until sizes.length - 1).iterator.zip(allPools))
      sizes(i) = sizes(i + 1) + pool.newStaticSize
    val newInstances = newDynamicSize - newDeleted.count(del ⇒ del) != 0
    if (!fields.exists(_.dataChunks.isEmpty) && !newInstances)
      if (!fields.isEmpty && !blockInfos.isEmpty)
        return ;
    doAppendNew(maps, chunkMap, oldDynamicSize, sizes)
  }

  /**
   * appends the data of new objects to the old data arrays and builds a new
   * block info for the new data
   */
  protected def updateData(map : Array[Int], start : Int, dcount : Int, scount : Int) = start + scount

  // update helpers
  /**
   * creates a new array that contains the mapped data of the field, given by an array of
   * old data and a function that maps subpools to local data arrays for new data.
   */
  @inline protected final def updateField[T : ClassTag](newSize : Int, data : Array[T], newData : (StoragePool[_, _]) ⇒ Array[T],
      map : (LongArrayBuffer[RemappingInfo], Array[Int])) = {
    val result = new Array[T](newSize)
    for ((ri, end) ← map._1.iterator.zipAll(map._1.iterator.drop(1).map(ri ⇒ ri.start - 1), RemappingInfo(0, 0), newSize) if !ri.isDeleted)
      data.view(ri.start - 1, end).copyToArray(result, ri.start + ri.offset - 1)
    for (((pool, oldI), newI) ← basePool.newPoolInfo.iterator.zip(basePool.newIndexInfo.iterator).zip(map._2.iterator) if newI != 0)
      result(newI - 1) = newData(pool)(oldI)
    result
  }
  /**
   * creates a new array that contains the mapped data of the field, given by an array of
   * old data and a function that maps subpools to local data arrays for new data.
   */
  @inline protected final def updateField[T : ClassTag](newSize : Int, data : Array[T], newData : (StoragePool[_, _]) ⇒ Array[T], map : Array[Int]) = {
    val result = new Array[T](newSize)
    data.copyToArray(result)
    for (((pool, oldI), newI) ← basePool.newPoolInfo.iterator.zip(basePool.newIndexInfo.iterator).zip(map.iterator) if newI != 0)
      result(newI - 1) = newData(pool)(oldI)
    result
  }
}

sealed class SubPool[T <: B : ClassTag, B <: SkillType](poolIndex : Int, name : String, knownFields : HashMap[String, FieldType], superPool : StoragePool[_ <: B, B])
    extends StoragePool[T, B](poolIndex, name, knownFields, Some(superPool)) {

  override val basePool = superPool.basePool

  private[internal] final override def state = basePool.state 
  
  // id mapping (id to local index)
  /**
   * returns the local index for the given skillID which points to an old object.
   * 
   * skillID is asserted to be greater than 0
   */
  private[internal] final def indexOfOldID(skillID : Int) : Int = {
    var start = 0
    for (i ← 0 until localBlocks.length;
         pos = blockInfos(i).bpsi;
         end = pos + localBlocks(i))
      if (pos < skillID && skillID <= end)
        return skillID - start - 1
      else
        start += localBlocks(i)
    -1 // should never be reached
  }

  // administration of old (written/read) data
  /**
   * local dynamic counts for blocks. Length is equal to blockInfos.length.
   */
  private final var _localBlocks = new Array[Int](16)
  /**
   * used to iterate over local blocks
   */
  private[internal] final def localBlocks = _localBlocks.view(0, blockInfos.length)
  /**
   * returns the blocks of old data with this type or any subtype.
   */
  private[internal] final override def dynamicBlockInfos =
    blockInfos.iterator.zip(localBlocks.iterator).map { case (bi, lb) ⇒ BlockInfo(bi.bpsi, lb) }
  /**
   * adds a new block info
   */
  private[internal] final override def addBlockInfo(lbpsi : Int, count : Int) = {
    if (_localBlocks.length == blockInfos.length)
      _localBlocks = resizeArray(_localBlocks, 128)
    _localBlocks(blockInfos.length) = count
    super.addBlockInfo(lbpsi, count)
  }
  /**
   * updates the dynamic count of the last block. Assumes that the dynamic block infos
   * for subpools are already set.
   */
  protected final override def updateDynamicBlockInfo {
    _localBlocks(blockInfos.length - 1) = subPools.foldLeft(blockInfos.last.count)(_ + _.localBlocks.last) 
  }
  
  /**
   * counts all old objects inside this pool or any subpool (including deleted)
   */
  final def oldDynamicSize = localBlocks.foldLeft(0)(_ + _)

  // administration of new (just created) data
  /**
   * local index of this subpool. Used only for building remapping tables for
   * new objects to make it an O(n) operation.
   */
  private[internal] final var localPoolIndex = 0
  /**
   * returns the maximal localPoolIndex that a subpool of this pool has
   */
  @inline protected final def maxPoolIndex = {
    @inline def maxSubPool(pool : SubPool[_, B]) : Int =
      if (pool.subPools.isEmpty) pool.localPoolIndex else maxSubPool(pool.subPools.last)
    maxSubPool(this)
  }
  /**
   * sets the local pool index of this pool and all transitive subpools.
   */
  private[internal] final def updateLocalIndices(index : Int) : Int = {
    localPoolIndex = index
    var i = index + 1
    for (sub ← subPools)
      i = sub.updateLocalIndices(i)
    i
  }

  // iteration and proxy creation
  /**
   * creates a proxy object of the type of this storage pool
   */
  private[internal] def createProxy(skillID : Int) : T = new UnknownSkillType(skillID, this).asInstanceOf[T]

  /**
   * counts all objects inside this pool or any subpool (including deleted)
   */
  final override def dynamicSize = localBlocks.foldLeft(newDynamicSize)(_ + _)
  
  /**
   * iterates over all valid instances and applies the function f to each
   */
  final override def foreach[U](f : T ⇒ U) {
    for (proxy ← all)
      f(proxy)
  }
  
  /**
   * returns an iterator over all valid instances in this pool and all subpools
   */
  final override def all : Iterator[T] = dynamicBlockInfos.foldRight(newDynamicInstances.filter(x ⇒ !basePool.isIDRemoved(x.getSkillID)))(
      (bi, it) ⇒ (for (id ← (bi.bpsi + 1 to bi.bpsi + bi.count).iterator if !basePool.isIDRemoved(id)) yield getOldByID(id)) ++ it)
  /**
   * returns an iterator over all valid instances in this pool and all subpools
   */
  final override def iterator : Iterator[T] = all

  /**
   * returns an iterator over all valid instances in this pool and all subpools, ordered by type
   */
  final override def allInTypeOrder : Iterator[T] = subPools.foldLeft(validStaticInstances)(_ ++ _.allInTypeOrder)

  /**
   * copies all valid instances inside this pool to an array. Use with care, because this can cause OutOfMemoryError
   */
  final override def toArray[B >: T : ClassTag] = {
    val r = new Array[B](size - basePool.deletedCount(this))
    all.copyToArray(r)
    r
  }
  
  // write and append helpers
  /**
   * builds a new block info only. Override this to compress data fields.
   * Overriders have to call this implementation. start is the bpsi of this pool,
   * the return value is the bpsi of the next pool.
   */
  protected def updateData(map : (LongArrayBuffer[RemappingInfo], Array[Int]), start : Int, deletedCounts : Array[Int]) = {

    // deletedCounts allows easy calculation of the new static and dynamic count.
    // It is assumed that all subpools are updated after this pool (what is ensured
    // by the implementation of compress).
    val _staticSize = staticSize + deletedCounts(localPoolIndex + 1) - deletedCounts(localPoolIndex)
    val _dynamicSize = dynamicSize + deletedCounts(maxPoolIndex + 1) - deletedCounts(localPoolIndex)

    blockInfos /-= 16 // "clear" with initial capacity
    blockInfos += BlockInfo(start, _staticSize)
    // clear local blocks
    _localBlocks = new Array[Int](16)
    _localBlocks(0) = _dynamicSize
    start + _staticSize
  }

  /**
   * appends the data of new objects to the old data arrays and builds a new
   * block info for the new data
   */
  protected def updateData(map : Array[Int], start : Int, dcount : Int, scount : Int) = start + scount

  // update helpers
  /**
   * creates update tables from remapping tables.
   */
  protected final def prepareUpdate(map : (LongArrayBuffer[RemappingInfo], Array[Int])) = {
    val iter = dynamicBlockInfos
    var bi = if(iter.hasNext) iter.next() else BlockInfo(basePool.dynamicSize, 0)
    var oldMap = LongArrayBuffer[RemappingInfo](16)
    var first = -1
    var blockFirst = 0
    for (ri ← map._1) {
      while (bi.bpsi + bi.count < ri.start) {
        blockFirst += bi.count
        bi = if(iter.hasNext) iter.next() else BlockInfo(basePool.dynamicSize, 0)
      }
      if (ri.start > bi.bpsi) {
        if (first < 0)
          first = ri.start + ri.offset - 1
        oldMap += (if (ri.isDeleted) RemappingInfo(ri.start - bi.bpsi + blockFirst - 1)
                   else RemappingInfo(ri.start - bi.bpsi + blockFirst - 1, ri.offset + bi.bpsi - blockFirst - first))
      }
    }
    if (first < 0)
      first = {
        var min = basePool.dynamicSize
        for ((poolI, newID) ← basePool.newPoolInfo.iterator.map(_.localPoolIndex).zip(map._2.iterator)
            if poolI >= localPoolIndex && poolI <= maxPoolIndex)
          min = Math.min(min, newID)
        min
      }
    else
      first += 1
    val newMap = new Array[Array[Int]](maxPoolIndex - localPoolIndex + 1)
    for (pool ← allPools)
      newMap(pool.localPoolIndex - localPoolIndex) = new Array[Int](pool.newStaticSize)
    for (((poolI, index), newID) ← basePool.newPoolInfo.iterator.map(_.localPoolIndex).zip(basePool.newIndexInfo.iterator).zip(map._2.iterator)
        if poolI >= localPoolIndex && poolI <= maxPoolIndex)
      newMap(poolI - localPoolIndex)(index) = if (newID == 0) -1 else newID - first
    (oldMap, newMap)
  }
  /**
   * creates an update table for new data from a remapping table for new data.
   */
  protected def prepareNewUpdate(map : Array[Int]) = {
    val first = {
        var min = basePool.dynamicSize
        for ((poolI, newID) ← basePool.newPoolInfo.iterator.map(_.localPoolIndex).zip(map.iterator)
            if poolI >= localPoolIndex && poolI <= maxPoolIndex)
          min = Math.min(min, newID)
        min - oldDynamicSize
      }
    val newMap = new Array[Array[Int]](maxPoolIndex - localPoolIndex + 1)
    for (pool ← allPools)
      newMap(pool.localPoolIndex - localPoolIndex) = new Array[Int](pool.newStaticSize)
    for (((poolI, index), newID) ← basePool.newPoolInfo.iterator.map(_.localPoolIndex).zip(basePool.newIndexInfo.iterator).zip(map.iterator)
        if poolI >= localPoolIndex && poolI <= maxPoolIndex)
      newMap(poolI - localPoolIndex)(index) = if (newID == 0) -1 else newID - first
    newMap
  }
  /**
   * creates a new array that contains the mapped data of the field, given by an array of
   * old data and a function that maps subpools to local data arrays for new data.
   */
  @inline protected final def updateField[T : ClassTag](newSize : Int, data : Array[T], newData : (StoragePool[_, _]) ⇒ Array[T],
      map : (LongArrayBuffer[RemappingInfo], Array[Array[Int]])) = {
    val result = new Array[T](newSize)
    for ((ri, end) ← map._1.iterator.zipAll(map._1.iterator.drop(1).map(_.start), RemappingInfo(0, 0), newSize) if !ri.isDeleted)
      data.view(ri.start, end).copyToArray(result, ri.start + ri.offset)
    for (pool ← allPools; (newI, oldI) ← map._2(pool.localPoolIndex - localPoolIndex).iterator.zipWithIndex if newI != -1)
      result(newI) = newData(pool)(oldI)
    result
  }
  /**
   * creates a new array that contains the mapped data of the field, given by an array of
   * old data and a function that maps subpools to local data arrays for new data.
   */
  @inline protected final def updateField[T : ClassTag](newSize : Int, data : Array[T], newData : (StoragePool[_, _]) ⇒ Array[T],
      map : Array[Array[Int]]) = {
    val result = new Array[T](newSize)
    data.copyToArray(result)
    for (pool ← allPools; (newI, oldI) ← map(pool.localPoolIndex - localPoolIndex).iterator.zipWithIndex if newI != -1)
      result(newI) = newData(pool)(oldI)
    result
  }
}
""")

    for (t ← IR) {
      val typeName = "_root_."+packagePrefix + t.getCapitalName
      val isSingleton = !t.getRestrictions.collect { case r : SingletonRestriction ⇒ r }.isEmpty
      val relevantFields = t.getFields.filterNot(f ⇒ f.isConstant() || f.isIgnored())
      val newFields = t.getAllFields.filterNot(f ⇒ f.isConstant() || f.isIgnored())

      out.write(s"""
sealed trait ${t.getCapitalName}PoolFields ${
        if (t.getSuperType == null) ""
        else s"extends ${t.getSuperType.getCapitalName}PoolFields"
      } {
${
        (for (f ← relevantFields)
          yield s"  private[internal] var _new${f.getName.capitalize} : Array[${mapTypeRepresentation(f.getType)}]"
        ).mkString("\n")
      }
}

final class ${t.getCapitalName}StoragePool(poolIndex : Int${
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
    with ${t.getCapitalName}Access
    with ${t.getCapitalName}PoolFields {

  // pools
  /**
   * adds a sub pool of the correct type; required for unknown generic sub types
   */
  override def makeSubPool(poolIndex : Int, name : String) = ${
        if (isSingleton) s"""throw new SkillException("${t.getCapitalName} is a Singleton and can therefore not have any subtypes.")"""
        else s"new ${t.getCapitalName}SubPool(poolIndex, name, this)"
      }

  // administration of old (written/read) data${
        (for (f ← relevantFields)
          yield s"""
  /**
   * contains the data for the ${f.getName} field of old instances
   */
  private[internal] var _${f.getName} = Array[${mapTypeRepresentation(f.getType)}]()"""
        ).mkString("\n")
      }
  
  /**
   * resizes all data arrays. Used while reading a state from file
   */
  private[internal] override def resizeData(increment : Int) {
    super.resizeData(increment)
${
        (for (f ← relevantFields)
          yield s"    _${f.getName} = resizeArray(_${f.getName}, increment)"
        ).mkString("\n")

      }
  }
  
  // administration of new (just created) data${
        (for (f ← newFields)
          yield s"""
  /**
   * contains the data for the ${f.getName} field of new instances
   */
  private[internal] var _new${f.getName.capitalize} = Array[${mapTypeRepresentation(f.getType)}]()
  /**
   * used part of _new${f.getName.capitalize}
   */
  private[internal] def new${f.getName.capitalize} = _new${f.getName.capitalize}.view(0, localDataLength)"""
        ).mkString("\n")
      }
  
${
        if (!newFields.isEmpty)
          s"""  /**
   * resizes all data arrays for new objects.
   */
  protected override def resizeNewData(increase : Int) {
${
            (for (f ← newFields)
              yield s"    _new${f.getName.capitalize} = resizeArray(_new${f.getName.capitalize}, increase)"
            ).mkString("\n")
          }
  }

  /**
   * clears all data arrays for new objects.
   */
  protected override def clearNew {
    super.clearNew
${
            (for (f ← newFields)
              yield s"    _new${f.getName.capitalize} = Array[${mapTypeRepresentation(f.getType)}]()"
            ).mkString("\n")
          }
  }"""
      }

  /**
   * adds a new instance to this storage pool
   */
  private def addInstance(${if (isSingleton) "" else makeConstructorArguments(t)}) = {
${
        if (newFields.isEmpty) ""
        else if (isSingleton) "    resizeNewData(1)"
        else s"""    if (localDataLength == _new${newFields.head.getName.capitalize}.length)
      resizeNewData(Math.max(128, _new${newFields.head.getName.capitalize}.length / 2)) // @note: constant can be changed
${
            (for (f ← newFields) yield s"    _new${f.getName.capitalize}(localDataLength) = ${
              f.getType match {
                case t : Declaration ⇒ s"${escaped(f.getName)}.reference"
                case t : GroundType if t.getSkillName == "annotation" ⇒ s"${escaped(f.getName)}.annotation"
                case t : de.ust.skill.ir.SingleBaseTypeContainer ⇒ {
                  t.getBaseType match {
                    case t : GroundType if t.getSkillName != "annotation" ⇒ escaped(f.getName)
                    case t ⇒ s"${escaped(f.getName)}.data"
                  }
                }
                case t : MapType ⇒ s"${escaped{f.getName}}.getData"
                case _ ⇒ escaped(f.getName)
              }
            }").mkString("\n")
          }"""
      }
    localDataLength += 1
    basePool.addPoolInstance(this, localDataLength - 1)
  }
  
  // field access${
      val oldIndex = if (t.getSuperType != null) "indexOfOldID(skillID)" else "skillID - 1"
      (for (f ← relevantFields) yield s"""
  @inline def get${f.getName.capitalize}(skillID : Int) : ${mapType(f.getType)} = {
    ${makeGetter(f.getType, s"""if (0 < skillID)
      _${f.getName}($oldIndex)
    else {
      val index = -skillID - 1
      basePool.newPoolInfo(index).asInstanceOf[${t.getCapitalName}PoolFields]._new${f.getName.capitalize}(basePool.newIndexInfo(index))
    }""")}
  }
  @inline def set${f.getName.capitalize}(skillID : Int, ${f.getName} : ${mapType(f.getType)}) {
    if (0 < skillID)
      ${makeSetter(f.getType, s"_${f.getName}($oldIndex)", f.getName)}
    else { // set data in real storage pool, assuming it is this pool or a subpool!
      val index = -skillID - 1
      ${makeSetter(f.getType, s"basePool.newPoolInfo(index).asInstanceOf[${t.getCapitalName}PoolFields]._new${f.getName.capitalize}(basePool.newIndexInfo(index))", f.getName)}
    }
  }""").mkString("\n")
    }
  
  // proxy creation
  /**
   * creates a proxy object of the type of this storage pool
   */
  override def createProxy(skillID : Int) = ${if (isSingleton) "theInstance" else s"new $typeName(skillID, state)"}

${
        if (isSingleton)
          s"""  lazy val theInstance = if (oldStaticSize > 0 && !basePool.isIDRemoved(blockInfos.head.bpsi + 1))
    new $typeName(blockInfos.head.bpsi + 1, state)
  else
    new $typeName(addInstance(), state)

  override def get = theInstance"""
        else
          s"""  /**
   * adds a new instance to this pool and returns a proxy for that instance.
   */
  override def apply(${makeConstructorArguments(t)}) =
    new $typeName(addInstance(${makeConstructorArguments(t)}), state)"""
      }

  // write and append helpers
  /**
   * creates new data arrays which contain all valid data in type order and
   * builds a new block info.
   */
  protected override def updateData(map : (LongArrayBuffer[RemappingInfo], Array[Int]), start : Int, deletedCounts : Array[Int]) = {
    ${
        if (t.getSuperType == null) "val newSize = dynamicSize + deletedCounts.last"
        else "val newSize = dynamicSize + deletedCounts(maxPoolIndex + 1) - deletedCounts(localPoolIndex)\n    val updateMap = prepareUpdate(map)"
      }

    // update fields${
        val mapName = if (t.getSuperType == null) "map" else "updateMap"
        (for (f ← relevantFields) yield s"""
    _${f.getName} = updateField(newSize, _${f.getName}, (pool : StoragePool[_, _]) ⇒ pool match { case p : ${t.getCapitalName}PoolFields ⇒ p._new${f.getName.capitalize}; case _ ⇒ null }, $mapName)""").mkString("")
      }
    ${
        if (isSingleton) "\n    theInstance.setSkillID(start + 1)\n" else ""
      }
    // update block infos etc.
    super.updateData(map, start, deletedCounts)
  }
  /**
   * remaps all references in fields to the new skillIDs of the objects.
   */
  protected override def fullRemap(maps : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) {
    import RemappingFunctions._

    super.fullRemap(maps)${
      (for (f ← relevantFields) yield f.getType match {
        case t : GroundType ⇒ if (t.getSkillName == "annotation") s"\n    remapField(_${f.getName}, maps)" else ""
        case t : Declaration ⇒ s"\n    remapField(_${f.getName}, maps(state.${t.getCapitalName}.asInstanceOf[StoragePool[_, _]].poolIndex))"
        case t : ConstantLengthArrayType ⇒ makeListRemapping("Array", f.getName, t.getBaseType)
        case t : VariableLengthArrayType ⇒ makeListRemapping("ArrayBuffer", f.getName, t.getBaseType)
        case t : ListType ⇒ makeListRemapping("ListBuffer", f.getName, t.getBaseType)
        case t : SetType ⇒ makeListRemapping("HashSet", f.getName, t.getBaseType)
        case t : MapType ⇒ makeMapRemapping(f.getName, t.getBaseTypes)
        case _ ⇒ ""
      }).mkString("")
    }
  }

  /**
   * creates new data arrays which contain all valid data, first the old data
   * and then the new data
   */
  protected override def updateData(map : Array[Int], start : Int, dcount : Int, scount : Int) = {
    val newSize = oldDynamicSize + dcount
    ${
        if (t.getSuperType == null) ""
        else "val updateMap = prepareNewUpdate(map)"
      }

    // update fields${
        val mapName = if (t.getSuperType == null) "map" else "updateMap"
        (for (f ← relevantFields) yield s"""
    _${f.getName} = updateField(newSize, _${f.getName}, (pool : StoragePool[_, _]) ⇒ pool match { case p : ${t.getCapitalName}PoolFields ⇒ p._new${f.getName.capitalize}; case _ ⇒ null }, $mapName)""").mkString("")
      }
    ${
        if (isSingleton) "\n    theInstance.setSkillID(start + 1)\n" else ""
      }
    start + scount
  }
  /**
   * remaps all references to new objects in fields to the new skillIDs of the objects.
   */
  protected override def newRemap(maps : Array[Array[Int]]) {
    import RemappingFunctions._

    super.newRemap(maps)${
      (for (f ← relevantFields) yield f.getType match {
        case t : GroundType ⇒ if (t.getSkillName == "annotation") s"\n    remapField(_${f.getName}, maps)" else ""
        case t : Declaration ⇒ s"\n    remapField(_${f.getName}, maps(state.${t.getCapitalName}.asInstanceOf[StoragePool[_, _]].poolIndex))"
        case t : ConstantLengthArrayType ⇒ makeListRemapping("Array", f.getName, t.getBaseType)
        case t : VariableLengthArrayType ⇒ makeListRemapping("ArrayBuffer", f.getName, t.getBaseType)
        case t : ListType ⇒ makeListRemapping("ListBuffer", f.getName, t.getBaseType)
        case t : SetType ⇒ makeListRemapping("HashSet", f.getName, t.getBaseType)
        case t : MapType ⇒ makeMapRemapping(f.getName, t.getBaseTypes)
        case _ ⇒ ""
      }).mkString("")
    }
  }
}
""")

      if (!isSingleton) {
        // create a sub pool
        out.write(s"""
final class ${t.getCapitalName}SubPool(poolIndex : Int, name : String, superPool : StoragePool[_ <: $typeName, ${packagePrefix}${t.getBaseType.getCapitalName}])
    extends SubPool[$typeName.SubType, ${packagePrefix}${t.getBaseType.getCapitalName}](
      poolIndex,
      name,
      HashMap[String, FieldType](),
      superPool
    ) {

  override def makeSubPool(poolIndex : Int, name : String) = new ${t.getCapitalName}SubPool(poolIndex, name, this)
  override def createProxy(skillID : Int) = new $typeName.SubType(skillID, this)
}
""")
      }
    }

    out.write("""
class AnnotationRef(private val _skillID : Long) extends AnyVal {
  @inline def typeIndex = (_skillID >> 32).toInt
  @inline def skillID = _skillID.toInt
}
object AnnotationRef {
  @inline def build(typeIndex : Int, skillID : Int) = (typeIndex.toLong << 32) | (skillID.toLong & 0xFFFFFFFFL)
  @inline def unapply(ref : AnnotationRef) = Some(ref.typeIndex, ref.skillID)
}
""")
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
  
  private def makeGetter(t : Type, data : String) : String = t match {
    case t : GroundType ⇒ {
      if (t.getSkillName == "annotation")
        s"state.resolveAnnotation(new AnnotationRef($data))"
      else
        data
    }
    case t : Declaration ⇒ s"state.resolve${t.getCapitalName}($data)"
    case t : de.ust.skill.ir.SingleBaseTypeContainer ⇒ {
      t.getBaseType match {
        case u : GroundType if u.getSkillName != "annotation" ⇒ data
        case u ⇒ {
          s"new ${mapType(t)}($data, state.resolve${ if (u.getSkillName == "annotation") "AnnotationInternal _" else u.getCapitalName + " _" })"
        }
      }
    }
    case t : MapType ⇒ {
      @inline def resolve(t : Type, parameter : String) = t match {
        case t : GroundType if t.getSkillName == "annotation" ⇒
          (Some(if (parameter == "_") "state.resolveAnnotationInternal _" else s"state.resolveAnnotation(new AnnotationRef($parameter))"), true)
        case t : Declaration ⇒
          (Some(if (parameter == "_") s"state.resolve${t.getCapitalName} _" else s"state.resolve${t.getCapitalName}($parameter)"), false)
        case _ ⇒ (if (parameter != "_") Some(parameter) else None, false)
      }
      def makeMapGetter(types : scala.collection.mutable.Buffer[Type], parameter : String) : (Option[String], Boolean) = {
        if (types.tail.isEmpty)
          resolve(types.head, parameter)
        else {
          val first = resolve(types.head, "_")
          val second = makeMapGetter(types.tail, "_")
          val args =
            if (first._2 && second._2) s", ${first._1.get}"
            else first._1.map(", " + _).getOrElse("") + second._1.map(", " + _).getOrElse("")
          (Some(s"new ${mapTypeName(types)}($parameter$args)"), false)
        }
      }
      makeMapGetter(t.getBaseTypes, data)._1.get
    }
  }
  
  private def makeSetter(t : Type, target : String, data : String) : String = t match {
    case t : GroundType ⇒ {
      if (t.getSkillName == "annotation")
        s"$target = $data.annotation"
      else
        s"$target = $data"
    }
    case t : Declaration ⇒ s"$target = $data.reference"
    case t : de.ust.skill.ir.SingleBaseTypeContainer ⇒ {
      t.getBaseType match {
        case u : GroundType if u.getSkillName != "annotation" ⇒ s"$target = $data"
        case u ⇒ s"$target = $data.data"
      }
    }
    case t : MapType ⇒ s"$target = $data.getData"
  }
  
  private def makeListRemapping(list : String, field : String, t : Type) = t match {
    case t : GroundType ⇒ if (t.getSkillName == "annotation") s"\n    remap$list(_$field, maps)" else ""
    case t : Declaration ⇒ s"\n    remap$list(_$field, maps(state.${t.getCapitalName}.asInstanceOf[StoragePool[_, _]].poolIndex))"
    case _ ⇒ ""
  }
  
  private def makeMapRemapping(field : String, types : scala.collection.mutable.Buffer[Type]) = {
    def remapValues(data : String, indent : String, types : scala.collection.mutable.Buffer[Type]) : String = {
      if (types.size == 1)
        types.head match {
          case t : GroundType ⇒ if (t.getSkillName == "annotation") s"\n$indent    remapHashMapValues($data, maps)" else ""
          case t : Declaration ⇒ s"\n$indent    remapHashMapValues($data, maps(state.${t.getCapitalName}.asInstanceOf[StoragePool[_, _]].poolIndex))"
          case _ ⇒ ""
        }
      else {
        val values = remapValues("inner", indent + "  ", types.tail)
        val keys = types.head match {
          case t : GroundType ⇒ if (t.getSkillName == "annotation") s"\n$indent    remapHashMapKeys($data, maps)" else ""
          case t : Declaration ⇒ s"\n$indent    remapHashMapKeys($data, maps(state.${t.getCapitalName}.asInstanceOf[StoragePool[_, _]].poolIndex))"
          case _ ⇒ ""
        }
        (if (values == "") "" else s"\n$indent    for (inner ← $data.values) {$values\n$indent    }") + keys
      }
    }
    
    remapValues("_" + field, "", types)
  }
}
