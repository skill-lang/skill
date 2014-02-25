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

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import scala.annotation.switch
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import ${packagePrefix}_
import ${packagePrefix}api.KnownType
import ${packagePrefix}api.SkillType
import ${packagePrefix}internal.pool._

/**
 * Provides serialization functions;
 *
 * @see SKilL §6.4
 * @author Timm Felden
 */
sealed abstract class SerializationFunctions(state: SerializableState) {
  import SerializationFunctions._

  //collect String instances from known string types; this is needed, because strings are something special on the jvm
${
      (for (
        d ← IR;
        f ← d.getFields;
        if ("string" == f.getType.getSkillName)
      ) yield {
        if (d.getRestrictions().collect({ case r: SingletonRestriction ⇒ r }).isEmpty)
          s"""  for(i ← state.${d.getName}.all) state.String.add(i.${f.getName})"""
        else
          s"""  state.String.add(state.${d.getName}.get.${f.getName})"""
      }).mkString("\n")
    }
  val serializationIDs = new HashMap[String, Long]

  def annotation(ref: SkillType): List[Array[Byte]]

  def string(v: String): Array[Byte] = v64(serializationIDs(v))
}

object SerializationFunctions {

  def bool(v: Boolean): Array[Byte] = Array[Byte](if (v) -1 else 0)

  def i8(v: Byte): Array[Byte] = Array(v)

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

  def userRef[T <: SkillType](ref: T) = v64(ref.getSkillID)

  // wraps translation functions to stream users
  implicit def wrap[T](f: T ⇒ Array[Byte]): (T, ByteArrayOutputStream) ⇒ Unit = { (v: T, out: ByteArrayOutputStream) ⇒ out.write(f(v)) }

  def writeConstArray[T, S >: T](trans: S ⇒ Array[Byte])(elements: $ArrayTypeName[T], out: ByteArrayOutputStream) {
    for (e ← elements)
      out.write(trans(e))
  }
  def writeVarArray[T, S >: T](trans: S ⇒ Array[Byte])(elements: $VarArrayTypeName[T], out: ByteArrayOutputStream) {
    out.write(v64(elements.size))
    for (e ← elements)
      out.write(trans(e))
  }
  def writeList[T, S >: T](trans: S ⇒ Array[Byte])(elements: $ListTypeName[T], out: ByteArrayOutputStream) {
    out.write(v64(elements.size))
    for (e ← elements)
      out.write(trans(e))
  }
  def writeSet[T, S >: T](trans: S ⇒ Array[Byte])(elements: $SetTypeName[T], out: ByteArrayOutputStream) {
    out.write(v64(elements.size))
    for (e ← elements)
      out.write(trans(e))
  }
  def writeMap[T, U](keys: (T, ByteArrayOutputStream) ⇒ Unit, vals: (U, ByteArrayOutputStream) ⇒ Unit)(elements: $MapTypeName[T, U], out: ByteArrayOutputStream) {
    out.write(v64(elements.size))
    for ((k, v) ← elements) {
      keys(k, out)
      vals(v, out)
    }
  }

  /**
   * creates an lbpsi map by recursively adding the local base pool start index to the map and adding all sub pools
   *  afterwards
   */
  final def makeLBPSIMap[T <: B, B <: KnownType](pool: KnownPool[T, B], lbpsiMap: HashMap[String, Long], next: Long, size: String ⇒ Long): Long = {
    lbpsiMap.put(pool.name, next);
    var result = next + size(pool.name)
    pool.getSubPools.foreach {
      case sub: SubPool[_, B] ⇒ result = makeLBPSIMap(sub, lbpsiMap, result, size)
    }
    result
  }

  /**
   * concatenates array buffers in the d-map. This will in fact turn the d-map from a map pointing from names to static
   *  instances into a map pointing from names to dynamic instances.
   */
  final def concatenateDMap[T <: B, B <: KnownType](pool: KnownPool[T, B], d: HashMap[String, ArrayBuffer[KnownType]]): Unit = pool.getSubPools.foreach {
    case sub: SubPool[_, B] ⇒
      d(pool.basePool.name) ++= d(sub.name)
      d(sub.name) = d(pool.basePool.name)
      concatenateDMap(sub, d)
  }
}

/**
 * holds state of an append operation
 * @author Timm Felden
 */
private[internal] final class AppendState(val state: SerializableState) extends SerializationFunctions(state) {
  import SerializationFunctions._
  import state._

  def getByID[T <: SkillType](typeName: String, id: Long): T = d(typeName)(id.toInt).asInstanceOf[T]

  /**
   * typeIDs used in the stored file
   * type IDs are constructed together with the lbpsi map
   */
  val typeID = new HashMap[String, Int]

  /**
   * prepares a state, i.e. calculates d-map and lpbsi-map
   */
  val d = new HashMap[String, ArrayBuffer[KnownType]]
  // store static instances in d
  knownPools.foreach { p ⇒
    d.put(p.name, p.newObjects.asInstanceOf[ArrayBuffer[KnownType]]: @unchecked)
  }

  // make lbpsi map and update d-maps
  val lbpsiMap = new HashMap[String, Long]
  pools.values.foreach {
    case p: BasePool[_] ⇒
      makeLBPSIMap(p, lbpsiMap, 1, { s ⇒ typeID.put(s, typeID.size + 32); d(s).size })
      concatenateDMap(p, d)
      var id = 1L
      for (i ← d(p.name)) {
        i.setSkillID(id)
        id += 1
      }
    case _ ⇒
  }

  def foreachOf[T <: SkillType](name: String, f: T ⇒ Unit) = {
    val low = lbpsiMap(name) - 1
    val r = low.toInt until (low + state.pools(name).dynamicSize).toInt
    val ab = d(name)
    for (i ← r)
      f(ab(i).asInstanceOf[T])
  }

  override def annotation(ref: SkillType): List[Array[Byte]] = {
    val baseName = state.pools(ref.getClass.getSimpleName.toLowerCase).asInstanceOf[KnownPool[_, _]].basePool.name

    List(v64(serializationIDs(baseName)), v64(ref.getSkillID))
  }

  def writeTypeBlock(file: FileChannel) {
    // write count of the type block
    file.write(ByteBuffer.wrap(v64(knownPools.filter { p ⇒ p.dynamicSize > 0 }.size)))

    // write fields back to their buffers
    val out = new ByteArrayOutputStream

    @inline def put(b: Array[Byte]) = file.write(ByteBuffer.wrap(b));
    @inline def putType(t: TypeInfo): Unit = t match {
      case t: ConstantI8Info ⇒
        put(v64(t.typeId));
        put(i8(t.value))

      case t: ConstantI16Info ⇒
        put(v64(t.typeId));
        put(i16(t.value))

      case t: ConstantI32Info ⇒
        put(v64(t.typeId));
        put(i32(t.value))

      case t: ConstantI64Info ⇒
        put(v64(t.typeId));
        put(i64(t.value))

      case t: ConstantV64Info ⇒
        put(v64(t.typeId));
        put(v64(t.value))

      case t: ConstantLengthArrayInfo ⇒
        put(v64(t.typeId));
        put(v64(t.length));
        putType(t.groundType)

      case t: VariableLengthArrayInfo ⇒
        put(v64(t.typeId));
        putType(t.groundType)
      case t: ListInfo ⇒
        put(v64(t.typeId));
        putType(t.groundType)
      case t: SetInfo ⇒
        put(v64(t.typeId));
        putType(t.groundType)

      case t: MapInfo ⇒
        put(v64(t.typeId));
        put(v64(t.groundType.size));
        t.groundType.foreach(putType)

      case t: NamedUserType ⇒
        put(v64(typeID(t.name).ensuring(_ >= 0)))
      case _ ⇒
        put(v64(t.typeId.ensuring(_ >= 0)))
    }
    @inline def write(p: KnownPool[_, _]) {
      (p.name: @switch) match {${
      (for (d ← IR) yield {
        val name = d.getName
        val sName = d.getSkillName
        val fields = d.getFields
        s"""
        case "$sName" ⇒ locally {
          val outData = d("$sName").asInstanceOf[Iterable[$name]]
          val fields = p.fields
          val blockInfos = p.blockInfos

          // check the kind of header we have to write
          if (blockInfos.isEmpty) {
            // the type is yet unknown, thus we will write all information
            put(string("$sName"))
            put(Array[Byte](0))
            put(v64(outData.size))
            put(v64(0)) // restrictions not implemented yet

            put(v64(fields.size))
          } else {
            // the type is known, thus we only have to write {name, ?lbpsi, count, fieldCount}
            put(string("$sName"))
            put(v64(outData.size)) // the append state known how many instances we will write

            if (0 == outData.size)
              put(v64(fields.filter { case (n, f) ⇒ f.dataChunks.isEmpty }.size))
            else
              put(v64(fields.size))
          }

          for ((name, f) ← fields) {
            (name: @switch) match {${
          (for (f ← fields) yield {
            val sName = f.getSkillName
            s"""
              case "$sName" ⇒ if (f.dataChunks.isEmpty) {
                put(v64(0)) // field restrictions not implemented yet
                putType(f.t)
                put(string("$sName"))
                ${writeField(d, f, s"p.asInstanceOf[${name}StoragePool]")}
                put(v64(out.size))
              } else if (0 != outData.size) {
                ${writeField(d, f, s"p.asInstanceOf[${name}StoragePool].newDynamicInstances")}
                put(v64(out.size))
              }"""
          }).mkString("")
        }
              case _ ⇒ assert(0 == p.newDynamicInstances.size, "adding instances with an unknown field is currently not supported")
            }
          }
        }"""
      }).mkString("")
    }
        case _ ⇒ // nothing to do
      }
    }

    // write header
    def writeSubPools(p: KnownPool[_, _]): Unit = if (p.dynamicSize > 0) {
      write(p)
      for (p ← p.getSubPools)
        if (p.isInstanceOf[KnownPool[_, _]])
          writeSubPools(p.asInstanceOf[KnownPool[_, _]])
    }
    for (p ← knownPools)
      if (p.isInstanceOf[BasePool[_]])
        writeSubPools(p)

    // write data
    file.write(ByteBuffer.wrap(out.toByteArray()))
  }
}

/**
 * holds state of a write operation
 * @author Timm Felden
 */
private[internal] final class WriteState(val state: SerializableState) extends SerializationFunctions(state) {
  import SerializationFunctions._
  import state._

  def getByID[T <: SkillType](typeName: String, id: Long): T = d(typeName)(id.toInt).asInstanceOf[T]

  /**
   * typeIDs used in the stored file
   * type IDs are constructed together with the lbpsi map
   */
  val typeID = new HashMap[String, Int]

  /**
   * prepares a state, i.e. calculates d-map and lpbsi-map
   */
  val d = new HashMap[String, ArrayBuffer[KnownType]]
  // store static instances in d
  knownPools.foreach { p ⇒
    val ab = new ArrayBuffer[KnownType](p.staticSize.toInt);
    for (i ← p.staticInstances)
      ab.append(i)
    d.put(p.name, ab)
  }

  // make lbpsi map and update d-maps
  val lbpsiMap = new HashMap[String, Long]
  pools.values.foreach {
    case p: BasePool[_] ⇒
      makeLBPSIMap(p, lbpsiMap, 1, { s ⇒ typeID.put(s, typeID.size + 32); d(s).size })
      concatenateDMap(p, d)
      var id = 1L
      for (i ← d(p.name)) {
        i.setSkillID(id)
        id += 1
      }
    case _ ⇒
  }

  def foreachOf[T <: SkillType](name: String, f: T ⇒ Unit) = {
    val low = lbpsiMap(name) - 1
    val r = low.toInt until (low + state.pools(name).dynamicSize).toInt
    val ab = d(name)
    for (i ← r)
      f(ab(i).asInstanceOf[T])
  }

  override def annotation(ref: SkillType): List[Array[Byte]] = {
    val baseName = state.pools(ref.getClass.getSimpleName.toLowerCase).asInstanceOf[KnownPool[_, _]].basePool.name

    List(v64(serializationIDs(baseName)), v64(ref.getSkillID))
  }

  def writeTypeBlock(file: FileChannel) {
    // write count of the type block
    file.write(ByteBuffer.wrap(v64(knownPools.filter { p ⇒ p.dynamicSize > 0 }.size)))

    // write fields back to their buffers
    val out = new ByteArrayOutputStream

    @inline def put(b: Array[Byte]) = file.write(ByteBuffer.wrap(b));
    @inline def putType(t: TypeInfo): Unit = t match {
      case t: ConstantI8Info ⇒
        put(v64(t.typeId));
        put(i8(t.value))

      case t: ConstantI16Info ⇒
        put(v64(t.typeId));
        put(i16(t.value))

      case t: ConstantI32Info ⇒
        put(v64(t.typeId));
        put(i32(t.value))

      case t: ConstantI64Info ⇒
        put(v64(t.typeId));
        put(i64(t.value))

      case t: ConstantV64Info ⇒
        put(v64(t.typeId));
        put(v64(t.value))

      case t: ConstantLengthArrayInfo ⇒
        put(v64(t.typeId));
        put(v64(t.length));
        putType(t.groundType)

      case t: VariableLengthArrayInfo ⇒
        put(v64(t.typeId));
        putType(t.groundType)
      case t: ListInfo ⇒
        put(v64(t.typeId));
        putType(t.groundType)
      case t: SetInfo ⇒
        put(v64(t.typeId));
        putType(t.groundType)

      case t: MapInfo ⇒
        put(v64(t.typeId));
        put(v64(t.groundType.size));
        t.groundType.foreach(putType)

      case t: NamedUserType ⇒
        put(v64(typeID(t.name).ensuring(_ >= 0)))
      case _ ⇒
        put(v64(t.typeId.ensuring(_ >= 0)))
    }
    @inline def write(p: KnownPool[_, _]) {
      (p.name: @switch) match {${
      (for (d ← IR) yield {
        val sName = d.getSkillName
        val fields = d.getFields
        s"""
        case "$sName" ⇒ locally {
          val outData = d("$sName").asInstanceOf[Iterable[${d.getName}]]
          val fields = p.fields

          put(string("$sName"))
          ${
          if (null == d.getSuperType) "put(Array[Byte](0))"
          else s"""put(string("${d.getSuperType.getSkillName}"))
          put(v64(lbpsiMap("$sName")))"""
        }
          put(v64(p.size))
          put(v64(0)) // restrictions not implemented yet

          put(v64(${fields.size})) // number of known fields

          for ((name, f) ← fields) {
            (name: @switch) match {${
          (for (f ← fields) yield s"""
              case "${f.getSkillName()}" ⇒ locally {
                put(v64(0)) // field restrictions not implemented yet
                putType(f.t)
                put(string("${f.getSkillName()}"))
                ${writeField(d, f, "outData")}
                put(v64(out.size))
              }""").mkString("")
        }
              case n ⇒ System.err.println(s"dropped unknown field Node.$$n during write operation!")
            }
          }
        }"""
      }
      ).mkString("")
    }
        case t ⇒ System.err.println(s"dropped unknown type $$t")
      }
    }
    @inline def writeSubPools(p: KnownPool[_, _]): Unit = if (p.dynamicSize > 0) {
      write(p)
      for (p ← p.getSubPools)
        if (p.isInstanceOf[KnownPool[_, _]])
          writeSubPools(p.asInstanceOf[KnownPool[_, _]])
    }
    for (p ← knownPools)
      if (p.isInstanceOf[BasePool[_]])
        writeSubPools(p)

    file.write(ByteBuffer.wrap(out.toByteArray()))
  }
}
""")

    //class prefix
    out.close()
  }

  // field writing helper functions
  def writeField(d: Declaration, f: Field, iteratorName: String): String = f.getType match {
    case t: GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒
        s"""$iteratorName.foreach { instance ⇒ annotation(instance.${escaped(f.getName)}).foreach(out.write _) }"""

      case "v64" ⇒
        s"""val target = new Array[Byte](9 * outData.size)
                var offset = 0

                val it = outData.iterator.asInstanceOf[Iterator[${d.getName}]]
                while (it.hasNext)
                  offset += v64(it.next.${escaped(f.getName)}, target, offset)

                out.write(target, 0, offset)"""

      case "i64" ⇒
        s"""val target = ByteBuffer.allocate(8 * outData.size)
                val it = outData.iterator.asInstanceOf[Iterator[${d.getName}]]
                while (it.hasNext)
                  target.putLong(it.next.${escaped(f.getName)})

                out.write(target.array)"""

      case _ ⇒ s"$iteratorName.foreach { instance ⇒ out.write(${f.getType().getSkillName()}(instance.${escaped(f.getName)})) }"
    }
    case t: Declaration ⇒
      s"""@inline def putField(i: $packagePrefix${d.getName}) { out.write(v64(i.${escaped(f.getName)}.getSkillID)) }
                foreachOf("${t.getSkillName}", putField)"""

    case t: ConstantLengthArrayType ⇒ s"$iteratorName.map(_.${escaped(f.getName)}).foreach { instance ⇒ writeConstArray(${
      t.getBaseType() match {
        case t: Declaration ⇒ s"userRef[${mapType(t)}]"
        case b              ⇒ b.getSkillName()
      }
    })(instance, out) }"
    case t: VariableLengthArrayType ⇒ s"$iteratorName.map(_.${escaped(f.getName)}).foreach { instance ⇒ writeVarArray(${
      t.getBaseType() match {
        case t: Declaration ⇒ s"userRef[${mapType(t)}]"
        case b              ⇒ b.getSkillName()
      }
    })(instance, out) }"
    case t: SetType ⇒ s"$iteratorName.map(_.${escaped(f.getName)}).foreach { instance ⇒ writeSet(${
      t.getBaseType() match {
        case t: Declaration ⇒ s"userRef[${mapType(t)}]"
        case b              ⇒ b.getSkillName()
      }
    })(instance, out) }"
    case t: ListType ⇒ s"$iteratorName.map(_.${escaped(f.getName)}).foreach { instance ⇒ writeList(${
      t.getBaseType() match {
        case t: Declaration ⇒ s"userRef[${mapType(t)}]"
        case b              ⇒ b.getSkillName()
      }
    })(instance, out) }"

    case t: MapType ⇒ locally {
      s"$iteratorName.map(_.${escaped(f.getName)}).foreach { instance ⇒ ${
        t.getBaseTypes().map {
          case t: Declaration ⇒ s"userRef[${mapType(t)}]"
          case b              ⇒ b.getSkillName()
        }.reduceRight { (t, v) ⇒
          s"writeMap($t, $v)"
        }
      }(instance, out) }"
    }

    // does this case even exist?   case _: ⇒ s"$iteratorName.foreach { instance ⇒ out.write(${f.getType().getSkillName()}(instance.${escaped(f.getName)})) }"
  }
}
