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

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.language.implicitConversions

import _root_.${packagePrefix}internal.streams.OutStream

/**
 * Provides serialization functions;
 *
 * @see SKilL §6.4
 * @author Timm Felden
 */
abstract class SerializationFunctions(state : State) {
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
        if (f.t.isInstanceOf[StringType]) {
          for (i ← p.all)
            strings.add(i.get(f.asInstanceOf[FieldDeclaration[String]]))
        }
      }
    }
  }

  val stringIDs = new HashMap[String, Long]

  def string(v : String, out : OutStream) : Unit = out.v64(stringIDs(v))

  @inline final def annotation(ref : SkillType, out : OutStream) {
    if (null == ref) {
      out.i8(0.toByte)
      out.i8(0.toByte)
    } else {
      if (ref.isInstanceOf[NamedType]) string(ref.asInstanceOf[NamedType].τName, out)
      else string(ref.getClass.getSimpleName.toLowerCase, out)
      out.v64(ref.getSkillID)
    }
  }
}

object SerializationFunctions {

  @inline final def userRef[T <: SkillType](ref : T, out : OutStream) {
    if (null == ref) out.i8(0.toByte)
    else out.v64(ref.getSkillID)
  }

  @inline def bool(v : Boolean, out : OutStream) = out.i8(if (v) -1.toByte else 0.toByte)

  @inline def i8(v : Byte, out : OutStream) = out.i8(v)
  @inline def i16(v : Short, out : OutStream) = out.i16(v)
  @inline def i32(v : Int, out : OutStream) = out.i32(v)
  @inline def i64(v : Long, out : OutStream) = out.i64(v)
  @inline def v64(v : Long, out : OutStream) = out.v64(v)

  @inline def f32(v : Float, out : OutStream) = out.f32(v)
  @inline def f64(v : Double, out : OutStream) = out.f64(v)

  @inline def writeConstArray[T, S >: T](trans : (S, OutStream) ⇒ Unit)(elements : scala.collection.mutable.ArrayBuffer[T], out : OutStream) {
    for (e ← elements)
      trans(e, out)
  }
  @inline def writeVarArray[T, S >: T](trans : (S, OutStream) ⇒ Unit)(elements : scala.collection.mutable.ArrayBuffer[T], out : OutStream) {
    out.v64(elements.size)
    for (e ← elements)
      trans(e, out)
  }
  @inline def writeList[T, S >: T](trans : (S, OutStream) ⇒ Unit)(elements : scala.collection.mutable.ListBuffer[T], out : OutStream) {
    out.v64(elements.size)
    for (e ← elements)
      trans(e, out)
  }
  @inline def writeSet[T, S >: T](trans : (S, OutStream) ⇒ Unit)(elements : scala.collection.mutable.HashSet[T], out : OutStream) {
    out.v64(elements.size)
    for (e ← elements)
      trans(e, out)
  }
  def writeMap[T, U](keys : (T, OutStream) ⇒ Unit, vals : (U, OutStream) ⇒ Unit)(elements : scala.collection.mutable.HashMap[T, U], out : OutStream) {
    out.v64(elements.size)
    for ((k, v) ← elements) {
      keys(k, out)
      vals(v, out)
    }
  }

  // TODO this is not a good solution! (slow and fucked up, but funny)
  def typeToSerializationFunction(t : FieldType[_]) : (Any, OutStream) ⇒ Unit = {
    implicit def lift[T](f : (T, OutStream) ⇒ Unit) : (Any, OutStream) ⇒ Unit = { case (x, out) ⇒ f(x.asInstanceOf[T], out) }
    t match {
      case I8                    ⇒ i8
      case I16                   ⇒ i16
      case I32                   ⇒ i32
      case I64                   ⇒ i64

      case SetType(sub)          ⇒ lift(writeSet(typeToSerializationFunction(sub)))

      case s : StoragePool[_, _] ⇒ userRef
    }
  }

  /**
   * TODO serialization of restrictions
   */
  def restrictions(p : StoragePool[_, _], out : OutStream) = out.i8(0.toByte)
  /**
   * TODO serialization of restrictions
   */
  def restrictions(f : FieldDeclaration[_], out : OutStream) = out.i8(0.toByte)
  /**
   * serialization of types is fortunately independent of state, because field types know their ID
   */
  def writeType(t : FieldType[_], out : OutStream) : Unit = t match {
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
      out.i8(0x0F.toByte)
      v64(l, out)
      v64(t.typeID, out)

    case VariableLengthArray(t) ⇒
      out.i8(0x11.toByte)
      v64(t.typeID, out)
    case ListType(t) ⇒
      out.i8(0x12.toByte)
      v64(t.typeID, out)
    case SetType(t) ⇒
      out.i8(0x13.toByte)
      v64(t.typeID, out)

    case MapType(k, v) ⇒
      out.i8(0x14.toByte)
      writeType(k, out)
      writeType(v, out)

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
