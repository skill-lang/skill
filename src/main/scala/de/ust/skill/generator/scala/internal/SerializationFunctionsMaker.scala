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

import ${packagePrefix}internal.streams.OutStream

/**
 * Provides serialization functions;
 *
 * @see SKilL §6.4
 * @author Timm Felden
 */
abstract class SerializationFunctions(state : SerializableState) {
  import SerializationFunctions._

  //collect String instances from known string types; this is needed, because strings are something special on the jvm
  //@note this is a O(σ) operation:)
  //@note we do no longer make use of the generation time type info, because we want to treat generic fields as well
  locally {
    val strings = state.String
    for (p ← state.pools) {
      strings.add(p.name)
      for (f ← p.fields) {
        if (p.knownFields.contains(f.name)) {
          strings.add(f.name)
          if (StringType == f.t) {
            for (i ← p.all)
              strings.add(i.get(p, f).asInstanceOf[String])
          }
        }
      }
    }
  }

  val stringIDs = new HashMap[String, Long]

  def annotation(ref : SkillType, out : OutStream) : Unit

  def string(v : String, out : OutStream) : Unit = out.v64(stringIDs(v))
}

object SerializationFunctions {

  @inline final def userRef[T <: SkillType](ref : T, out : OutStream) {
    if (null == ref) out.put(0.toByte)
    else out.v64(ref.getSkillID)
  }

  @inline def bool(v : Boolean, out : OutStream) = out.put(if (v) -1.toByte else 0.toByte)

  @inline def i8(v : Byte, out : OutStream) = out.put(v)
  @inline def i16(v : Short, out : OutStream) = out.put(ByteBuffer.allocate(2).putShort(v).array)
  @inline def i32(v : Int, out : OutStream) = out.put(ByteBuffer.allocate(4).putInt(v).array)
  @inline def i64(v : Long, out : OutStream) = out.put(ByteBuffer.allocate(8).putLong(v).array)
  @inline def v64(v : Long, out : OutStream) = out.v64(v)

  @inline def f32(v : Float, out : OutStream) = out.put(ByteBuffer.allocate(4).putFloat(v).array)
  @inline def f64(v : Double, out : OutStream) = out.put(ByteBuffer.allocate(8).putDouble(v).array)

  // wraps translation functions to stream users
  implicit def wrap[T](f : T ⇒ Array[Byte]) : (T, OutStream) ⇒ Unit = { (v : T, out : OutStream) ⇒ out.put(f(v)) }

  def writeConstArray[T, S >: T](trans : (S, OutStream) ⇒ Unit)(elements : scala.collection.mutable.ArrayBuffer[T], out : OutStream) {
    for (e ← elements)
      trans(e, out)
  }
  def writeVarArray[T, S >: T](trans : (S, OutStream) ⇒ Unit)(elements : scala.collection.mutable.ArrayBuffer[T], out : OutStream) {
    v64(elements.size, out)
    for (e ← elements)
      trans(e, out)
  }
  def writeList[T, S >: T](trans : (S, OutStream) ⇒ Unit)(elements : scala.collection.mutable.ListBuffer[T], out : OutStream) {
    v64(elements.size, out)
    for (e ← elements)
      trans(e, out)
  }
  def writeSet[T, S >: T](trans : (S, OutStream) ⇒ Unit)(elements : scala.collection.mutable.HashSet[T], out : OutStream) {
    v64(elements.size, out)
    for (e ← elements)
      trans(e, out)
  }
  def writeMap[T, U](keys : (T, OutStream) ⇒ Unit, vals : (U, OutStream) ⇒ Unit)(elements : scala.collection.mutable.HashMap[T, U], out : OutStream) {
    v64(elements.size, out)
    for ((k, v) ← elements) {
      keys(k, out)
      vals(v, out)
    }
  }

  /**
   * TODO serialization of restrictions
   */
  def restrictions(p : StoragePool[_, _], out : OutStream) = out.put(0.toByte)
  /**
   * TODO serialization of restrictions
   */
  def restrictions(f : FieldDeclaration, out : OutStream) = out.put(0.toByte)
  /**
   * serialization of types is fortunately independent of state, because field types know their ID
   */
  def writeType(t : FieldType, out : OutStream) = t match {
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
  final def makeLBPSIMap[T <: B, B <: SkillType](pool : StoragePool[T, B], lbpsiMap : Array[Long], next : Long, size : String ⇒ Long) : Long = {
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
  final def concatenateDataMap[T <: B, B <: SkillType](pool : StoragePool[T, B], data : HashMap[String, ArrayBuffer[SkillType]]) : Unit = for (sub ← pool.subPools) {
    data(pool.basePool.name) ++= data(sub.name)
    data(sub.name) = data(pool.basePool.name)
    concatenateDataMap(sub, data)
  }
}
""")

    out.close()
  }
}
