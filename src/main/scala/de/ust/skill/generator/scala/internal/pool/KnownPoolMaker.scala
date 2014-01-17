/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait KnownPoolMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/pool/KnownPool.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.pool

import java.io.ByteArrayOutputStream
import java.nio.channels.FileChannel

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import ${packagePrefix}api.KnownType
import ${packagePrefix}internal._
import ${packagePrefix}internal.parsers.FieldParser

/**
 * Marker to indicate that a pool has a type known at generation time.
 *
 * @author Timm Felden
 */
abstract class KnownPool[T <: B, B <: KnownType](name: String, fields: HashMap[String, FieldDeclaration])
    extends AbstractPool(name, fields)
    with Iterable[T] {

  private[internal] def basePool: BasePool[B]

  /**
   * @return a new iterator over all dynamic instances of a type in index ascending order, followed by new instances in type order
   */
  override def iterator: Iterator[T]

  /**
   * @return a new iterator over all dynamic instances of a type in type order
   */
  def typeOrderIterator: Iterator[T]

  /**
   * @return a new iterator over all static instances of a type in type order
   */
  def staticInstances: Iterator[T]

  /**
   * @return a new iterator over all new objects of dynamic type T
   */
  private[internal] def newDynamicInstances: Iterator[T]

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
   * prepares serialization, i.e. ensures that all objects get IDs, which can be used as logic pointers,
   * and can be written to disk
   */
  private[internal] def prepareSerialization(σ: SerializableState): Unit

  /**
   * creates an lbpsi map by recursively adding the local base pool start index to the map and adding all sub pools
   *  afterwards
   */
  final def makeLBPSIMap(lbpsiMap: HashMap[String, Long], next: Long, size: String ⇒ Long): Long = {
    lbpsiMap.put(name, next);
    var result = next + size(name)
    subPools.foreach {
      case sub: SubPool[_, B] ⇒ result = sub.makeLBPSIMap(lbpsiMap, result, size)
    }
    result
  }

  /**
   * concatenates array buffers in the d-map. This will in fact turn the d-map from a map pointing from names to static
   *  instances into a map pointing from names to dynamic instances.
   */
  final def concatenateDMap(d: HashMap[String, ArrayBuffer[KnownType]]): Unit = subPools.foreach {
    case sub: SubPool[_, B] ⇒ d(basePool.name) ++= d(sub.name); d(sub.name) = d(basePool.name); sub.concatenateDMap(d)
  }
}
""")

    //class prefix
    out.close()
  }
}
