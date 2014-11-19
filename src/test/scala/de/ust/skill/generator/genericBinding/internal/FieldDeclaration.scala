/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 19.11.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import java.nio.MappedByteBuffer
import java.util.Arrays

import _root_.de.ust.skill.generator.genericBinding.internal.streams.MappedInStream
import _root_.de.ust.skill.generator.genericBinding.internal.restrictions.FieldRestriction

/**
 * Chunks contain information on where some field data can be found.
 *
 * @param begin position of the first byte of the first instance's data
 * @param end position of the last byte, i.e. the first byte that is not read
 * @param bpso the offset of the first instance
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
trait FieldDeclaration[@specialized(Boolean, Byte, Char, Double, Float, Int, Long, Short) T] {
  var t : FieldType[T];
  def name : String;
  def index : Long;
  def owner : StoragePool[_ <: SkillType, _ <: SkillType];

  /**
   *  Data chunk information, as it is required for later parsing.
   */
  protected val dataChunks = ListBuffer[ChunkInfo]();
  private[internal] def addChunk(ci : ChunkInfo) : Unit = dataChunks += ci
  private[internal] def addOffsetToLastChunk(offset : Long) {
    val c = dataChunks.last
    c.begin += offset
    c.end += offset
  }
  private[internal] def noDataChunk = dataChunks.isEmpty
  private[internal] def lastChunk = dataChunks.last

  /**
   * Restriction handling.
   */
  val restrictions = HashSet[FieldRestriction[T]]();
  def addRestriction[U](r : FieldRestriction[U]) = restrictions += r.asInstanceOf[FieldRestriction[T]]
  def check {
    if(!restrictions.isEmpty)
      owner.all.foreach { x => restrictions.foreach(_.check(x.get(this))) }
  }

  override def toString = t.toString+" "+name
  override def equals(obj : Any) = obj match {
    case f : FieldDeclaration[T] ⇒ name == f.name && t == f.t
    case _                       ⇒ false
  }
  override def hashCode = name.hashCode ^ t.hashCode
}

/**
 * This field type indicate that the type is known and therefore a field of the emitted instance.
 *
 * @note the name is a bit miss-leading, as it excludes distributed and lazy known fields
 */
final class KnownField[@specialized(Boolean, Byte, Char, Double, Float, Int, Long, Short) T](
  override var t : FieldType[T],
  override val name : String,
  override val index : Long,
  override val owner : StoragePool[_ <: SkillType, _ <: SkillType])
    extends FieldDeclaration[T];

/**
 * The fields data is distributed into an array holding its instances.
 *
 * TODO sicherstellen, dass distributed felder vom generierten serialisierungscode ausgenommen sind!!
 */
sealed class DistributedField[@specialized(Boolean, Byte, Char, Double, Float, Int, Long, Short) T : Manifest](
  override var t : FieldType[T],
  override val name : String,
  override val index : Long,
  override val owner : StoragePool[_ <: SkillType, _ <: SkillType])
    extends FieldDeclaration[T] {

  // data held as in storage pools
  protected var data = Array[T]()
  protected var newData = HashMap[SkillType, T]()

  /**
   * resizes the data array, but leaves data unchanged
   */
  override def addChunk(ci : ChunkInfo) {
    dataChunks += ci
    val d = data
    data = new Array[T](data.length + ci.count.toInt)
    for (i ← 0 until d.length)
      data(i) = d(i)
  }

  private[internal] def read(part : MappedInStream) {
    // TODO parse current part
  }

  /**
   * direct access
   */
  private[internal] def get(ref : SkillType) : T = {
    if (-1 == ref.getSkillID)
      return newData(ref)
    else
      return data(ref.getSkillID.toInt - 1)
  }
  private[internal] def set(ref : SkillType, value : T) {
    if (-1 == ref.getSkillID)
      newData.put(ref, value)
    else
      data(ref.getSkillID.toInt - 1) = value
  }

  def iterator = data.iterator ++ newData.valuesIterator
}

///**
// * The field is distributed and loaded on demand.
// */
//
//final class LazyField[@specialized T : Manifest](t : FieldType[T], name : String, index : Long)
//    extends DistributedField[T](t, name, index) {
//
//  // pending parts that have to be loaded
//  private var parts = ListBuffer[MappedInStream]()
//  private def isLoaded = parts.isEmpty
//  // executes pending read operations
//  private def load {
//    // TODO not correct in case of multiple pending cis...add an argument to read!
//    for (part ← parts)
//      super.read(part)
//  }
//
//  override def read(part : MappedInStream) {
//    parts += part
//  }
//
//  /**
//   * direct access
//   */
//  private[internal] def get(ref : SkillType) : T = {
//    if (-1 == ref.getSkillID)
//      return newData(ref)
//
//    if (!isLoaded)
//      load
//
//    return data(ref.getSkillID.toInt - 1)
//  }
//
//  // TODO set
//
//  def iterator = {
//    if (!isLoaded)
//      load
//
//    super.iterator
//  }
//
//}
