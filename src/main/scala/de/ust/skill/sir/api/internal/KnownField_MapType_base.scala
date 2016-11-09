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
 * type[] MapType.base
 */
final class KnownField_MapType_base(
  _index : Int,
  _owner : MapTypePool,
  _type : FieldType[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType]])
    extends KnownField[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType],_root_.de.ust.skill.sir.MapType](_type,
      "base",
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
              d(i).asInstanceOf[_root_.de.ust.skill.sir.MapType].Internal_base = t.read(in).asInstanceOf[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType]]
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
                d(i).asInstanceOf[_root_.de.ust.skill.sir.MapType].Internal_base = t.read(in).asInstanceOf[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType]]
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
          val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.MapType].Internal_base
          result += (if(null == v) 1 else V64.offset(v.size))
      
          if(null != v) v.foreach { v => result += (if (null == v) 1 else V64.offset(v.getSkillID)) }
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
          val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.MapType].Internal_base
          result += (if(null == v) 1 else V64.offset(v.size))
      
          if(null != v) v.foreach { v => result += (if (null == v) 1 else V64.offset(v.getSkillID)) }
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
          val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.MapType].Internal_base
          if(null == v) out.i8(0) else { out.v64(v.size)
      
            v.foreach { v => if (null == v) out.i8(0) else out.v64(v.getSkillID) }}
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
            val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.MapType].Internal_base
            if(null == v) out.i8(0) else { out.v64(v.size)
      
            v.foreach { v => if (null == v) out.i8(0) else out.v64(v.getSkillID) }}
            i += 1
          }
        }
    }
  }

  //override def get(i : _root_.de.ust.skill.sir.MapType) = i.base
  //override def set(i : _root_.de.ust.skill.sir.MapType, v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Type]) = i.base = v.asInstanceOf[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType]]

  // note: reflective field access will raise exception for ignored fields
  override def getR(i : SkillObject) : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType] = i.asInstanceOf[_root_.de.ust.skill.sir.MapType].base
  override def setR(i : SkillObject, v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType]) : Unit = i.asInstanceOf[_root_.de.ust.skill.sir.MapType].base = v.asInstanceOf[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.GroundType]]
}
