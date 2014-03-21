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
    val packageName = if(this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.')+1) else this.packageName;
    val out = open("internal/StateAppender.scala")
    //package
    out.write(s"""package ${packagePrefix}internal

import java.util.Arrays
import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import ${packagePrefix}_
import ${packagePrefix}internal.streams.OutBuffer
import ${packagePrefix}internal.streams.OutStream

/**
 * Holds state of a write operation.
 *
 * @see SKilL §6
 * @author Timm Felden
 */
private[internal] final class StateAppender(state : SerializableState, out : OutStream) extends SerializationFunctions(state) {
  import SerializationFunctions._

  // save the index of the first new pool
  val newPoolIndex = state.pools.indexWhere(_.blockInfos.isEmpty) match {
    case -1L ⇒ state.pools.size + 1 // ensure that no pool is marked as *new*
    case i   ⇒ i.toLong
  }

  // make lbpsi map, update data map to contain dynamic instances and create serialization skill IDs for serialization
  // index → bpsi
  val lbpsiMap = new Array[Long](state.pools.length)
  val chunkMap = HashMap[FieldDeclaration, ChunkInfo]()
  state.pools.foreach {
    case p : BasePool[_] ⇒
      makeLBPSIMap(p, lbpsiMap, 1, { s ⇒ state.poolByName(s).newObjects.size })
      //@note it is very important to prepare after the creation of the lbpsi map
      p.prepareAppend(chunkMap)
    case _ ⇒
  }

  override def annotation(ref : SkillType, out : OutStream) {
    if (null == ref) {
      out.put(0.toByte)
      out.put(0.toByte)
    } else {
      val baseName = state.poolByName(ref.getClass.getSimpleName.toLowerCase).basePool.name
      string(baseName, out)
      v64(ref.getSkillID, out)
    }
  }

  /**
   * ****************
   * PHASE 3: WRITE *
   * ****************
   */
  locally {
    // write string block
    state.String.asInstanceOf[StringPool].prepareAndAppend(out, this)

    // write count of the type block
    v64(state.pools.size, out)

    // we have to buffer the data chunk before writing it
    val dataChunk = new OutBuffer();

    // @note performance hack: requires at least 1 instance in order to work correctly
    @inline def genericPutField(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration, instances : TraversableOnce[SkillType]) {
      f.t match {
        case I8         ⇒ for (i ← instances) i8(i.get(p, f).asInstanceOf[Byte], dataChunk)
        case I16        ⇒ for (i ← instances) i16(i.get(p, f).asInstanceOf[Short], dataChunk)
        case I32        ⇒ for (i ← instances) i32(i.get(p, f).asInstanceOf[Int], dataChunk)
        case I64        ⇒ for (i ← instances) i64(i.get(p, f).asInstanceOf[Long], dataChunk)
        case V64        ⇒ for (i ← instances) v64(i.get(p, f).asInstanceOf[Long], dataChunk)

        case StringType ⇒ for (i ← instances) string(i.get(p, f).asInstanceOf[String], dataChunk)
      }
    }
    for (p ← state.pools) {
      p match {${
      (for (d ← IR) yield {
        val sName = d.getSkillName
        val fields = d.getFields.filterNot(_.isIgnored)
        s"""
        case p : ${d.getCapitalName}StoragePool ⇒
          val newPool = p.poolIndex >= newPoolIndex
          val fields = p.fields.filter(chunkMap.contains(_))
          if (0 != fields.size || newPool) {
            string("$sName", out)
            if (newPool) {
              ${
          if (null == d.getSuperType) "out.put(0.toByte)"
          else s"""string("${d.getSuperType.getSkillName}", out)
              v64(lbpsiMap(p.poolIndex.toInt), out)"""
        }
            }
            val count = p.blockInfos.tail.size
            out.v64(count)

            if (newPool)
              restrictions(p, out)

            out.v64(fields.size)

            for (f ← fields) {
              val outData = f.dataChunks.last match {
                case bci : BulkChunkInfo ⇒
                  restrictions(f, out)
                  writeType(f.t, out)
                  string(f.name, out)

                  p.all

                case sci : SimpleChunkInfo ⇒
                  p.data.view(sci.bpsi.toInt, (sci.bpsi + sci.count).toInt)

              }
              f.name match {${
          (for (f ← fields) yield s"""
                case "${f.getSkillName()}" ⇒ locally {
                  ${writeField(d, f)}
                }""").mkString("")
        }
                case _ ⇒ if (outData.size > 0) genericPutField(p, f, outData)
              }
              // end
              out.v64(dataChunk.size)
            }
          }"""
      }
      ).mkString("")
    }
        case _ ⇒ locally {
          // generic append
          val newPool = p.poolIndex >= newPoolIndex
          val fields = p.fields.filter(chunkMap.contains(_))
          if (0 != fields.size || newPool) {

            string(p.name, out)
            if (newPool) {
              p.superName match {
                case Some(sn) ⇒
                  string(sn, out)
                  v64(lbpsiMap(p.poolIndex.toInt), out)
                case None ⇒
                  out.put(0.toByte)
              }
            }
            val count = p.blockInfos.tail.size
            v64(count, out)

            if (newPool)
              restrictions(p, out)

            v64(fields.size, out)
            for (f ← fields) {
              val outData = f.dataChunks.last match {
                case bci : BulkChunkInfo ⇒
                  restrictions(f, out)
                  writeType(f.t, out)
                  string(f.name, out)

                  p.all

                case sci : SimpleChunkInfo ⇒
                  p.basePool.data.view(sci.bpsi.toInt, (sci.bpsi + sci.count).toInt)

              }

              if (outData.size > 0) genericPutField(p, f, outData)
              // end
              v64(dataChunk.size, out)
            }
          }
        }
      }
    }
    out.putAll(dataChunk)

    out.close
  }
}
""")

    //class prefix
    out.close()
  }
}
