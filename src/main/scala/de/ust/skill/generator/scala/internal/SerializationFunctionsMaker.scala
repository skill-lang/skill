/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
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
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions

import de.ust.skill.common.jvm.streams.OutStream

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
      for (f ← p.fields if f.index != 0) {
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

  /**
   * ************************************************
   * UTILITY FUNCTIONS REQUIRED FOR PARALLEL ENCODING
   * ************************************************
   */

  def offset(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[_]) : Long = f.t match {
    case ConstantI8(_) | ConstantI16(_) | ConstantI32(_) | ConstantI64(_) | ConstantV64(_) ⇒ 0
    case BoolType | I8 ⇒ p.blockInfos.last.count
    case I16 ⇒ 2 * p.blockInfos.last.count
    case F32 | I32 ⇒ 4 * p.blockInfos.last.count
    case F64 | I64 ⇒ 8 * p.blockInfos.last.count

    case V64 ⇒ encodeV64(p, f)
    case s : Annotation ⇒ p.all.map(_.get(f).asInstanceOf[SkillType]).foldLeft(0L)((r : Long, v : SkillType) ⇒ r + encodeSingleV64(1 + state.poolByName(v.getClass.getName.toLowerCase).poolIndex) + encodeSingleV64(v.getSkillID))
    case s : StringType ⇒ p.all.map(_.get(f).asInstanceOf[String]).foldLeft(0L)((r : Long, v : String) ⇒ r + encodeSingleV64(stringIDs(v)))

    case s : StoragePool[_, _] ⇒
      if (p.blockInfos.last.count < 128) p.blockInfos.last.count // quick solution for small pools
      else {
        encodeRefs(p, f)
      }

    case ConstantLengthArray(l, t) ⇒
      val b = p.blockInfos.last
      p.basePool.data.view(b.bpo.toInt, (b.bpo + b.count).toInt).foldLeft(0L) {
        case (sum, i) ⇒
          val xs = i.get(f).asInstanceOf[Iterable[_]];
          sum + encode(xs, t)
      }
    case VariableLengthArray(t) ⇒
      val b = p.blockInfos.last
      p.basePool.data.view(b.bpo.toInt, (b.bpo + b.count).toInt).foldLeft(0L) {
        case (sum, i) ⇒
          val xs = i.get(f).asInstanceOf[Iterable[_]];
          sum + encodeSingleV64(xs.size) + encode(xs, t)
      }
    case ListType(t) ⇒
      val b = p.blockInfos.last
      p.basePool.data.view(b.bpo.toInt, (b.bpo + b.count).toInt).foldLeft(0L) {
        case (sum, i) ⇒
          val xs = i.get(f).asInstanceOf[Iterable[_]];
          sum + encodeSingleV64(xs.size) + encode(xs, t)
      }
    case SetType(t) ⇒
      val b = p.blockInfos.last
      p.basePool.data.view(b.bpo.toInt, (b.bpo + b.count).toInt).foldLeft(0L) {
        case (sum, i) ⇒
          val xs = i.get(f).asInstanceOf[Iterable[_]];
          sum + encodeSingleV64(xs.size) + encode(xs, t)
      }

    case MapType(k, v) ⇒
      val b = p.blockInfos.last
      p.basePool.data.view(b.bpo.toInt, (b.bpo + b.count).toInt).foldLeft(0L) {
        case (sum, i) ⇒
          val m = i.get(f).asInstanceOf[HashMap[_, _]];
          sum + encodeSingleV64(m.size) + encode(m.keys, k) + encode(m.values, v)
      }

    case TypeDefinitionIndex(_) | TypeDefinitionName(_) ⇒ ??? // should have been eliminated already
  }

  @inline private[this] def encodeSingleV64(v : Long) : Long = {
    if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
      1
    } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
      2
    } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
      3
    } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
      4
    } else if (0L == (v & 0xFFFFFFF800000000L)) {
      5
    } else if (0L == (v & 0xFFFFFC0000000000L)) {
      6
    } else if (0L == (v & 0xFFFE000000000000L)) {
      7
    } else if (0L == (v & 0xFF00000000000000L)) {
      8
    } else {
      9
    }
  }

  @inline private[this] def encodeV64(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[_]) : Long = {
    var result = 0L
    val b = p.blockInfos.last
    for (i ← p.basePool.data.view(b.bpo.toInt, (b.bpo + b.count).toInt)) {
      val v = i.get(f).asInstanceOf[Long]
      if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
        result += 1
      } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
        result += 2
      } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
        result += 3
      } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
        result += 4
      } else if (0L == (v & 0xFFFFFFF800000000L)) {
        result += 5
      } else if (0L == (v & 0xFFFFFC0000000000L)) {
        result += 6
      } else if (0L == (v & 0xFFFE000000000000L)) {
        result += 7
      } else if (0L == (v & 0xFF00000000000000L)) {
        result += 8
      } else {
        result += 9
      }
    }
    result
  }

  @inline private[this] def encodeV64(p : Iterable[Long]) : Long = {
    var result = 0L
    for (v ← p) {
      if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
        result += 1
      } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
        result += 2
      } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
        result += 3
      } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
        result += 4
      } else if (0L == (v & 0xFFFFFFF800000000L)) {
        result += 5
      } else if (0L == (v & 0xFFFFFC0000000000L)) {
        result += 6
      } else if (0L == (v & 0xFFFE000000000000L)) {
        result += 7
      } else if (0L == (v & 0xFF00000000000000L)) {
        result += 8
      } else {
        result += 9
      }
    }
    result
  }

  @inline private[this] def encodeRefs(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[_]) : Long = {
    var result = 0L
    val b = p.blockInfos.last
    for (i ← p.basePool.data.view(b.bpo.toInt, (b.bpo + b.count).toInt)) {
      val v = i.get(f).asInstanceOf[SkillType].getSkillID
      if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
        result += 1
      } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
        result += 2
      } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
        result += 3
      } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
        result += 4
      } else if (0L == (v & 0xFFFFFFF800000000L)) {
        result += 5
      } else if (0L == (v & 0xFFFFFC0000000000L)) {
        result += 6
      } else if (0L == (v & 0xFFFE000000000000L)) {
        result += 7
      } else if (0L == (v & 0xFF00000000000000L)) {
        result += 8
      } else {
        result += 9
      }
    }
    result
  }

  @inline private[this] def encodeRefs(p : Iterable[SkillType]) : Long = {
    var result = 0L
    for (i ← p) {
      val v = i.getSkillID
      if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
        result += 1
      } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
        result += 2
      } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
        result += 3
      } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
        result += 4
      } else if (0L == (v & 0xFFFFFFF800000000L)) {
        result += 5
      } else if (0L == (v & 0xFFFFFC0000000000L)) {
        result += 6
      } else if (0L == (v & 0xFFFE000000000000L)) {
        result += 7
      } else if (0L == (v & 0xFF00000000000000L)) {
        result += 8
      } else {
        result += 9
      }
    }
    result
  }

  private[this] def encode[T](xs : Iterable[T], t : FieldType[_]) : Long = t match {
    case ConstantI8(_) | ConstantI16(_) | ConstantI32(_) | ConstantI64(_) | ConstantV64(_) ⇒ 0
    case BoolType | I8 ⇒ xs.size
    case I16 ⇒ 2 * xs.size
    case F32 | I32 ⇒ 4 * xs.size
    case F64 | I64 ⇒ 8 * xs.size

    case V64 ⇒ encodeV64(xs.asInstanceOf[Iterable[Long]])
    case s : Annotation ⇒ xs.asInstanceOf[Iterable[SkillType]].foldLeft(0L)((r : Long, v : SkillType) ⇒ r + encodeSingleV64(1 + state.poolByName(v.getClass.getName.toLowerCase).poolIndex) + encodeSingleV64(v.getSkillID))
    case s : StringType ⇒ xs.asInstanceOf[Iterable[String]].foldLeft(0L)((r : Long, v : String) ⇒ r + encodeSingleV64(stringIDs(v)))

    case t : StoragePool[_, _] ⇒
      if (t.blockInfos.last.count < 128) xs.size // quick solution for small pools
      else encodeRefs(xs.asInstanceOf[Iterable[SkillType]])

    case ConstantLengthArray(l, t) ⇒ xs.asInstanceOf[Iterable[ArrayBuffer[_]]].foldLeft(0L) {
      case (sum, i) ⇒ sum + encode(i, t)
    }
    case VariableLengthArray(t) ⇒ xs.asInstanceOf[Iterable[ArrayBuffer[_]]].foldLeft(0L) {
      case (sum, i) ⇒ sum + encodeSingleV64(i.size) + encode(i, t)
    }
    case ListType(t) ⇒ xs.asInstanceOf[Iterable[ListBuffer[_]]].foldLeft(0L) {
      case (sum, i) ⇒ sum + encodeSingleV64(i.size) + encode(i, t)
    }
    case SetType(t) ⇒ xs.asInstanceOf[Iterable[HashSet[_]]].foldLeft(0L) {
      case (sum, i) ⇒ sum + encodeSingleV64(i.size) + encode(i, t)
    }

    case MapType(k, v) ⇒ xs.asInstanceOf[Iterable[HashMap[_, _]]].foldLeft(0L) {
      case (sum, i) ⇒ sum + encodeSingleV64(i.size) + encode(i.keys, k) + encode(i.values, v)
    }

    case TypeDefinitionIndex(_) | TypeDefinitionName(_) ⇒ ??? // should have been eliminated already
  }

  // TODO this is not a good solution! (slow and fucked up, but funny)
  def typeToSerializationFunction(t : FieldType[_]) : (Any, OutStream) ⇒ Unit = {
    implicit def lift[T](f : (T, OutStream) ⇒ Unit) : (Any, OutStream) ⇒ Unit = { case (x, out) ⇒ f(x.asInstanceOf[T], out) }
    t match {
      case ConstantI8(_) | ConstantI16(_) | ConstantI32(_) | ConstantI64(_) | ConstantV64(_) ⇒ { case (x, out) ⇒ }

      case BoolType ⇒ bool
      case I8 ⇒ i8
      case I16 ⇒ i16
      case I32 ⇒ i32
      case I64 ⇒ i64
      case V64 ⇒ v64
      case F32 ⇒ f32
      case F64 ⇒ f64

      case Annotation(_) ⇒ annotation
      case StringType(_) ⇒ string

      case ConstantLengthArray(len, sub) ⇒ lift(writeConstArray(typeToSerializationFunction(sub)))
      case VariableLengthArray(sub) ⇒ lift(writeVarArray(typeToSerializationFunction(sub)))
      case ListType(sub) ⇒ lift(writeList(typeToSerializationFunction(sub)))
      case SetType(sub) ⇒ lift(writeSet(typeToSerializationFunction(sub)))

      case MapType(k, v) ⇒ lift(writeMap(typeToSerializationFunction(k), typeToSerializationFunction(v)))

      case s : StoragePool[_, _] ⇒ userRef

      case TypeDefinitionIndex(_) | TypeDefinitionName(_) ⇒
        throw new IllegalStateException("trying to serialize an intermediary type representation can never be successful")
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
   * creates an lbpo map by recursively adding the local base pool offset to the map and adding all sub pools
   *  afterwards
   */
  final def makeLBPOMap[T <: B, B <: SkillType](pool : StoragePool[T, B], lbpsiMap : Array[Long], next : Long, size : StoragePool[_, _] ⇒ Long) : Long = {
    lbpsiMap(pool.poolIndex.toInt) = next
    var result = next + size(pool)
    for (sub ← pool.subPools) {
      result = makeLBPOMap(sub, lbpsiMap, result, size)
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
