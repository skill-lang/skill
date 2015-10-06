/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api.internal

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
import de.ust.skill.ir.View
trait StateWriterMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val packageName = if (this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.') + 1) else this.packageName;
    val out = open("internal/StateWriter.scala")
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
import scala.language.existentials

import de.ust.skill.common.jvm.streams.MappedOutStream
import de.ust.skill.common.jvm.streams.FileOutputStream

/**
 * Holds state of a write operation.
 *
 * @see SKilL §6
 * @author Timm Felden
 */
private[internal] final class StateWriter(state : State, out : FileOutputStream) extends SerializationFunctions(state) {
  import SerializationFunctions._

  // make lbpsi map, update data map to contain dynamic instances and create serialization skill IDs for serialization
  // index → bpsi
  //@note pools.par would not be possible if it were an actual map:)
  val lbpsiMap = new Array[Long](state.pools.length)
  state.pools.par.foreach {
    case p : BasePool[_] ⇒
      makeLBPOMap(p, lbpsiMap, 0, _.staticSize)
      p.compress(lbpsiMap)
    case _ ⇒
  }

  /**
   * ****************
   * PHASE 3: WRITE *
   * ****************
   */
  // write string block
  state.String.asInstanceOf[StringPool].prepareAndWrite(out, this)

  // write count of the type block
  v64(state.pools.size, out)

  // calculate offsets
  val offsets = new HashMap[StoragePool[_ <: SkillType, _ <: SkillType], HashMap[FieldDeclaration[_], Future[Long]]]
  for (p ← state.pools) {
    val vs = new HashMap[FieldDeclaration[_], Future[Long]]
    for (f ← p.fields if f.index != 0) {
      val v = new FutureTask(new Callable[Long]() {
        def call : Long = offset(p, f)
      })
      vs.put(f, v)
      ExecutionContext.Implicits.global.execute(v)
    }
    offsets.put(p, vs)
  }

  // create type definitions
  @inline def genericPutField[T](p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[T], dataChunk : MappedOutStream) {
    f.t match {
      case I8                    ⇒ for (i ← p) dataChunk.i8(i.get(f).asInstanceOf[Byte])
      case I16                   ⇒ for (i ← p) dataChunk.i16(i.get(f).asInstanceOf[Short])
      case I32                   ⇒ for (i ← p) dataChunk.i32(i.get(f).asInstanceOf[Int])
      case I64                   ⇒ for (i ← p) dataChunk.i64(i.get(f).asInstanceOf[Long])
      case V64                   ⇒ for (i ← p) dataChunk.v64(i.get(f).asInstanceOf[Long])

      case StringType(_)         ⇒ for (i ← p) string(i.get(f).asInstanceOf[String], dataChunk)

      case s : StoragePool[_, _] ⇒ for (i ← p) userRef(i, out)

      case other ⇒
        val den = typeToSerializationFunction(other); for (i ← p) den(i.get(f), dataChunk)
    }
  }

  case class Task[B <: SkillType](val p : StoragePool[_ <: B, B], val f : FieldDeclaration[_], val begin : Long, val end : Long);
  val data = new ArrayBuffer[Task[_ <: SkillType]];
  var offset = 0L
  var fieldQueue = new ArrayBuffer[ArrayBuffer[FieldDeclaration[_]]]
  for (p ← state.pools) {
    val fields = p.fields.filter(_.index != 0)
    string(p.name, out)
    val LCount = p.blockInfos.last.count
    out.v64(LCount)
    restrictions(p, out)
    p.superName match {
      case Some(sn) ⇒
        string(sn, out)
        if (0L != LCount)
          out.v64(lbpsiMap(p.poolIndex.toInt))

      case None ⇒
        out.i8(0.toByte)
    }

    out.v64(fields.size)
    fieldQueue += fields;
  }

  // write fields
  for (fields ← fieldQueue) {
    for (f ← fields) {
      val p = f.owner;
      val vs = offsets(p)
      out.v64(f.index)
      string(f.name, out)
      writeType(f.t, out)
      restrictions(f, out)
      val end = offset + vs(f).get
      out.v64(end)
      data += Task(p.asInstanceOf[StoragePool[SkillType, SkillType]], f, offset, end)
      offset = end
    }
  }

  // write field data
  val barrier = new Semaphore(0)
  val baseOffset = out.position
  for ((Task(p, f, begin, end)) ← data) {
    val dataChunk = out.map(baseOffset, begin, end)
    // @note use semaphore instead of data.par, because map is not thread-safe
    ExecutionContext.Implicits.global.execute(new Runnable {
      override def run = try {
        p match {${
      (for (d ← IR) yield {
        val fields = d.getFields.filterNot(_.isIgnored)
        if (fields.isEmpty) ""
        else s"""
          case pool : ${storagePool(d)} ⇒
            val outData = pool.${if(d.getSuperType()!=null)"all"else"data"}
            f.name match {${
          (for (f ← fields if !f.isInstanceOf[View]) yield s"""
              case "${f.getSkillName()}" ⇒ ${writeField(d, f)}""").mkString("")
        }
              case _ ⇒ genericPutField(p, f, dataChunk)
            }"""
      }
      ).mkString
    }

          case _ ⇒ genericPutField(p, f, dataChunk)
        }
      } finally {
        // ensure that writer can terminate, errors will be printed to command line anyway, and we wont be able to
        // recover, because errors can only happen if the skill implementation itself is broken
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
