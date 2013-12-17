/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.parsers

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait FieldParserMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/parsers/FieldParser.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.parsers

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import ${packagePrefix}api.SkillType
import ${packagePrefix}internal._
import ${packagePrefix}internal.pool.KnownPool

/**
 * The field parser is able to turn field data from a type block data chunk into an array of field entries
 */
final class FieldParser(val σ: SerializableState) extends ByteStreamParsers {
  val in: ByteReader = σ.fromReader

  /**
   * reads a single element of a field; some type such as constants can not appear here
   */
  private[this] def readSingleField(t: TypeInfo): Any = t match {

    case I8Info  ⇒ in.next
    case I16Info ⇒ in.i16
    case I32Info ⇒ in.i32
    case I64Info ⇒ in.i64
    case V64Info ⇒ in.v64
    case AnnotationInfo ⇒ (in.v64, in.v64) match {
      case (0L, _) ⇒ null
      case (t, i)  ⇒ σ.pools(σ.String.get(t)).getByID(i)
    }
    case BoolInfo ⇒ in.next != 0
    case F32Info  ⇒ in.f32
    case F64Info  ⇒ in.f64
    case StringInfo ⇒ in.v64 match {
      case 0 ⇒ null
      case i ⇒ σ.String.get(i)
    }

    //maps are rather complicated
    case d: MapInfo ⇒ parseMap(d.groundType)

    // user types are just references, easy pray
    case d: UserType ⇒ v64 ^^ {
      _ match {
        case 0 ⇒ null
        case index ⇒ σ.pools.get(d.name).getOrElse(
          throw new IllegalStateException("Found a nonnull reference to missing usertype "+d.name)
        // note: the cast is safe, because the generator requires us to have a declaration for a used user type
        ).asInstanceOf[KnownPool[_, _]].getByID(index)
      }

    }

    case _ ⇒ throw new IllegalStateException(
      "preliminary usertypes and constants should already have been eleminitad")
  }

  /**
   * if d is a map<T,U,V>, the result is a Parser[Map[T,Map[U,V]]]
   */
  private[this] def parseMap(types: List[TypeInfo]): Any = types match {
    case t :: Nil ⇒ readSingleField(t)
    case t :: ts ⇒ {
      val result = new HashMap[Any, Any]
      for (i ← 0 until in.v64.toInt)
        result.put(readSingleField(t), parseMap(ts))
      result
    }
  }

  /**
   * Reads an array of single fields of type t.
   */
  private[this] def readArray[T](size: Long, t: TypeInfo): ArrayBuffer[T] = {
    val result = new ArrayBuffer[T](size.toInt)
    for (i ← 0 until result.length) {
      result(i) = readSingleField(t).asInstanceOf[T]
    }
    result
  }

  def readI8s(length: Long, data: ListBuffer[ChunkInfo]): Array[Byte] = {
    val buff = ByteBuffer.allocate(length.toInt);

    data.foreach { chunk ⇒
      in.push(chunk.begin)
      buff.limit(chunk.end.toInt)
      in.fill(buff)

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "i8")

      in.pop
    }
    buff.array
  }

  def readI16s(length: Long, data: ListBuffer[ChunkInfo]): Array[Short] = {
    val buff = ByteBuffer.allocate(2 * length.toInt)

    data.foreach { chunk ⇒
      in.push(chunk.begin)
      buff.limit(chunk.end.toInt)
      in.fill(buff)

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "i16")

      in.pop
    }
    val result = new Array[Short](length.toInt)
    buff.asShortBuffer().get(result)
    result
  }

  def readI32s(length: Long, data: ListBuffer[ChunkInfo]): Array[Int] = {
    val buff = ByteBuffer.allocate(4 * length.toInt)

    data.foreach { chunk ⇒
      in.push(chunk.begin)
      buff.limit(chunk.end.toInt)
      in.fill(buff)

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "i32")

      in.pop
    }
    val result = new Array[Int](length.toInt)
    buff.asIntBuffer().get(result)
    result
  }

  def readI64s(length: Long, data: ListBuffer[ChunkInfo]): Array[Long] = {
    val buff = ByteBuffer.allocate(8 * length.toInt)

    data.foreach { chunk ⇒
      in.push(chunk.begin)
      buff.limit(chunk.end.toInt)
      in.fill(buff)

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "i64")

      in.pop
    }
    val result = new Array[Long](length.toInt)
    buff.asLongBuffer().get(result)
    result
  }

  def readV64s(length: Long, data: ListBuffer[ChunkInfo]): Array[Long] = {
    val result = new Array[Long](length.toInt)
    var index = 0

    data.foreach { chunk ⇒
      in.push(chunk.begin)

      for (i ← index until index + chunk.count.toInt) {
        result(i) = in.v64
      }
      index += chunk.count.toInt

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "v64")

      in.pop
    }
    result
  }

  def readAnnotations(length: Long, data: ListBuffer[ChunkInfo]): Iterator[SkillType] = {
    val result = new Array[SkillType](length.toInt)
    var index = 0

    data.foreach { chunk ⇒
      in.push(chunk.begin)

      for (i ← index until index + chunk.count.toInt) {
        result(i) = (in.v64, in.v64) match {
          case (0L, _) ⇒ null
          case (t, i)  ⇒ σ.pools(σ.String.get(t)).getByID(i)
        }
      }
      index += chunk.count.toInt

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "annotation")

      in.pop
    }
    result.iterator
  }

  def readBools(length: Long, data: ListBuffer[ChunkInfo]): Iterator[Boolean] = {
    val buff = ByteBuffer.allocate(length.toInt)

    data.foreach { chunk ⇒
      in.push(chunk.begin)
      buff.limit(chunk.end.toInt)
      in.fill(buff)

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "bool")

      in.pop
    }

    buff.rewind()

    new Iterator[Boolean] {
      val data = buff
      override def hasNext = data.hasRemaining
      override def next = 0 != data.get
    }
  }

  def readF32s(length: Long, data: ListBuffer[ChunkInfo]): Iterator[Float] = {
    val buff = ByteBuffer.allocate(4 * length.toInt)

    data.foreach { chunk ⇒
      in.push(chunk.begin)
      buff.limit(chunk.end.toInt)
      in.fill(buff)

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "f32")

      in.pop
    }

    buff.rewind()

    new Iterator[Float] {
      val data = buff.asFloatBuffer()
      override def hasNext = data.hasRemaining
      override def next = data.get
    }
  }

  def readF64s(length: Long, data: ListBuffer[ChunkInfo]): Iterator[Double] = {
    val buff = ByteBuffer.allocate(8 * length.toInt)

    data.foreach { chunk ⇒
      in.push(chunk.begin)
      buff.limit(chunk.end.toInt)
      in.fill(buff)

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "f64")

      in.pop
    }

    buff.rewind()

    new Iterator[Double] {
      val data = buff.asDoubleBuffer()
      override def hasNext = data.hasRemaining
      override def next = data.get
    }
  }

  def readStrings(length: Long, data: ListBuffer[ChunkInfo]): Iterator[String] = {
    val result = new Array[String](length.toInt)
    var index = 0

    data.foreach { chunk ⇒
      in.push(chunk.begin)

      for (i ← index until index + chunk.count.toInt) {
        result(i) = in.v64 match {
          case 0     ⇒ null
          case index ⇒ σ.String.get(index)
        }
      }
      index += chunk.count.toInt

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "annotation")

      in.pop
    }
    result.iterator
  }

  def readUserRefs[T <: SkillType](name: String, result: Array[T], data: ListBuffer[ChunkInfo]) {
    var index = 0
    val pool = σ.pools.get(name).getOrElse(
      throw new IllegalStateException("Found a nonnull reference to missing usertype "+name)
    // note: the cast is safe, because the generator requires us to have a declaration for a used user type
    ).asInstanceOf[KnownPool[T, _]]

    data.foreach { chunk ⇒
      in.push(chunk.begin)

      for (i ← index until index + chunk.count.toInt) {
        result(i) = in.v64 match {
          case 0     ⇒ null.asInstanceOf[T]
          case index ⇒ pool.getByID(index)
        }
      }
      index += chunk.count.toInt

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "annotation")

      in.pop
    }
  }

  def readConstantLengthArrays[T](t: ConstantLengthArrayInfo, length: Long, data: ListBuffer[ChunkInfo]): Iterator[ArrayBuffer[T]] = {
    val result = new ArrayBuffer[ArrayBuffer[T]](length.toInt)
    var index = 0

    data.foreach { chunk ⇒
      in.push(chunk.begin)

      for (i ← index until index + chunk.count.toInt) {
        result(i) = readArray(t.length, t.groundType)
      }
      index += chunk.count.toInt

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "annotation")

      in.pop
    }
    result.iterator
  }

  def readVariableLengthArrays[T](t: VariableLengthArrayInfo, length: Long, data: ListBuffer[ChunkInfo]): Iterator[ArrayBuffer[T]] = {
    val result = new ArrayBuffer[ArrayBuffer[T]](length.toInt)
    var index = 0

    data.foreach { chunk ⇒
      in.push(chunk.begin)

      for (i ← index until index + chunk.count.toInt) {
        result(i) = readArray(in.v64, t.groundType)
      }
      index += chunk.count.toInt

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "annotation")

      in.pop
    }
    result.iterator
  }

  def readLists[T](t: ListInfo, length: Long, data: ListBuffer[ChunkInfo]): Iterator[List[T]] = {
    val result = new ArrayBuffer[List[T]](length.toInt)
    var index = 0

    data.foreach { chunk ⇒
      in.push(chunk.begin)

      for (i ← index until index + chunk.count.toInt) {
        result(i) = readArray(in.v64, t.groundType).toList
      }
      index += chunk.count.toInt

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "annotation")

      in.pop
    }
    result.iterator
  }

  def readSets[T](t: SetInfo, length: Long, data: ListBuffer[ChunkInfo]): Iterator[Set[T]] = {
    val result = new ArrayBuffer[Set[T]](length.toInt)
    var index = 0

    data.foreach { chunk ⇒
      in.push(chunk.begin)

      for (i ← index until index + chunk.count.toInt) {
        result(i) = readArray(in.v64, t.groundType).toSet
      }
      index += chunk.count.toInt

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "annotation")

      in.pop
    }
    result.iterator
  }

  def readMaps[T](t: MapInfo, length: Long, data: ListBuffer[ChunkInfo]): Iterator[T] = {
    val result = new ArrayBuffer[T](length.toInt)
    var index = 0

    data.foreach { chunk ⇒
      in.push(chunk.begin)

      for (i ← index until index + chunk.count.toInt) {
        result(i) = readSingleField(t).asInstanceOf[T]
      }
      index += chunk.count.toInt

      // ensure the data chunk had the expected size
      if (in.position != chunk.end)
        throw PoolSizeMissmatchError(in.position-chunk.begin, chunk.end-chunk.begin, "annotation")

      in.pop
    }
    result.iterator
  }
}
""")

    //class prefix
    out.close()
  }
}
