/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait BasePoolMaker extends GeneralOutputMaker{
  abstract override def make {
    super.make
    val out = open("internal/pool/BasePool.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.pool

import ${packagePrefix}api.KnownType
import ${packagePrefix}internal.SerializableState
import ${packagePrefix}internal.UserType

/**
 * provides common functionality for base type storage pools
 *
 * @author Timm Felden
 */
abstract class BasePool[T <: KnownType: Manifest](userType: UserType, σ: SerializableState, blockCount: Int)
    extends KnownPool[T, T](userType, blockCount) with IndexedSeq[T] {

  final override private[internal] def superPool = None

  /**
   * Implements the contract of indexed seq.
   *
   * @note  that this will use an index range starting by 0 and not the ID range, which starts by 1.
   * @note the implementation makes use of the fact, that there are no other pools above this pool
   */
  final override def apply(index: Int) = getByID(index + 1L)
  /**
   * Implements the contract of indexed seq.
   *
   * @note the implementation makes use of the fact, that there are no other pools above this pool
   */
  final override def length: Int = staticSize.toInt

  /**
   * We are the base pool.
   */
  final override def basePool = this

  /**
   * the base type data store
   */
  private[pool] var data = new Array[T](userType.instanceCount.toInt)

  /**
   * returns instances directly from the data store
   *
   * @note base pool data access can not fail, because this would yeald an arary store exception at an earlier stage
   */
  final def getByID(index: Long): T = data(index.toInt - 1)

  /**
   * this function can be used to restore indices after a write operation; this is required to allow for appending after
   * write operations.
   * // TODO maybe it is also required to set nonzero indices of new objects to -1
   * @note it is not possible to append to the written file; maybe write should in fact return a state
   */
  final def restoreIndices = for ((inst, idx) ← data.zipWithIndex if inst.getSkillID != 0) inst.setSkillID(idx+1)
}
""")

    //class prefix
    out.close()
  }
}
