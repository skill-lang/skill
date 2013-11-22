/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait SerializationFunctionsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/SerializationFunctions.scala")
    //package
    out.write(s"""package ${packagePrefix}internal

import java.nio.ByteBuffer

import ${packagePrefix}api.SkillType
import ${packagePrefix}internal.pool.KnownPool

/**
 * Provides serialization functions;
 *
 * @see SKilL §6.4
 * @author Timm Felden
 */
abstract class SerializationFunctions(state: SerializableState) {
  import SerializationFunctions._

  def annotation(ref: SkillType): List[Array[Byte]]

  def string(v: String): Array[Byte] = v64(state.strings.serializationIDs(v))
}

object SerializationFunctions {

  def bool(v: Boolean): Array[Byte] = new Array(if (v) 0xFF else 0x00)

  def i8(v: Byte): Array[Byte] = new Array(v)

  def i16(v: Short): Array[Byte] = ByteBuffer.allocate(2).putShort(v).array
  def i32(v: Int): Array[Byte] = ByteBuffer.allocate(4).putInt(v).array
  @inline def i64(v: Long): Array[Byte] = ByteBuffer.allocate(8).putLong(v).array

  /**
   *  encode a v64 value into a stream
   *  @param v the value to be encoded
   *  @param out the array, where the encoded value is stored
   *  @param offset the first index to be used to encode v
   *  @result the number of bytes used to encode v
   *  @note usage: "∀ v. offset += (v, out, offset)"
   */
  @inline def v64(v: Long, out: Array[Byte], offset: Int): Int = {
    if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
      out(offset) = v.toByte;
      return 1;
    } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
      out(offset + 0) = (0x80L | v).toByte
      out(offset + 1) = (v >> 7).toByte
      return 2;
    } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
      out(offset + 0) = (0x80L | v).toByte
      out(offset + 1) = (0x80L | v >> 7).toByte
      out(offset + 2) = (v >> 14).toByte
      return 3;
    } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
      out(offset + 0) = (0x80L | v).toByte
      out(offset + 1) = (0x80L | v >> 7).toByte
      out(offset + 2) = (0x80L | v >> 14).toByte
      out(offset + 3) = (v >> 21).toByte
      return 4;
    } else if (0L == (v & 0xFFFFFFF800000000L)) {
      out(offset + 0) = (0x80L | v).toByte
      out(offset + 1) = (0x80L | v >> 7).toByte
      out(offset + 2) = (0x80L | v >> 14).toByte
      out(offset + 3) = (0x80L | v >> 21).toByte
      out(offset + 4) = (v >> 28).toByte
      return 5;
    } else if (0L == (v & 0xFFFFFC0000000000L)) {
      out(offset + 0) = (0x80L | v).toByte
      out(offset + 1) = (0x80L | v >> 7).toByte
      out(offset + 2) = (0x80L | v >> 14).toByte
      out(offset + 3) = (0x80L | v >> 21).toByte
      out(offset + 4) = (0x80L | v >> 28).toByte
      out(offset + 5) = (v >> 35).toByte
      return 6;
    } else if (0L == (v & 0xFFFE000000000000L)) {
      out(offset + 0) = (0x80L | v).toByte
      out(offset + 1) = (0x80L | v >> 7).toByte
      out(offset + 2) = (0x80L | v >> 14).toByte
      out(offset + 3) = (0x80L | v >> 21).toByte
      out(offset + 4) = (0x80L | v >> 28).toByte
      out(offset + 5) = (0x80L | v >> 35).toByte
      out(offset + 6) = (v >> 42).toByte
      return 7;
    } else if (0L == (v & 0xFF00000000000000L)) {
      out(offset + 0) = (0x80L | v).toByte
      out(offset + 1) = (0x80L | v >> 7).toByte
      out(offset + 2) = (0x80L | v >> 14).toByte
      out(offset + 3) = (0x80L | v >> 21).toByte
      out(offset + 4) = (0x80L | v >> 28).toByte
      out(offset + 5) = (0x80L | v >> 35).toByte
      out(offset + 6) = (0x80L | v >> 42).toByte
      out(offset + 7) = (v >> 49).toByte
      return 8;
    } else {
      out(offset + 0) = (0x80L | v).toByte
      out(offset + 1) = (0x80L | v >> 7).toByte
      out(offset + 2) = (0x80L | v >> 14).toByte
      out(offset + 3) = (0x80L | v >> 21).toByte
      out(offset + 4) = (0x80L | v >> 28).toByte
      out(offset + 5) = (0x80L | v >> 35).toByte
      out(offset + 6) = (0x80L | v >> 42).toByte
      out(offset + 7) = (0x80L | v >> 49).toByte
      out(offset + 8) = (v >> 56).toByte
      return 9;
    }
  }

  /**
   * @param v the value to be encoded
   * @result returns the encoded value as new array
   * @note do not use this method for encoding of field data!
   */
  def v64(v: Long): Array[Byte] = {
    if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
      val r = new Array[Byte](1)
      r(0) = v.toByte
      r
    } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
      val r = new Array[Byte](2)
      r(0) = (0x80L | v).toByte
      r(1) = (v >> 7).toByte
      r
    } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
      val r = new Array[Byte](3)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (v >> 14).toByte
      r
    } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
      val r = new Array[Byte](4)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (0x80L | v >> 14).toByte
      r(3) = (v >> 21).toByte
      r
    } else if (0L == (v & 0xFFFFFFF800000000L)) {
      val r = new Array[Byte](5)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (0x80L | v >> 14).toByte
      r(3) = (0x80L | v >> 21).toByte
      r(4) = (v >> 28).toByte
      r
    } else if (0L == (v & 0xFFFFFC0000000000L)) {
      val r = new Array[Byte](6)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (0x80L | v >> 14).toByte
      r(3) = (0x80L | v >> 21).toByte
      r(4) = (0x80L | v >> 28).toByte
      r(5) = (v >> 35).toByte
      r
    } else if (0L == (v & 0xFFFE000000000000L)) {
      val r = new Array[Byte](7)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (0x80L | v >> 14).toByte
      r(3) = (0x80L | v >> 21).toByte
      r(4) = (0x80L | v >> 28).toByte
      r(5) = (0x80L | v >> 35).toByte
      r(6) = (v >> 42).toByte
      r
    } else if (0L == (v & 0xFF00000000000000L)) {
      val r = new Array[Byte](8)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (0x80L | v >> 14).toByte
      r(3) = (0x80L | v >> 21).toByte
      r(4) = (0x80L | v >> 28).toByte
      r(5) = (0x80L | v >> 35).toByte
      r(6) = (0x80L | v >> 42).toByte
      r(7) = (v >> 49).toByte
      r
    } else {
      val r = new Array[Byte](9)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (0x80L | v >> 14).toByte
      r(3) = (0x80L | v >> 21).toByte
      r(4) = (0x80L | v >> 28).toByte
      r(5) = (0x80L | v >> 35).toByte
      r(6) = (0x80L | v >> 42).toByte
      r(7) = (0x80L | v >> 49).toByte
      r(8) = (v >> 56).toByte
      r
    }
  }

  def f32(v: Float): Array[Byte] = ByteBuffer.allocate(4).putFloat(v).array
  def f64(v: Double): Array[Byte] = ByteBuffer.allocate(8).putDouble(v).array
}
""")

    //class prefix
    out.close()
  }
}
