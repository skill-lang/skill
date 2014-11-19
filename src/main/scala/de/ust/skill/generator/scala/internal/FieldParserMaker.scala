/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal
import de.ust.skill.generator.scala.GeneralOutputMaker
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.ir.Type
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Field

trait FieldParserMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/FieldParser.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.WrappedArray

import _root_.${packagePrefix}api.Access
import _root_.${packagePrefix}api.StringAccess
import _root_.${packagePrefix}internal._
import _root_.${packagePrefix}internal.streams.InStream

/**
 * The field parser is able to turn field data from a type block data chunk into an array of field entries
 */
object FieldParser {
  /**
   * Parse a field assuming that in is at the right position and the last chunk of f is to be processed.
   * This function is expected to perform bulk read optimizations wherever possible.
   */
  def parseThisField[T](in : InStream, t : StoragePool[_ <: SkillType, _ <: SkillType], f : KnownField[T]) {

    val c = f.lastChunk
    if (in.position != c.begin)
      throw new SkillException(
        "@begin of data chunk: expected position(0x$${in.position.toHexString}) to be 0x$${c.begin.toHexString}"
      )

    ${
      //TODO turn this thing into an option, because users may get annoyed by intended partial fields
      "// partial fields, refer to TR14§???"
    }
    if (0 != c.count && c.begin == c.end && f.t.typeID > 4) {
      System.err.println(s"[SKilL TR14] detected partial field: $$f");
      return
    }

    t match {
${
      (for (t ← IR)
        yield s"""      case p : ${t.getName.capital}StoragePool ⇒
        val d = p.data
        f.name match {
${
        (for (f ← t.getAllFields)
          yield s"""          case "${f.getSkillName}" ⇒"""+(
          if (f.isIgnored) " in.jump(c.end)"
          else if (f.isConstant) ""
          else readField(f)
        )
        ).mkString("\n")
      }
          case _ ⇒
            c match {
              case c : SimpleChunkInfo ⇒
                val low = c.bpo.toInt
                val high = (c.bpo + c.count).toInt
                for (i ← low until high)
                  d(i).set(f, f.t.readSingleField(in))

              case bci : BulkChunkInfo ⇒
                for (
                  bi ← t.blockInfos;
                  i ← bi.bpo.toInt until (bi.bpo + bi.count).toInt
                ) d(i).set(f, f.t.readSingleField(in))
            }
        }""").mkString("\n")
    }
      case _ ⇒
        c match {
          case c : SimpleChunkInfo ⇒
            val low = c.bpo.toInt
            val high = (c.bpo + c.count).toInt
            for (i ← low until high)
              t.getByID(i + 1).set(f, f.t.readSingleField(in))

          case bci : BulkChunkInfo ⇒
            for (
              bi ← t.blockInfos;
              i ← bi.bpo.toInt until (bi.bpo + bi.count).toInt
            ) t.getByID(i + 1).set(f, f.t.readSingleField(in))
        }
    }

    if (in.position != c.end)
      throw PoolSizeMissmatchError(c.end - c.begin, in.position - c.begin, f.t.toString)
  }

  /**
   * Reads an array of single fields of type t.
   */
  private[this] def readArray[T](size : Long, t : FieldType[T], in : InStream) : ArrayBuffer[T] = {
    val result = new ArrayBuffer[T](size.toInt)
    for (i ← 0 until size.toInt) {
      result += t.readSingleField(in)
    }
    result
  }
}
""")

    //class prefix
    out.close()
  }

  private def readField(f : Field) : String = {
    val t = f.getType
    val (prelude, action, result) = readSingleField(t)
    s"""$prelude
            c match {
              case c : SimpleChunkInfo ⇒
                val low = c.bpo.toInt
                val high = (c.bpo + c.count).toInt
                for (i ← low until high) {$action
                  d(i).${escaped(f.getName.camel)} = $result
                }

              case bci : BulkChunkInfo ⇒
                for (
                  bi ← t.blockInfos;
                  i ← bi.bpo.toInt until (bi.bpo + bi.count).toInt
                ) {$action
                  d(i).${escaped(f.getName.camel)} = $result
                }
            }"""
  }

  private def readSingleField(t : Type) : (String, String, String) = t match {
    case t : Declaration ⇒ (s"""
            val ref = f.t.asInstanceOf[${t.getName.capital}StoragePool]""", "", "ref.getByID(in.v64)")
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ ("val r = f.t.asInstanceOf[Annotation].types", "", """(in.v64, in.v64) match {
                    case (0L, _) ⇒ null
                    case (t, i)  ⇒ r(t.toInt - 1).getByID(i)
                  }""")
      case "string" ⇒ ("val r = f.t.asInstanceOf[StringType].strings", "", "r.get(in.v64)")
      case n        ⇒ ("", "", "in."+n)
    }

    case t : SetType ⇒
      val (p, r, s) = readSingleField(t.getBaseType)
      (p.replace("f.t.", s"f.t.asInstanceOf[SetType[${mapType(t.getBaseType)}]].groundType."), s"""
                  val r = new HashSet[${mapType(t.getBaseType)}]
                  val count = in.v64.toInt
                  r.sizeHint(count)
                  for (i ← 0 until count) r.add($s)""", "r")

    case _ ⇒ ("", "", "f.t.readSingleField(in).asInstanceOf["+mapType(t)+"]")
  }

}
