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

import ${packagePrefix}api.Access
import ${packagePrefix}api.StringAccess
import ${packagePrefix}internal._
import ${packagePrefix}internal.streams.InStream

/**
 * The field parser is able to turn field data from a type block data chunk into an array of field entries
 */
object FieldParser {
  /**
   * Parse a field assuming that in is at the right position and the last chunk of f is to be processed.
   */
  def parseThisField(in : InStream, t : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration, pools : HashMap[String, StoragePool[_ <: SkillType, _ <: SkillType]], String : StringAccess) {
    @inline def readSingleField(t : FieldType) : Any = t match {
      case I8  ⇒ in.i8
      case I16 ⇒ in.i16
      case I32 ⇒ in.i32
      case I64 ⇒ in.i64
      case V64 ⇒ in.v64
      case Annotation ⇒ (in.v64, in.v64) match {
        case (0L, _) ⇒ null
        case (t, i)  ⇒ pools(String.get(t)).getByID(i)
      }
      case BoolType   ⇒ in.i8 != 0
      case F32        ⇒ in.f32
      case F64        ⇒ in.f64
      case StringType ⇒ String.get(in.v64)

      case d : ConstantLengthArray ⇒
        (for (i ← 0 until d.length.toInt)
          yield readSingleField(d.groundType)).to[ArrayBuffer]

      case d : VariableLengthArray ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[ArrayBuffer]

      case d : SetType ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[HashSet]

      case d : ListType ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[ListBuffer]

      // map parsing is recursive on the ground types
      case d : MapType ⇒ parseMap(d.groundType)

      // user types are just references, easy pray
      case d : StoragePool[_, _] ⇒ in.v64 match {
        case 0     ⇒ null
        case index ⇒ d.getByID(index)
      }

      case d ⇒ throw new IllegalStateException("unsupported or unexpected type: "+d)
    }
    /**
     * if d is a map<T,U,V>, the result is a Parser[Map[T,Map[U,V]]]
     */
    @inline def parseMap(types : Seq[FieldType]) : Any =
      if (1 == types.size) readSingleField(types.head)
      else {
        val result = new HashMap[Any, Any]
        for (i ← 0 until in.v64.toInt)
          result.put(readSingleField(types.head), parseMap(types.tail))
        result
      }

    val c = f.dataChunks.last
    if (in.position != c.begin)
      throw new SkillException("@begin of data chunk: expected position(0x$${in.position.toHexString}) to be 0x$${c.begin.toHexString}")

    t match {
${
      (for (t ← IR)
        yield s"""      case p : ${t.getCapitalName}StoragePool ⇒ f.name match {
${
        (for (f ← t.getAllFields)
          yield s"""        case "${f.getSkillName}" ⇒"""+(
          if (f.isIgnored) " in.jump(c.end)"
          else if (f.isConstant) ""
          else readField(f)
        )
        ).mkString("\n")
      }
        case _ ⇒
          c match {
            case c : SimpleChunkInfo ⇒
              for (i ← c.bpsi until c.bpsi + c.count)
                t.getByID(i + 1).set(t.asInstanceOf[Access[SkillType]], f, readSingleField(f.t))

            case bci : BulkChunkInfo ⇒
              for (
                bi ← t.blockInfos;
                i ← bi.bpsi until bi.bpsi + bi.count
              ) t.getByID(i + 1).set(t.asInstanceOf[Access[SkillType]], f, readSingleField(f.t))
          }
      }""").mkString("\n")
    }
      case _ ⇒
        c match {
          case c : SimpleChunkInfo ⇒
            for (i ← c.bpsi until c.bpsi + c.count)
              t.getByID(i + 1).set(t.asInstanceOf[Access[SkillType]], f, readSingleField(f.t))

          case bci : BulkChunkInfo ⇒
            for (
              bi ← t.blockInfos;
              i ← bi.bpsi until bi.bpsi + bi.count
            ) t.getByID(i + 1).set(t.asInstanceOf[Access[SkillType]], f, readSingleField(f.t))
        }
    }

    if (in.position != c.end)
      throw PoolSizeMissmatchError(c.end - c.begin, in.position - c.begin, f.t.toString)
  }

  /**
   * Reads an array of single fields of type t.
   */
  private[this] def readArray[T](size : Long, t : FieldType, in : InStream, pools : HashMap[String, StoragePool[_, _ <: SkillType]], strings : ArrayBuffer[String]) : ArrayBuffer[T] = {
    @inline def readSingleField(t : FieldType) : Any = t match {
      case I8  ⇒ in.i8
      case I16 ⇒ in.i16
      case I32 ⇒ in.i32
      case I64 ⇒ in.i64
      case V64 ⇒ in.v64
      case Annotation ⇒ (in.v64, in.v64) match {
        case (0L, _) ⇒ null
        case (t, i)  ⇒ pools(strings(t.toInt)).getByID(i)
      }
      case BoolType   ⇒ in.i8 != 0
      case F32        ⇒ in.f32
      case F64        ⇒ in.f64
      case StringType ⇒ strings(in.v64.toInt)

      case d : ConstantLengthArray ⇒
        (for (i ← 0 until d.length.toInt)
          yield readSingleField(d.groundType)).to[ArrayBuffer]

      case d : VariableLengthArray ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[ArrayBuffer]

      case d : SetType ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[HashSet]

      case d : ListType ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[ListBuffer]

      // map parsing is recursive on the ground types
      case d : MapType ⇒ parseMap(d.groundType)

      // user types are just references, easy pray
      case d : StoragePool[_, _] ⇒ in.v64 match {
        case 0     ⇒ null
        case index ⇒ pools(d.name).getByID(index)
      }

      case d ⇒ throw new IllegalStateException("unsupported or unexpected type: "+d)
    }
    /**
     * if d is a map<T,U,V>, the result is a Parser[Map[T,Map[U,V]]]
     */
    @inline def parseMap(types : Seq[FieldType]) : Any =
      if (1 == types.size) readSingleField(types.head)
      else {
        val result = new HashMap[Any, Any]
        for (i ← 0 until in.v64.toInt)
          result.put(readSingleField(types.head), parseMap(types.tail))
        result
      }

    val result = new ArrayBuffer[T](size.toInt)
    for (i ← 0 until size.toInt) {
      result += readSingleField(t).asInstanceOf[T]
    }
    result
  }
}
""")

    //class prefix
    out.close()
  }

  private def readField(f: Field): String = {
    val t = f.getType
    val (prelude, action, result) = readSingleField(t)
    s"""$prelude
          c match {
            case c : SimpleChunkInfo ⇒
              for (i ← c.bpsi until c.bpsi + c.count) {$action
                p.getByID(i + 1).${f.getName} = $result
              }

            case bci : BulkChunkInfo ⇒
              for (
                bi ← t.blockInfos;
                i ← bi.bpsi until bi.bpsi + bi.count
              ) {$action
                p.getByID(i + 1).${f.getName} = $result
              }
          }"""
  }

  private def readSingleField(t: Type): (String, String, String) = t match {
    case t: Declaration ⇒ (s"""
          val ref = pools("${t.getSkillName}").asInstanceOf[${t.getCapitalName}StoragePool]""", "", "ref.getByID(in.v64)")
    case t: GroundType ⇒ ("", "", t.getSkillName match {
      case "annotation" ⇒ """(in.v64, in.v64) match {
                case (0L, _) ⇒ null
                case (t, i)  ⇒ pools(String.get(t)).getByID(i)
              }"""
      case "string" ⇒ "String.get(in.v64)"
      case n        ⇒ "in."+n
    })

    case t: SetType ⇒
      val (p, r, s) = readSingleField(t.getBaseType)
      (p, s"""
                val r = new HashSet[${mapType(t.getBaseType)}]
                val count = in.v64.toInt
                r.sizeHint(count)
                for (i ← 0 until count) r.add($s)""", "r")

    case _ ⇒ ("", "", "readSingleField(f.t).asInstanceOf["+mapType(t)+"]")
  }

}
