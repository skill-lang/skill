/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 09.11.2016                               *
 * |___/_|\_\_|_|____|    by: feldentm                                        *
\*                                                                            */
package de.ust.skill.sir.api.internal

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.WrappedArray

import java.nio.BufferUnderflowException
import java.nio.MappedByteBuffer
import java.util.Arrays

import de.ust.skill.common.jvm.streams.MappedInStream
import de.ust.skill.common.jvm.streams.MappedOutStream
import de.ust.skill.common.scala.api.PoolSizeMissmatchError
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.internal.AutoField
import de.ust.skill.common.scala.internal.BulkChunk
import de.ust.skill.common.scala.internal.Chunk
import de.ust.skill.common.scala.internal.IgnoredField
import de.ust.skill.common.scala.internal.KnownField
import de.ust.skill.common.scala.internal.SimpleChunk
import de.ust.skill.common.scala.internal.SingletonStoragePool
import de.ust.skill.common.scala.internal.fieldTypes._
import de.ust.skill.common.scala.internal.restrictions._

/**
 * v64 ConstantInteger.value
 */
final class KnownField_ConstantInteger_value(
  _index : Int,
  _owner : ConstantIntegerPool,
  _type : FieldType[Long] = V64)
    extends KnownField[Long,_root_.de.ust.skill.sir.ConstantInteger](_type,
      "value",
      _index,
      _owner) {

  override def createKnownRestrictions : Unit = {
    
  }

  override def read(part : MappedInStream, target : Chunk) {
    val d = owner.data
    val in = part.view(target.begin.toInt, target.end.toInt)

    try {
        target match {
          case c : SimpleChunk ⇒
            var i = c.bpo.toInt
            val high = i + c.count
            while (i != high) {
              d(i).asInstanceOf[_root_.de.ust.skill.sir.ConstantInteger].Internal_value = t.read(in).asInstanceOf[Long]
              i += 1
            }
          case bci : BulkChunk ⇒
            val blocks = owner.blocks
            var blockIndex = 0
            while (blockIndex < bci.blockCount) {
              val b = blocks(blockIndex)
              blockIndex += 1
              var i = b.bpo
              val end = i + b.dynamicCount
              while (i != end) {
                d(i).asInstanceOf[_root_.de.ust.skill.sir.ConstantInteger].Internal_value = t.read(in).asInstanceOf[Long]
                i += 1
              }
            }
        }
    } catch {
      case e : BufferUnderflowException ⇒
        throw new PoolSizeMissmatchError(dataChunks.size - 1,
          part.position() + target.begin,
          part.position() + target.end,
          this, in.position())
    }

    if(!in.eof())
      throw new PoolSizeMissmatchError(dataChunks.size - 1,
        part.position() + target.begin,
        part.position() + target.end,
        this, in.position())
  }

  def offset: Unit = {
    val data = owner.data
    var result = 0L
    dataChunks.last match {
      case c : SimpleChunk ⇒
        var i = c.bpo.toInt
        val high = i + c.count
        while (i != high) {
          val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.ConstantInteger].Internal_value
          result += (if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
              1L
            } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
              2
            } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
              3
            } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
              4
            } else if (0L == (v & 0xFFFFFFF800000000L)) {
              5
            } else if (0L == (v & 0xFFFFFC0000000000L)) {
              6
            } else if (0L == (v & 0xFFFE000000000000L)) {
              7
            } else if (0L == (v & 0xFF00000000000000L)) {
              8
            } else {
              9
            })
          i += 1
        }
      case bci : BulkChunk ⇒
        val blocks = owner.blocks
        var blockIndex = 0
        while (blockIndex < bci.blockCount) {
          val b = blocks(blockIndex)
          blockIndex += 1
          var i = b.bpo
          val end = i + b.dynamicCount
          while (i != end) {
          val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.ConstantInteger].Internal_value
          result += (if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
              1L
            } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
              2
            } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
              3
            } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
              4
            } else if (0L == (v & 0xFFFFFFF800000000L)) {
              5
            } else if (0L == (v & 0xFFFFFC0000000000L)) {
              6
            } else if (0L == (v & 0xFFFE000000000000L)) {
              7
            } else if (0L == (v & 0xFF00000000000000L)) {
              8
            } else {
              9
            })
          i += 1
          }
        }
    }
    cachedOffset = result
  }

  def write(out: MappedOutStream): Unit = {
    val data = owner.data
    dataChunks.last match {
      case c : SimpleChunk ⇒
        var i = c.bpo.toInt
        val high = i + c.count
        while (i != high) {
          val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.ConstantInteger].Internal_value
          out.v64(v)
          i += 1
        }
      case bci : BulkChunk ⇒
        val blocks = owner.blocks
        var blockIndex = 0
        while (blockIndex < bci.blockCount) {
          val b = blocks(blockIndex)
          blockIndex += 1
          var i = b.bpo
          val end = i + b.dynamicCount
          while (i != end) {
            val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.ConstantInteger].Internal_value
            out.v64(v)
            i += 1
          }
        }
    }
  }

  //override def get(i : _root_.de.ust.skill.sir.ConstantInteger) = i.value
  //override def set(i : _root_.de.ust.skill.sir.ConstantInteger, v : Long) = i.value = v.asInstanceOf[Long]

  // note: reflective field access will raise exception for ignored fields
  override def getR(i : SkillObject) : Long = i.asInstanceOf[_root_.de.ust.skill.sir.ConstantInteger].value
  override def setR(i : SkillObject, v : Long) : Unit = i.asInstanceOf[_root_.de.ust.skill.sir.ConstantInteger].value = v.asInstanceOf[Long]
}
