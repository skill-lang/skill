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
trait SerializationFunctionsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/SerializationFunctions.scala")
    //package
    out.write(s"""package ${packagePrefix}internal

import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import ${packagePrefix}api.SkillType
import ${packagePrefix}internal.streams.OutStream

/**
 * Provides serialization functions;
 *
 * @see SKilL §6.4
 * @author Timm Felden
 */
abstract class SerializationFunctions(state: SerializableState) {
  import SerializationFunctions._

  //collect String instances from known string types; this is needed, because strings are something special on the jvm
  //@note this is a O(σ) operation:)
  //@note we do no longer make use of the generation time type info, because we want to treat generic fields as well
  locally {
    val strings = state.String
    for (p ← state.pools) {
      strings.add(p.name)
      for (f ← p.fields) {
        strings.add(f.name)
        if (StringType == f.t) {
          for (i ← p.all)
            strings.add(i.get(p, f).asInstanceOf[String])
        }
      }
    }
  }

  val stringIDs = new HashMap[String, Long]

  def annotation(ref: SkillType, out: OutStream): Unit

  def string(v: String, out: OutStream): Unit = v64(stringIDs(v), out)

  val skillIDs: HashMap[SkillType, Long]

  @inline def userRef[T <: SkillType](ref: T, out: OutStream) {
    if (null == ref) out.put(0.toByte)
    else v64(skillIDs(ref), out)
  }
}

object SerializationFunctions {

  @inline def bool(v: Boolean): Array[Byte] = Array[Byte](if (v) -1 else 0)

  @inline def bool(v: Boolean, out: OutStream) = out.put(if (v) -1.toByte else 0.toByte)

  @inline def i8(v: Byte, out: OutStream) = out.put(v)
  @inline def i16(v: Short, out: OutStream) = out.put(ByteBuffer.allocate(2).putShort(v).array)
  @inline def i32(v: Int, out: OutStream) = out.put(ByteBuffer.allocate(4).putInt(v).array)
  @inline def i64(v: Long, out: OutStream) = out.put(ByteBuffer.allocate(8).putLong(v).array)

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
  @inline def v64(v: Long, out: OutStream) {
    if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
      out.put(v.toByte)
    } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
      val r = new Array[Byte](2)
      r(0) = (0x80L | v).toByte
      r(1) = (v >> 7).toByte
      out.put(r)
    } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
      val r = new Array[Byte](3)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (v >> 14).toByte
      out.put(r)
    } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
      val r = new Array[Byte](4)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (0x80L | v >> 14).toByte
      r(3) = (v >> 21).toByte
      out.put(r)
    } else if (0L == (v & 0xFFFFFFF800000000L)) {
      val r = new Array[Byte](5)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (0x80L | v >> 14).toByte
      r(3) = (0x80L | v >> 21).toByte
      r(4) = (v >> 28).toByte
      out.put(r)
    } else if (0L == (v & 0xFFFFFC0000000000L)) {
      val r = new Array[Byte](6)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (0x80L | v >> 14).toByte
      r(3) = (0x80L | v >> 21).toByte
      r(4) = (0x80L | v >> 28).toByte
      r(5) = (v >> 35).toByte
      out.put(r)
    } else if (0L == (v & 0xFFFE000000000000L)) {
      val r = new Array[Byte](7)
      r(0) = (0x80L | v).toByte
      r(1) = (0x80L | v >> 7).toByte
      r(2) = (0x80L | v >> 14).toByte
      r(3) = (0x80L | v >> 21).toByte
      r(4) = (0x80L | v >> 28).toByte
      r(5) = (0x80L | v >> 35).toByte
      r(6) = (v >> 42).toByte
      out.put(r)
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
      out.put(r)
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
      out.put(r)
    }
  }

  @inline def f32(v: Float, out: OutStream) = out.put(ByteBuffer.allocate(4).putFloat(v).array)
  @inline def f64(v: Double, out: OutStream) = out.put(ByteBuffer.allocate(8).putDouble(v).array)

  // wraps translation functions to stream users
  implicit def wrap[T](f: T ⇒ Array[Byte]): (T, OutStream) ⇒ Unit = { (v: T, out: OutStream) ⇒ out.put(f(v)) }

  def writeConstArray[T, S >: T](trans: (S, OutStream) ⇒ Unit)(elements: scala.collection.mutable.ArrayBuffer[T], out: OutStream) {
    for (e ← elements)
      trans(e, out)
  }
  def writeVarArray[T, S >: T](trans: (S, OutStream) ⇒ Unit)(elements: scala.collection.mutable.ArrayBuffer[T], out: OutStream) {
    v64(elements.size, out)
    for (e ← elements)
      trans(e, out)
  }
  def writeList[T, S >: T](trans: (S, OutStream) ⇒ Unit)(elements: scala.collection.mutable.ListBuffer[T], out: OutStream) {
    v64(elements.size, out)
    for (e ← elements)
      trans(e, out)
  }
  def writeSet[T, S >: T](trans: (S, OutStream) ⇒ Unit)(elements: scala.collection.mutable.HashSet[T], out: OutStream) {
    v64(elements.size, out)
    for (e ← elements)
      trans(e, out)
  }
  def writeMap[T, U](keys: (T, OutStream) ⇒ Unit, vals: (U, OutStream) ⇒ Unit)(elements: scala.collection.mutable.HashMap[T, U], out: OutStream) {
    v64(elements.size, out)
    for ((k, v) ← elements) {
      keys(k, out)
      vals(v, out)
    }
  }

  /**
   * TODO serialization of restrictions
   */
  def restrictions(p: StoragePool[_], out: OutStream) = out.put(0.toByte)
  /**
   * TODO serialization of restrictions
   */
  def restrictions(f: FieldDeclaration, out: OutStream) = out.put(0.toByte)
  /**
   * serialization of types is fortunately independent of state, because field types know their ID
   */
  def writeType(t: FieldType, out: OutStream) = t match {
    case ConstantI8(v) ⇒
      v64(t.typeID, out)
      i8(v, out)
    case ConstantI16(v) ⇒
      v64(t.typeID, out)
      i16(v, out)
    case ConstantI32(v) ⇒
      v64(t.typeID, out)
      i32(v, out)
    case ConstantI64(v) ⇒
      v64(t.typeID, out)
      i64(v, out)
    case ConstantV64(v) ⇒
      v64(t.typeID, out)
      v64(v, out)

    case ConstantLengthArray(l, t) ⇒
      out.put(0x0F.toByte)
      v64(l, out)
      v64(t.typeID, out)

    case VariableLengthArray(t) ⇒
      out.put(0x11.toByte)
      v64(t.typeID, out)
    case ListType(t) ⇒
      out.put(0x12.toByte)
      v64(t.typeID, out)
    case SetType(t) ⇒
      out.put(0x13.toByte)
      v64(t.typeID, out)

    case MapType(ts) ⇒
      out.put(0x13.toByte)
      v64(ts.size, out)
      for (t ← ts)
        v64(t.typeID, out)

    case _ ⇒
      v64(t.typeID, out)
  }

  /**
   * creates an lbpsi map by recursively adding the local base pool start index to the map and adding all sub pools
   *  afterwards
   */
  final def makeLBPSIMap[T <: SkillType](pool: StoragePool[T], lbpsiMap: Array[Long], next: Long, size: String ⇒ Long): Long = {
    lbpsiMap(pool.poolIndex.toInt) = next
    var result = next + size(pool.name)
    for (sub ← pool.subPools) {
      result = makeLBPSIMap(sub, lbpsiMap, result, size)
    }
    result
  }

  /**
   * concatenates array buffers in the d-map. This will in fact turn the d-map from a map pointing from names to static
   *  instances into a map pointing from names to dynamic instances.
   */
  final def concatenateDataMap[T <: SkillType](pool: StoragePool[T], data: HashMap[String, ArrayBuffer[SkillType]]): Unit = for (sub ← pool.subPools) {
    data(pool.basePool.name) ++= data(sub.name)
    data(sub.name) = data(pool.basePool.name)
    concatenateDataMap(sub, data)
  }
}
""")

    out.close()
  }
}
