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
        case (0L, _) ⇒ 0L
        case (t, i)  ⇒ AnnotationRef.build(pools(String.get(t)).poolIndex, i.toInt)
      }
      case BoolType   ⇒ in.i8 != 0
      case F32        ⇒ in.f32
      case F64        ⇒ in.f64
      case StringType ⇒ String.get(in.v64)

      case d : ConstantLengthArray ⇒
        asArray(for (i ← 0 until d.length.toInt)
          yield readSingleField(d.groundType), d.groundType)

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
      case d : StoragePool[_, _] ⇒ in.v64.toInt

      case d ⇒ throw new IllegalStateException("unsupported or unexpected type: "+d)
    }
    /**
     * if d is a constant length array, the result is a correct typed array
     */
    @inline def asArray(data : Seq[Any], t : FieldType) = t match {
        case I8  ⇒ data.asInstanceOf[Seq[Byte]].toArray
        case I16 ⇒ data.asInstanceOf[Seq[Short]].toArray
        case I32 ⇒ data.asInstanceOf[Seq[Int]].toArray
        case I64 ⇒ data.asInstanceOf[Seq[Long]].toArray
        case V64 ⇒ data.asInstanceOf[Seq[Long]].toArray
        case Annotation ⇒ data.asInstanceOf[Seq[Long]].toArray
        case BoolType   ⇒ data.asInstanceOf[Seq[Boolean]].toArray
        case F32        ⇒ data.asInstanceOf[Seq[Float]].toArray
        case F64        ⇒ data.asInstanceOf[Seq[Double]].toArray
        case StringType ⇒ data.asInstanceOf[Seq[String]].toArray
        case _ : StoragePool[_, _] ⇒ data.asInstanceOf[Seq[Int]].toArray
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
      throw new SkillException(s"@begin of data chunk: expected position(0x$${in.position.toHexString}) to be 0x$${c.begin.toHexString}")

    // partial fields, refer to TR14§???
    if (0 != c.count && c.begin == c.end && f.t.typeID > 4) {
      System.err.println(s"[SKilL TR14] detected partial field: $$f");
      return
    }

    t match {
${
      (for (t ← IR)
        yield s"""      case p : ${t.getCapitalName}StoragePool ⇒
        f.name match {
${
        (for (f ← t.getFields)
          yield s"""        case "${f.getSkillName}" ⇒"""+(
          if (f.isIgnored) " in.jump(c.end)"
          else if (f.isConstant) ""
          else readField(f, t.getSuperType != null)
        )
        ).mkString("\n")
      }
        case _ ⇒
          c match {
            case c : SimpleChunkInfo ⇒
              val low = c.bpsi
              val high = c.bpsi + c.count
              for (i ← low until high)
                p.unknownFieldData(f).put(i + 1, readSingleField(f.t))

            case bci : BulkChunkInfo ⇒
              for (
                bi ← t.dynamicBlockInfos;
                i ← bi.bpsi until bi.bpsi + bi.count
              ) p.unknownFieldData(f).put(i + 1, readSingleField(f.t))
          }
      }""").mkString("\n")
    }
      case _ ⇒
        c match {
          case c : SimpleChunkInfo ⇒
            val low = c.bpsi.toInt
            val high = (c.bpsi + c.count).toInt
            for (i ← low until high)
              t.getByID(i + 1).set(t.asInstanceOf[Access[SkillType]], f, readSingleField(f.t))

          case bci : BulkChunkInfo ⇒
            for (
              bi ← t.dynamicBlockInfos;
              i ← bi.bpsi until bi.bpsi + bi.count
            ) t.getByID(i + 1).set(t.asInstanceOf[Access[SkillType]], f, readSingleField(f.t))
        }
    }

    if (in.position != c.end)
      throw PoolSizeMissmatchError(c.end - c.begin, in.position - c.begin, f.t.toString)
  }
}
""")

    //class prefix
    out.close()
  }

  private def readField(f: Field, isSubType : Boolean): String = {
    val t = f.getType
    val (prelude, action, result) = readSingleField(t)
    s"""$prelude
          c match {
            case c : SimpleChunkInfo ⇒
              ${if (isSubType) "val low = p.indexOfOldID(c.bpsi + 1)"
                else           "val low = c.bpsi"}
              val high = low + c.count
              for (i ← low until high) {$action
                p._${f.getName}(i) = $result
              }

            case bci : BulkChunkInfo ⇒
              for (i ← 0 until bci.count) {$action
                p._${f.getName}(i) = $result
              }
          }"""
  }

  private def readSingleField(t: Type): (String, String, String) = t match {
    case t: Declaration ⇒ (s"", "", "in.v64.toInt")
    case t: GroundType ⇒ ("", "", t.getSkillName match {
      case "annotation" ⇒ """(in.v64, in.v64) match {
                case (0L, _) ⇒ 0L
                case (t, i)  ⇒ AnnotationRef.build(pools(String.get(t.toInt)).poolIndex, i.toInt)
              }"""
      case "string" ⇒ "String.get(in.v64.toInt)"
      case n        ⇒ "in."+n
    })

    case t: SetType ⇒
      val (p, r, s) = readSingleField(t.getBaseType)
      (p, s"""
                val r = new HashSet[${mapTypeRepresentation(t.getBaseType)}]
                val count = in.v64.toInt
                r.sizeHint(count)
                for (i ← 0 until count) r.add($s)""", "r")

    case _ ⇒ ("", "", "readSingleField(f.t).asInstanceOf["+mapTypeRepresentation(t)+"]")
  }

}
