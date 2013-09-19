package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait KnownPoolMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/pool/KnownPool.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.pool

import java.io.ByteArrayOutputStream
import java.nio.channels.FileChannel

import scala.collection.mutable.ArrayBuffer

import ${packagePrefix}api.KnownType
import ${packagePrefix}internal.{PoolIterator,SerializableState,UserType}
import ${packagePrefix}internal.parsers.FieldParser

/**
 * Marker to indicate that a pool has a type known at generation time.
 *
 * @author Timm Felden
 */
abstract class KnownPool[T <: B, B <: KnownType](
  userType: UserType,
  superPool: ⇒ Option[KnownPool[_ >: T <: B, B]],
  blockCount: Int)
    extends AbstractPool(userType, superPool, blockCount)
    with Seq[T] {
  // override the super pool, because we know, that all our super types are known as well
  override private[internal] def superPool: Option[KnownPool[B, B]] = super.superPool.asInstanceOf[Option[KnownPool[B, B]]]

  private[internal] def basePool: BasePool[B]

  /**
   * the default size is the one including the sub-types
   */
  final override def length = dynamicSize.toInt
  /**
   * @return a new pool iterator
   */
  override def iterator(): PoolIterator[T] = new PoolIterator[T](this)
  /**
   * implementation of the Seq[T] contract
   */
  final override def apply(index: Int) = getByID(index);
  /**
   * we wan to get objects by a long ID, because we might have up to 2^64 instances (at least somewhere in the future)
   */
  def getByID(index: Long): T

  /**
   * All stored objects, which have exactly the type T. Objects are stored as arrays of field entries. The types of the
   *  respective fields can be retrieved using the fieldTypes map.
   */
  private[internal] final var newObjects = ArrayBuffer[T]()

  /**
   * Can be used on known pools to construct instances of the pool, AFTER the sub pool relation has been constructed.
   */
  private[internal] def constructPool(): Unit

  /**
   * provides a new instance of T using the static implementation type of T
   *
   * @note implementations will declare this as @inline
   */
  protected[this] def newInstance: T

  /**
   * Reads all fields required by the generation time specification.
   *
   * Fields are read after creation of the pools in an arbitrary order.
   */
  private[internal] def readFields(fieldParser: FieldParser): Unit

  /**
   * write the type definition into head and field data into out; the offset of field data has to be out.size
   */
  private[internal] def write(head: FileChannel, out: ByteArrayOutputStream, σ: SerializableState): Unit

  /**
   * prepares serialization, i.e. ensures that all objects get IDs, which can be used as logic pointers,
   * and can be written to disk
   */
  private[internal] def prepareSerialization(σ: SerializableState): Unit
}""")

    //class prefix
    out.close()
  }
}