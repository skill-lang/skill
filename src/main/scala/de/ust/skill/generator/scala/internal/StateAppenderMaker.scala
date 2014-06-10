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
import ${packagePrefix}internal.streams.FileOutputStream
import ${packagePrefix}internal.streams.OutStream

/**
 * Holds state of a write operation.
 *
 * @see SKilL §6
 * @author Timm Felden
 */
private[internal] final class StateAppender(state : SerializableState, out : FileOutputStream) extends SerializationFunctions(state) {
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
      makeLBPSIMap(p, lbpsiMap, 1, { s ⇒ state.poolByName(s).newObjects.size })
      //@note it is very important to prepare after the creation of the lbpsi map
      p.prepareAppend(chunkMap)
    case _ ⇒
  }

  /**
   * ****************
   * PHASE 3: WRITE *
   * ****************
   */

  // write string block
  state.String.asInstanceOf[StringPool].prepareAndAppend(out, this)

  // write count of the type block
  v64(state.pools.filter { p ⇒
    p.poolIndex >= newPoolIndex || (p.dynamicSize > 0 && p.fields.exists(chunkMap.contains(_)))
  }.size, out)

  // TODO see code generator

  out.close
}
""")

// TODO integrate this
val todo = s"""
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
    (for (t ← IR) yield {
      val sName = t.getSkillName
      val fields = t.getFields.filterNot(_.isIgnored)
      s"""
      case p : ${t.getCapitalName}StoragePool ⇒
        val newPool = p.poolIndex >= newPoolIndex
        val fields = p.fields.filter(chunkMap.contains(_))
        if (newPool || (0 != fields.size && p.dynamicSize > 0)) {
          string("$sName", out)
          if (newPool) {
            ${
        if (null == t.getSuperType) "out.put(0.toByte)"
        else s"""string("${t.getSuperType.getSkillName}", out)"""
      }
          }${if(null == t.getSuperType)"" else """
          out.v64(lbpsiMap(p.poolIndex.toInt))"""}
          val count = p.blockInfos.last.count
          out.v64(count)

          if (newPool)
            restrictions(p, out)

          if (newPool && 0 == count) {
            out.put(0.toByte);
          } else {
            out.v64(fields.size)
            for (f ← fields) {
            var fieldSize = 0
              val outData = f.dataChunks.last match {
                case bci : BulkChunkInfo ⇒
                  fieldSize = p.size
                  restrictions(f, out)
                  writeType(f.t, out)
                  string(f.name, out)

                  p.all

                case sci : SimpleChunkInfo ⇒
                  fieldSize = count.toInt
                  p.data.view(sci.bpsi.toInt, (sci.bpsi + sci.count).toInt).iterator.asInstanceOf[Iterator[_root_.${packagePrefix}${t.getCapitalName}]]

              }
              f.name match {${
          (for (f ← fields) yield s"""
                case "${f.getSkillName()}" ⇒ locally {
                  ${writeField(t, f)}
                }""").mkString("")
        }
                case _ ⇒ if (outData.size > 0) genericPutField(p, f, outData)
              }
              // end
              out.v64(dataChunk.size)
            }
          }
        }"""
      }
      ).mkString("")
    }
      case _ ⇒ locally {
        // generic append
        val newPool = p.poolIndex >= newPoolIndex
        val fields = p.fields.filter(chunkMap.contains(_))
        if (newPool || (0 != fields.size && p.dynamicSize > 0)) {

          string(p.name, out)
          if (newPool) {
            p.superName match {
              case Some(sn) ⇒
                string(sn, out)
                out.v64(lbpsiMap(p.poolIndex.toInt))
              case None ⇒
                out.put(0.toByte)
            }
          } else for (sn ← p.superName) {
            out.v64(lbpsiMap(p.poolIndex.toInt))
          }
          val count = p.blockInfos.tail.size
          v64(count, out)

          if (newPool)
            restrictions(p, out)

          if (newPool && 0 == count) {
            out.put(0.toByte);
          } else {
            out.v64(fields.size)
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
  }
  out.putAll(dataChunk)"""

    //class prefix
    out.close()
  }
}
