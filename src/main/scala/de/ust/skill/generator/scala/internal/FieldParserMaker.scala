/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal
import de.ust.skill.generator.scala.GeneralOutputMaker

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
import ${packagePrefix}api.SkillType
import ${packagePrefix}api.StringAccess
import ${packagePrefix}internal._
import ${packagePrefix}internal.streams.InStream
""")
    out.write("""
/**
 * The field parser is able to turn field data from a type block data chunk into an array of field entries
 */
object FieldParser {
  /**
   * Parse a field assuming that in is at the right position and the last chunk of f is to be processed.
   */
  def parseThisField(in: InStream, t: StoragePool[_ <: SkillType], f: FieldDeclaration, pools: HashMap[String, StoragePool[_ <: SkillType]], String: StringAccess) {
    @inline def readSingleField(t: FieldType): Any = t match {
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

      case d: ConstantLengthArray ⇒
        (for (i ← 0 until d.length.toInt)
          yield readSingleField(d.groundType)).to[ArrayBuffer]

      case d: VariableLengthArray ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[ArrayBuffer]

      case d: SetType ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[HashSet]

      case d: ListType ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[ListBuffer]

      // map parsing is recursive on the ground types
      case d: MapType ⇒ parseMap(d.groundType)

      // user types are just references, easy pray
      case d: StoragePool[_] ⇒ in.v64 match {
        case 0     ⇒ null
        case index ⇒ pools(d.name).getByID(index)
      }

      case d ⇒ throw new IllegalStateException("unsupported or unexpected type: "+d)
    }
    /**
     * if d is a map<T,U,V>, the result is a Parser[Map[T,Map[U,V]]]
     */
    @inline def parseMap(types: Seq[FieldType]): Any =
      if (1 == types.size) readSingleField(types.head)
      else {
        val result = new HashMap[Any, Any]
        for (i ← 0 until in.v64.toInt)
          result.put(readSingleField(types.head), parseMap(types.tail))
        result
      }

    val c = f.dataChunks.last
    assert(in.position == c.begin, s"@end of data chunk: expected position(0x${in.position.toHexString}) to be 0x${c.begin.toHexString}")
    c match {
      case c: SimpleChunkInfo ⇒
        for (i ← c.bpsi until c.bpsi + c.count)
          t.getByID(i + 1).set(t.asInstanceOf[Access[SkillType]], f, readSingleField(f.t))

      case bci: BulkChunkInfo ⇒
        for (
          bi ← t.blockInfos;
          i ← bi.bpsi until bi.bpsi + bi.count
        ) t.getByID(i + 1).set(t.asInstanceOf[Access[SkillType]], f, readSingleField(f.t))
    }
    assert(in.position == c.end, s"@end of data chunk: expected position(0x${in.position.toHexString}) to be 0x${c.end.toHexString}")
  }

  /**
   * Reads an array of single fields of type t.
   */
  private[this] def readArray[T](size: Long, t: FieldType, in: InStream, pools: HashMap[String, StoragePool[_ <: SkillType]], strings: ArrayBuffer[String]): ArrayBuffer[T] = {
    @inline def readSingleField(t: FieldType): Any = t match {
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

      case d: ConstantLengthArray ⇒
        (for (i ← 0 until d.length.toInt)
          yield readSingleField(d.groundType)).to[ArrayBuffer]

      case d: VariableLengthArray ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[ArrayBuffer]

      case d: SetType ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[HashSet]

      case d: ListType ⇒
        (for (i ← 0 until in.v64.toInt)
          yield readSingleField(d.groundType)).to[ListBuffer]

      // map parsing is recursive on the ground types
      case d: MapType ⇒ parseMap(d.groundType)

      // user types are just references, easy pray
      case d: StoragePool[_] ⇒ in.v64 match {
        case 0     ⇒ null
        case index ⇒ pools(d.name).getByID(index)
      }

      case d ⇒ throw new IllegalStateException("unsupported or unexpected type: "+d)
    }
    /**
     * if d is a map<T,U,V>, the result is a Parser[Map[T,Map[U,V]]]
     */
    @inline def parseMap(types: Seq[FieldType]): Any =
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

//  def readI8s(in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Array[Byte] = {
//    val buff = ByteBuffer.allocate(length.toInt);
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//      buff.limit(buff.position + chunk.count.toInt)
//      in.fill(buff)
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "i8")
//
//      in.pop
//    }
//    buff.array
//  }
//
//  def readI16s(in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Array[Short] = {
//    val buff = ByteBuffer.allocate(2 * length.toInt)
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//      buff.limit(buff.position + 2 * chunk.count.toInt)
//      in.fill(buff)
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "i16")
//
//      in.pop
//    }
//    val result = new Array[Short](length.toInt)
//    buff.asShortBuffer().get(result)
//    result
//  }
//
//  def readI32s(in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Array[Int] = {
//    val buff = ByteBuffer.allocate(4 * length.toInt)
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//      buff.limit(buff.position + 4 * chunk.count.toInt)
//      in.fill(buff)
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "i32")
//
//      in.pop
//    }
//    val result = new Array[Int](length.toInt)
//    buff.asIntBuffer().get(result)
//    result
//  }
//
//  def readI64s(in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Array[Long] = {
//    val buff = ByteBuffer.allocate(8 * length.toInt)
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//      buff.limit(buff.position + 8 * chunk.count.toInt)
//      in.fill(buff)
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "i64")
//
//      in.pop
//    }
//    val result = new Array[Long](length.toInt)
//    buff.asLongBuffer().get(result)
//    result
//  }
//
//  def readV64s(in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Array[Long] = {
//    val result = new Array[Long](length.toInt)
//    var index = 0
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//
//      for (i ← index until index + chunk.count.toInt) {
//        result(i) = in.v64
//      }
//      index += chunk.count.toInt
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "v64")
//
//      in.pop
//    }
//    result
//  }
//
//  def readAnnotations(in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Iterator[SkillType] = {
//    val result = new Array[SkillType](length.toInt)
//    var index = 0
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//
//      for (i ← index until index + chunk.count.toInt) {
//        result(i) = (in.v64, in.v64) match {
//          case (0L, _) ⇒ null
//          case (t, i)  ⇒ σ.pools(σ.String.get(t)).getByID(i)
//        }
//      }
//      index += chunk.count.toInt
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "annotation")
//
//      in.pop
//    }
//    result.iterator
//  }
//
//  def readBools(in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Iterator[Boolean] = {
//    val buff = ByteBuffer.allocate(length.toInt)
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//      buff.limit(buff.position + chunk.count.toInt)
//      in.fill(buff)
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "bool")
//
//      in.pop
//    }
//
//    buff.rewind()
//
//    new Iterator[Boolean] {
//      val data = buff
//      override def hasNext = data.hasRemaining
//      override def next = 0 != data.get
//    }
//  }
//
//  def readF32s(in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Iterator[Float] = {
//    val buff = ByteBuffer.allocate(4 * length.toInt)
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//      buff.limit(buff.position + 4 * chunk.count.toInt)
//      in.fill(buff)
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "f32")
//
//      in.pop
//    }
//
//    buff.rewind()
//
//    new Iterator[Float] {
//      val data = buff.asFloatBuffer()
//      override def hasNext = data.hasRemaining
//      override def next = data.get
//    }
//  }
//
//  def readF64s(in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Iterator[Double] = {
//    val buff = ByteBuffer.allocate(8 * length.toInt)
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//      buff.limit(buff.position + 8 * chunk.count.toInt)
//      in.fill(buff)
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "f64")
//
//      in.pop
//    }
//
//    buff.rewind()
//
//    new Iterator[Double] {
//      val data = buff.asDoubleBuffer()
//      override def hasNext = data.hasRemaining
//      override def next = data.get
//    }
//  }
//
//  def readStrings(in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Iterator[String] = {
//    val result = new Array[String](length.toInt)
//    var index = 0
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//
//      for (i ← index until index + chunk.count.toInt) {
//        result(i) = in.v64 match {
//          case 0     ⇒ null
//          case index ⇒ σ.String.get(index)
//        }
//      }
//      index += chunk.count.toInt
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "annotation")
//
//      in.pop
//    }
//    result.iterator
//  }
//
//  def readUserRefs[T <: SkillType](name: String, result: Array[T], data: ListBuffer[ChunkInfo]) {
//    var index = 0
//    val pool = σ.pools.get(name).getOrElse(
//      throw new IllegalStateException("Found a nonnull reference to missing usertype "+name)
//    // note: the cast is safe, because the generator requires us to have a declaration for a used user type
//    ).asInstanceOf[KnownPool[T, _]]
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//      try {
//        for (i ← index until index + chunk.count.toInt) {
//          result(i) = in.v64 match {
//            case 0     ⇒ null.asInstanceOf[T]
//            case index ⇒ pool.getByID(index)
//          }
//        }
//        index += chunk.count.toInt
//
//      } catch {
//        case e: SkillException ⇒ throw new SkillException(s"failed to read user refs in chunk $chunk @${in.position}", e)
//      }
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "annotation")
//
//      in.pop
//    }
//  }
//
//  def readConstantLengthArrays[T](t: ConstantLengthArray, in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Iterator[scala.collection.mutable.ArrayBuffer[T]] = {
//    val result = new Array[scala.collection.mutable.ArrayBuffer[T]](length.toInt)
//    var index = 0
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//
//      for (i ← index until index + chunk.count.toInt) {
//        result(i) = readArray(t.length, t.groundType).to
//      }
//      index += chunk.count.toInt
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "annotation")
//
//      in.pop
//    }
//    result.iterator
//  }
//
//  def readVariableLengthArrays[T](t: VariableLengthArray, in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Iterator[scala.collection.mutable.ArrayBuffer[T]] = {
//    val result = new Array[scala.collection.mutable.ArrayBuffer[T]](length.toInt)
//    var index = 0
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//
//      for (i ← index until index + chunk.count.toInt) {
//        result(i) = readArray(in.v64, t.groundType).to
//      }
//      index += chunk.count.toInt
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "annotation")
//
//      in.pop
//    }
//    result.iterator
//  }
//
//  def readLists[T](t: ListType, in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Iterator[scala.collection.mutable.ListBuffer[T]] = {
//    val result = new Array[scala.collection.mutable.ListBuffer[T]](length.toInt)
//    var index = 0
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//
//      for (i ← index until index + chunk.count.toInt) {
//        result(i) = readArray(in.v64, t.groundType).to
//      }
//      index += chunk.count.toInt
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "annotation")
//
//      in.pop
//    }
//    result.iterator
//  }
//
//  def readSets[T](t: SetType, in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Iterator[scala.collection.mutable.HashSet[T]] = {
//    val result = new Array[scala.collection.mutable.HashSet[T]](length.toInt)
//    var index = 0
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//
//      for (i ← index until index + chunk.count.toInt) {
//        result(i) = readArray(in.v64, t.groundType).to
//      }
//      index += chunk.count.toInt
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "annotation")
//
//      in.pop
//    }
//    result.iterator
//  }
//
//  def readMaps[T](t: MapType, in: InStream, length: Long, data: ListBuffer[ChunkInfo]): Iterator[T] = {
//    val result = new ArrayBuffer[T](length.toInt)
//    var index = 0
//
//    for (chunk ← data) {
//      in.push(chunk.begin)
//
//      for (i ← index until index + chunk.count.toInt) {
//        result += readSingleField(t).asInstanceOf[T]
//      }
//      index += chunk.count.toInt
//
//      // ensure the data chunk had the expected size
//      if (in.position != chunk.end)
//        throw PoolSizeMissmatchError(in.position - chunk.begin, chunk.end - chunk.begin, "annotation")
//
//      in.pop
//    }
//    result.iterator
//  }
}
""")

    //class prefix
    out.close()
  }
}
