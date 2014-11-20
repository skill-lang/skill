/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.restriction.SingletonRestriction
import de.ust.skill.ir.SetType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.Type
trait StateAppenderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val packageName = if (this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.') + 1) else this.packageName;
    val out = open("internal/StateAppender.scala")
    //package
    out.write(s"""package ${packagePrefix}internal

import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.Semaphore

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.WrappedArray
import scala.concurrent.ExecutionContext

import de.ust.skill.common.jvm.streams.MappedOutStream
import de.ust.skill.common.jvm.streams.FileOutputStream

/**
 * Holds state of a write operation.
 *
 * @see SKilL §6
 * @author Timm Felden
 */
private[internal] final class StateAppender(state : State, out : FileOutputStream) extends SerializationFunctions(state) {
  import SerializationFunctions._

  // save the index of the first new pool
  val newPoolIndex = state.pools.indexWhere(_.blockInfos.isEmpty) match {
    case -1L ⇒ state.pools.size + 1 // ensure that no pool is marked as *new*
    case i   ⇒ i.toLong
  }

  // make lbpsi map, update data map to contain dynamic instances and create serialization skill IDs for serialization
  // index → bpsi
  val lbpsiMap = new Array[Long](state.pools.length)
  val chunkMap = HashMap[FieldDeclaration[_], ChunkInfo]()
  state.pools.foreach {
    case p : BasePool[_] ⇒
      makeLBPOMap(p, lbpsiMap, 0, _.newObjects.size)
      //@note it is very important to prepare after the creation of the lbpsi map
      p.prepareAppend(chunkMap)
    case _ ⇒
  }

  // locate relevant pools
  val rPools = state.pools.filter { p ⇒
    p.poolIndex >= newPoolIndex || (p.dynamicSize > 0 && p.fields.exists(chunkMap.contains(_)))
  }

  // locate relevant fields
  val rFields = chunkMap.keySet.toArray

  /**
   * ****************
   * PHASE 3: WRITE *
   * ****************
   */

  // write string block
  state.String.asInstanceOf[StringPool].prepareAndAppend(out, this)

  // write count of the type block
  v64(rPools.size, out)

  // calculate offsets
  val offsets = HashMap[StoragePool[_ <: SkillType, _ <: SkillType], HashMap[FieldDeclaration[_], Future[Long]]]();
  for (p ← rPools) offsets(p) = HashMap()
  for (f ← rFields) {
    val v = new FutureTask(new Callable[Long]() {
      def call : Long = offset(f.owner, f)
    })
    ExecutionContext.Implicits.global.execute(v)
    offsets(f.owner)(f) = v
  }

  // @note performance hack: requires at least 1 instance in order to work correctly
  @inline def genericPutField[T](p : TraversableOnce[SkillType], f : FieldDeclaration[T], dataChunk : MappedOutStream) {
    f.t match {
      case I8            ⇒ for (i ← p) dataChunk.i8(i.get(f).asInstanceOf[Byte])
      case I16           ⇒ for (i ← p) dataChunk.i16(i.get(f).asInstanceOf[Short])
      case I32           ⇒ for (i ← p) dataChunk.i32(i.get(f).asInstanceOf[Int])
      case I64           ⇒ for (i ← p) dataChunk.i64(i.get(f).asInstanceOf[Long])
      case V64           ⇒ for (i ← p) dataChunk.v64(i.get(f).asInstanceOf[Long])

      case StringType(_) ⇒ for (i ← p) string(i.get(f).asInstanceOf[String], dataChunk)

      case other ⇒
        val den = typeToSerializationFunction(other); for (i ← p) den(i.get(f), dataChunk)
    }
  }

  case class Task(val is : TraversableOnce[SkillType], val f : FieldDeclaration[_], val begin : Long, val end : Long);
  val data = new ArrayBuffer[Task];
  var offset = 0L
  for (p ← rPools) {
    // generic append
    val newPool = p.poolIndex >= newPoolIndex
    val fields = p.fields.filter(chunkMap.contains(_))
    if (newPool || (0 != fields.size && p.dynamicSize > 0)) {

      string(p.name, out)
      val count = p.blockInfos.last.count
      v64(count, out)

      if (newPool) {
        restrictions(p, out)
        p.superName match {
          case Some(sn) ⇒
            string(sn, out)
            out.v64(lbpsiMap(p.poolIndex.toInt))
          case None ⇒
            out.i8(0)
        }
      } else for (sn ← p.superName) {
        out.v64(lbpsiMap(p.poolIndex.toInt))
      }

      if (newPool && 0 == count) {
        out.i8(0);
      } else {
        val vs = offsets(p)
        out.v64(fields.size)
        for (f ← fields) {
          out.v64(f.index)
          val outData = f.lastChunk match {
            case bci : BulkChunkInfo ⇒
              string(f.name, out)
              writeType(f.t, out)
              restrictions(f, out)

              p.all

            case sci : SimpleChunkInfo ⇒
              p.basePool.data.view(sci.bpo.toInt, (sci.bpo + sci.count).toInt)
          }
          // put end offset and enqueue data
          val end = offset + vs(f).get
          out.v64(end)
          data += Task(outData, f, offset, end)
          offset = end
        }
      }
    }
  }

  // write field data
  val barrier = new Semaphore(0)
  val baseOffset = out.position
  for ((Task(fieldData, f, begin, end)) ← data) {
    val dataChunk = out.map(baseOffset, begin, end)
    ExecutionContext.Implicits.global.execute(new Runnable {
      override def run = {
        genericPutField(fieldData, f, dataChunk)
        barrier.release(1)
      }
    }
    )
  }
  barrier.acquire(data.size)
  out.close
}
""")

    //class prefix
    out.close()
  }
}
