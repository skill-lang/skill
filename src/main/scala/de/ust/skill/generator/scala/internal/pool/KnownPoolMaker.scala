/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait KnownPoolMaker extends GeneralOutputMaker{
  abstract override def make {
    super.make
    val out = open("internal/pool/KnownPool.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.pool

import java.io.ByteArrayOutputStream
import java.nio.channels.FileChannel

import scala.collection.mutable.ArrayBuffer

import ${packagePrefix}api.KnownType
import ${packagePrefix}internal.{ PoolIterator, SerializableState, UserType }
import ${packagePrefix}internal.parsers.FieldParser

/**
 * Marker to indicate that a pool has a type known at generation time.
 *
 * @author Timm Felden
 */
abstract class KnownPool[T <: B, B <: KnownType](
  userType: UserType,
  blockCount: Int)
    extends AbstractPool(userType, blockCount)
    with Iterable[T] {

  private[internal] def basePool: BasePool[B]

  /**
   * @return a new pool iterator
   */
  override def iterator(): Iterator[T] = new PoolIterator[T](this)
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
   * the number of static instances loaded from the file
   */
  protected var staticData = 0
  /**
   * the static size is thus the number of static instances plus the number of new objects
   */
  override final def staticSize = staticData + newObjects.length

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
}
""")

    //class prefix
    out.close()
  }
}
