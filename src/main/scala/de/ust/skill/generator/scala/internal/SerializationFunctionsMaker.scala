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
class SerializationFunctions(state: SerializableState) {
  import SerializationFunctions._

  def annotation(ref: SkillType, ws: WriteState): List[Array[Byte]] = {
    val baseName = state.pools(ref.getClass.getSimpleName.toLowerCase).asInstanceOf[KnownPool[_, _]].basePool.name
    val refID = ws.getByRef(baseName, ref)

    List(v64(state.strings.serializationIDs(baseName)), v64(refID))
  }

  def string(v: String): Array[Byte] = v64(state.strings.serializationIDs(v))
}

object SerializationFunctions {

  def bool(v: Boolean): Array[Byte] = new Array(if (v) 0xFF else 0x00)

  def i8(v: Byte): Array[Byte] = new Array(v)

  def i16(v: Short): Array[Byte] = ByteBuffer.allocate(2).putShort(v).array
  def i32(v: Int): Array[Byte] = ByteBuffer.allocate(4).putInt(v).array
  def i64(v: Long): Array[Byte] = ByteBuffer.allocate(8).putLong(v).array

  /**
   *  encode a v64 value into a stream
   */
  def v64(v: Long): Array[Byte] = {
    // calculate effective size
    var size = 0;
    {
      var q = v;
      while (q != 0) {
        q >>>= 7;
        size += 1;
      }
    }
    if (0 == size) {
      val rval = new Array[Byte](1);
      rval(0) = 0;
      return rval;
    } else if (10 == size)
      size = 9;

    // split
    val rval = new Array[Byte](size);
    var count = 0;
    while (count < 8 && count < size - 1) {
      rval(count) = (v >> (7 * count)).asInstanceOf[Byte];
      rval(count) = (rval(count) | 0x80).asInstanceOf[Byte];
      count += 1;
    }
    rval(count) = (v >> (7 * count)).asInstanceOf[Byte];
    return rval;
  }

  def f32(v: Float): Array[Byte] = ByteBuffer.allocate(4).putFloat(v).array
  def f64(v: Double): Array[Byte] = ByteBuffer.allocate(8).putDouble(v).array
}
""")

    //class prefix
    out.close()
  }
}
