/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 27.01.2015                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.WrappedArray

import java.nio.MappedByteBuffer
import java.util.Arrays

import de.ust.skill.common.jvm.streams.MappedInStream

import _root_.de.ust.skill.generator.genericBinding.internal.restrictions.FieldRestriction

/**
 * Chunks contain information on where some field data can be found.
 *
 * @param begin position of the first byte of the first instance's data
 * @param end position of the last byte, i.e. the first byte that is not read
 * @param bpo the offset of the first instance
 * @param count the number of instances in this chunk
 *
 * @note indices of recipient of the field data is not necessarily continuous; make use of staticInstances!
 * @note begin and end are vars, because they will contain relative offsets while parsing a type block
 *
 * @author Timm Felden
 */
sealed abstract class ChunkInfo(var begin : Long, var end : Long, val count : Long);
final class SimpleChunkInfo(begin : Long, end : Long, val bpo : Long, count : Long) extends ChunkInfo(begin, end, count);
final class BulkChunkInfo(begin : Long, end : Long, count : Long) extends ChunkInfo(begin, end, count);

/**
 * Blocks contain information about the type of an index range.
 *
 * @param bpo the offset of the first instance
 * @param count the number of instances in this chunk
 * @author Timm Felden
 */
case class BlockInfo(val bpo : Long, val count : Long);

/**
 * A field declaration, as it occurs during parsing of a type blocks header.
 *
 * @author Timm Felden
 * @param t the actual type of the field; can be an intermediate type, while parsing a block
 * @param name the name of the field
 * @param index the index of this field, starting from 0; required for append operations
 * @param T the scala type of t
 *
 * @note index 0 is used for the skillID
 * @note specialized in everything but unit
 */
sealed abstract class FieldDeclaration[T](
    var t : FieldType[T],
    val name : String,
    val index : Long,
    val owner : StoragePool[_ <: SkillType, _ <: SkillType]) {

  /**
   *  Data chunk information, as it is required for later parsing.
   */
  protected val dataChunks = ListBuffer[ChunkInfo]();
  private[internal] final def addChunk(ci : ChunkInfo) : Unit = dataChunks.append(ci)
  private[internal] def addOffsetToLastChunk(offset : Long) {
    val c = dataChunks.last
    c.begin += offset
    c.end += offset
  }
  private[internal] final def noDataChunk = dataChunks.isEmpty
  private[internal] final def lastChunk = dataChunks.last

  /**
   * Restriction handling.
   */
  val restrictions = HashSet[FieldRestriction[T]]();
  def addRestriction[U](r : FieldRestriction[U]) = restrictions += r.asInstanceOf[FieldRestriction[T]]
  def check {
    if (!restrictions.isEmpty)
      owner.all.foreach { x ⇒ restrictions.foreach(_.check(x.get(this))) }
  }

  override def toString = t.toString+" "+name
  override def equals(obj : Any) = obj match {
    case f : FieldDeclaration[T] ⇒ name == f.name && t == f.t
    case _                       ⇒ false
  }
  override def hashCode = name.hashCode ^ t.hashCode

  /**
   * Read data from a mapped input stream and set it accordingly
   */
  def read(in : MappedInStream) : Unit

  /**
   * reflective get
   * @note it is silently assumed, that owner.contains(i)
   * @note known fields provide .get methods that are generally faster, because they exist without boxing
   */
  def getR(i : SkillType) : T;
  /**
   * reflective set
   * @note it is silently assumed, that owner.contains(i)
   * @note known fields provide .get methods that are generally faster, because they exist without boxing
   */
  def setR(i : SkillType, v : T) : Unit;
}

/**
 * This field type indicate that the type is known and therefore a field of the emitted instance.
 *
 * @note the name is a bit miss-leading, as it excludes distributed and lazy known fields
 */
sealed trait KnownField[B <: SkillType, @specialized T] {
  def get(i : B) : T
  def set(i : B, v : T) : Unit
}

/**
 * This trait marks auto fields, i.e. fields that wont be touched by serialization.
 */
trait AutoField {
  final def read(in : MappedInStream) = throw new NoSuchMethodError("one can not read auto fields!")
}

/**
 * This trait marks ignored fields.
 */
trait IgnoredField {
  final def read(in : MappedInStream) {};
}

/**
 * Special skillID auto field.
 */
final class KnownField_SkillID(owner : StoragePool[_ <: SkillType, _ <: SkillType])
    extends FieldDeclaration[Long](V64, "skillid", 0, owner)
    with KnownField[SkillType, Long]
    with AutoField {

  override def get(i : SkillType) = i.getSkillID
  override def set(i : SkillType, v : Long) = throw new NoSuchMethodError("setting skillIDs is not legal")
  override def getR(i : SkillType) = i.getSkillID
  override def setR(i : SkillType, v : Long) = throw new NoSuchMethodError("setting skillIDs is not legal")
}

/**
 * The fields data is distributed into an array (for now its a hash map) holding its instances.
 */
sealed class DistributedField[@specialized(Boolean, Byte, Char, Double, Float, Int, Long, Short) T : Manifest](
  t : FieldType[T],
  name : String,
  index : Long,
  owner : StoragePool[_ <: SkillType, _ <: SkillType])
    extends FieldDeclaration[T](t, name, index, owner) {

  // data held as in storage pools
  // @note see paper notes for O(1) implementation
  protected var data = HashMap[SkillType, T]() //Array[T]()
  protected var newData = HashMap[SkillType, T]()

  override def read(in : MappedInStream) {
    val d : WrappedArray[_ <: SkillType] = owner match {
      case p : BasePool[_]   ⇒ p.data
      case p : SubPool[_, _] ⇒ p.data
    }
    lastChunk match {
      case c : SimpleChunkInfo ⇒
        val low = c.bpo.toInt
        val high = (c.bpo + c.count).toInt
        for (i ← low until high) {
          data(d(i)) = t.readSingleField(in)
        }
      case bci : BulkChunkInfo ⇒
        for (
          bi ← owner.blockInfos;
          i ← bi.bpo.toInt until (bi.bpo + bi.count).toInt
        ) {
          data(d(i)) = t.readSingleField(in)
        }
    }
  }

  override def getR(ref : SkillType) : T = {
    if (-1 == ref.getSkillID)
      return newData(ref)
    else
      return data(ref)
  }
  override def setR(ref : SkillType, value : T) {
    if (-1 == ref.getSkillID)
      newData.put(ref, value)
    else
      data(ref) = value
  }

  def iterator = data.iterator ++ newData.valuesIterator
}

/**
 * The field is distributed and loaded on demand.
 * Unknown fields are lazy as well.
 *
 * @note implementation abuses a distributed field that can be accessed iff there are no data chunks to be processed
 */
final class LazyField[T : Manifest](
  t : FieldType[T],
  name : String,
  index : Long,
  owner : StoragePool[_ <: SkillType, _ <: SkillType])
    extends DistributedField[T](t, name, index, owner) {

  // pending parts that have to be loaded
  private var parts = ListBuffer[MappedInStream]()
  private def isLoaded = parts.isEmpty

  // executes pending read operations
  private def load {
    val d : WrappedArray[_ <: SkillType] = owner match {
      case p : BasePool[_]   ⇒ p.data
      case p : SubPool[_, _] ⇒ p.data
    }

    for (chunk ← dataChunks) {
      val in = parts.head
      parts.remove(0)
      chunk match {
        case c : SimpleChunkInfo ⇒
          val low = c.bpo.toInt
          val high = (c.bpo + c.count).toInt
          for (i ← low until high) {
            data(d(i)) = t.readSingleField(in)
          }
        case bci : BulkChunkInfo ⇒
          var count = bci.count;
          for (
            bi ← owner.blockInfos; if ({ count -= bi.count; count >= 0 });
            i ← bi.bpo.toInt until (bi.bpo + bi.count).toInt
          ) {
            data(d(i)) = t.readSingleField(in)
          }
      }
    }
  }

  override def read(part : MappedInStream) {
    parts += part
  }

  override def getR(ref : SkillType) : T = {
    if (-1 == ref.getSkillID)
      return newData(ref)

    if (!isLoaded)
      load

    return super.getR(ref)
  }

  override def setR(ref : SkillType, v : T) {
    if (-1 == ref.getSkillID)
      newData(ref) = v
    else {

      if (!isLoaded)
        load

      return super.setR(ref, v)
    }
  }

  override def iterator = {
    if (!isLoaded)
      load

    super.iterator
  }

}
