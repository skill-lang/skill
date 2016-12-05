/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 05.12.2016                               *
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
import de.ust.skill.common.scala.internal.DistributedField
import de.ust.skill.common.scala.internal.IgnoredField
import de.ust.skill.common.scala.internal.FieldDeclaration
import de.ust.skill.common.scala.internal.KnownField
import de.ust.skill.common.scala.internal.LazyField
import de.ust.skill.common.scala.internal.SimpleChunk
import de.ust.skill.common.scala.internal.SingletonStoragePool
import de.ust.skill.common.scala.internal.fieldTypes._
import de.ust.skill.common.scala.internal.restrictions._

/**
 * comment FieldLike.comment
 */
final class KnownField_FieldLike_comment(
  _index : Int,
  _owner : FieldLikePool,
  _type : FieldType[_root_.de.ust.skill.sir.Comment])
    extends FieldDeclaration[_root_.de.ust.skill.sir.Comment,_root_.de.ust.skill.sir.FieldLike](_type,
      "comment",
      _index,
      _owner)
    with KnownField[_root_.de.ust.skill.sir.Comment,_root_.de.ust.skill.sir.FieldLike] {

      
  override def createKnownRestrictions : Unit = {
    
  }

  override def read(part : MappedInStream, target : Chunk) {
    val d = owner.data
    val in = part.view(target.begin.toInt, target.end.toInt)
    val t = this.t.asInstanceOf[CommentPool]
    try {
        target match {
          case c : SimpleChunk ⇒
            var i = c.bpo.toInt
            val high = i + c.count
            while (i != high) {
              d(i).asInstanceOf[_root_.de.ust.skill.sir.FieldLike].Internal_comment = t.read(in).asInstanceOf[_root_.de.ust.skill.sir.Comment]
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
                d(i).asInstanceOf[_root_.de.ust.skill.sir.FieldLike].Internal_comment = t.read(in).asInstanceOf[_root_.de.ust.skill.sir.Comment]
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
          val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.FieldLike].Internal_comment
          result += (if (null == v) 1 else V64.offset(v.getSkillID))
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
          val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.FieldLike].Internal_comment
          result += (if (null == v) 1 else V64.offset(v.getSkillID))
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
          val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.FieldLike].Internal_comment
          if (null == v) out.i8(0) else out.v64(v.getSkillID)
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
            val v = data(i).asInstanceOf[_root_.de.ust.skill.sir.FieldLike].Internal_comment
            if (null == v) out.i8(0) else out.v64(v.getSkillID)
            i += 1
          }
        }
    }
  }

  //override def get(i : _root_.de.ust.skill.sir.FieldLike) = i.comment
  //override def set(i : _root_.de.ust.skill.sir.FieldLike, v : _root_.de.ust.skill.sir.Comment) = i.comment = v.asInstanceOf[_root_.de.ust.skill.sir.Comment]

  // note: reflective field access will raise exception for ignored fields
  override def getR(i : SkillObject) : _root_.de.ust.skill.sir.Comment = i.asInstanceOf[_root_.de.ust.skill.sir.FieldLike].comment
  override def setR(i : SkillObject, v : _root_.de.ust.skill.sir.Comment) : Unit = i.asInstanceOf[_root_.de.ust.skill.sir.FieldLike].comment = v.asInstanceOf[_root_.de.ust.skill.sir.Comment]
}
