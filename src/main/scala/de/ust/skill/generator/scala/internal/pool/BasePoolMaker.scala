package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait BasePoolMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/pool/BasePool.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.pool

import ${packagePrefix}api.KnownType
import ${packagePrefix}internal.{ UserType, SerializableState }

/**
 * provides common functionality for base type storage pools
 *
 * @author Timm Felden
 */
abstract class BasePool[T <: KnownType: Manifest](userType: UserType, σ: SerializableState, blockCount: Int)
    extends KnownPool[T, T](userType, None, blockCount) {

  /**
   * We are the base pool.
   */
  final override def basePool = this

  /**
   * the base type data store
   */
  private[pool] var data = new Array[T](userType.instanceCount.toInt)

  /**
   * construct instances of the pool in post-order, i.e. bottom-up
   */
  final override def constructPool() {
    // construct data in a bottom up order
    subPools.collect { case p: KnownPool[_, _] ⇒ p }.foreach(_.constructPool)
    userType.blockInfos.values.foreach({ b ⇒
      for (i ← b.bpsi - 1 until b.bpsi + b.count - 1)
        if (null == data(i.toInt))
          data(i.toInt) = newInstance
    })
  }

  /**
   * returns instances directly from the data store
   *
   * @note base pool data access can not fail, because this would yeald an arary store exception at an earlier stage
   */
  final def getByID(index: Long): T = data(index.toInt - 1)
}""")

    //class prefix
    out.close()
  }
}