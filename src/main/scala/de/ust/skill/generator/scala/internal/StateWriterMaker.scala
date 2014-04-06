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
trait StateWriterMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val packageName = if (this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.') + 1) else this.packageName;
    val out = open("internal/StateWriter.scala")
    //package
    out.write(s"""package ${packagePrefix}internal

import java.util.Arrays
import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.WrappedArray

import ${packagePrefix}internal.streams.OutBuffer
import ${packagePrefix}internal.streams.OutStream

/**
 * Holds state of a write operation.
 *
 * @see SKilL §6
 * @author Timm Felden
 */
private[internal] final class StateWriter(state : SerializableState, out : OutStream) extends SerializationFunctions(state) {
  import SerializationFunctions._

  // make lbpsi map, update data map to contain dynamic instances and create serialization skill IDs for serialization
  // index → bpsi
  //@note pools.par would not be possible if it were an actual map:)
  val lbpsiMap = new Array[Long](state.pools.length)
  state.pools.par.foreach {
    case p : BasePool[_] ⇒
      makeLBPSIMap(p, lbpsiMap, 1, { s ⇒ state.poolByName(s).staticSize })
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

  // we have to buffer the data chunk before writing it
  val dataChunk = new OutBuffer();

  // @note performance hack: requires at least 1 instance in order to work correctly
  @inline def genericPutField(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration, instances : Iterable[SkillType]) {
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
        val outData = ${
          if (null == d.getSuperType) "p.data"
          else s"WrappedArray.make[_root_.${packagePrefix}${d.getCapitalName}](p.data).view(p.blockInfos.last.bpsi.toInt - 1, p.blockInfos.last.bpsi.toInt - 1 + p.blockInfos.last.count.toInt)"
        }
        val fields = p.fields

        string("$sName", out)
        ${
          if (null == d.getSuperType) "out.put(0.toByte)"
          else s"""string("${d.getSuperType.getSkillName}", out)
        out.v64(lbpsiMap(p.poolIndex.toInt))"""
        }
        out.v64(outData.length)
        restrictions(p, out)

        out.${
          if (fields.size < 127) s"put(${fields.size}.toByte)"
          else s"v64(${fields.size})"
        }
${
          if (fields.size != 0) s"""
        for (f ← fields if p.knownFields.contains(f.name)) {
          restrictions(f, out)
          writeType(f.t, out)
          string(f.name, out)

          // data
          f.name match {${
            (for (f ← fields) yield s"""
            case "${f.getSkillName()}" ⇒ locally {
              ${writeField(d, f)}
            }""").mkString("")
          }
          }
          // end
          v64(dataChunk.size, out)
        }"""
          else ""
        }"""
      }
      ).mkString("")
    }
      case p : StoragePool[SkillType, SkillType] @unchecked ⇒ locally {
        // generic write
        string(p.name, out)
        p.superName match {
          case Some(sn) ⇒
            string(sn, out)
            v64(lbpsiMap(p.poolIndex.toInt), out)
          case None ⇒
            out.put(0.toByte)
        }
        v64(p.dynamicSize, out)
        restrictions(p, out)

        v64(p.fields.size, out)
        for (f ← p.fields) {
          restrictions(f, out)
          writeType(f.t, out)
          string(f.name, out)

          // data
          if (p.dynamicSize > 0) {
            genericPutField(p, f, p)
          }

          // end
          v64(dataChunk.size, out)
        }
      }
    }
  }
  out.putAll(dataChunk)

  out.close
}
""")

    //class prefix
    out.close()
  }
}
