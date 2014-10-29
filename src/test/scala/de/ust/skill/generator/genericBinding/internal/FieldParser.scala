/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 29.10.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.WrappedArray

import de.ust.skill.generator.genericBinding.api.Access
import de.ust.skill.generator.genericBinding.api.StringAccess
import de.ust.skill.generator.genericBinding.internal._
import de.ust.skill.generator.genericBinding.internal.streams.InStream

/**
 * The field parser is able to turn field data from a type block data chunk into an array of field entries
 */
object FieldParser {
  /**
   * Parse a field assuming that in is at the right position and the last chunk of f is to be processed.
   */
  def parseThisField[T](in : InStream, t : StoragePool[_ <: SkillType, _ <: SkillType], f : KnownField[T], pools : HashMap[String, StoragePool[_ <: SkillType, _ <: SkillType]], String : StringAccess) {

    val c = f.lastChunk
    if (in.position != c.begin)
      throw new SkillException("@begin of data chunk: expected position(0x${in.position.toHexString}) to be 0x${c.begin.toHexString}")

    // partial fields, refer to TR14§???
    if (0 != c.count && c.begin == c.end && f.t.typeID > 4) {
      System.err.println(s"[SKilL TR14] detected partial field: $f");
      return
    }

    t match {

      case _ ⇒
        c match {
          case c : SimpleChunkInfo ⇒
            val low = c.bpsi.toInt
            val high = (c.bpsi + c.count).toInt
            for (i ← low until high)
              t.getByID(i + 1).set(f, f.t.readSingleField(in))

          case bci : BulkChunkInfo ⇒
            for (
              bi ← t.blockInfos;
              i ← bi.bpsi.toInt until (bi.bpsi + bi.count).toInt
            ) t.getByID(i + 1).set(f, f.t.readSingleField(in))
        }
    }

    if (in.position != c.end)
      throw PoolSizeMissmatchError(c.end - c.begin, in.position - c.begin, f.t.toString)
  }

  /**
   * Reads an array of single fields of type t.
   */
  private[this] def readArray[T](size : Long, t : FieldType[T], in : InStream, pools : HashMap[String, StoragePool[_, _ <: SkillType]], strings : ArrayBuffer[String]) : ArrayBuffer[T] = {
    val result = new ArrayBuffer[T](size.toInt)
    for (i ← 0 until size.toInt) {
      result += t.readSingleField(in)
    }
    result
  }
}
