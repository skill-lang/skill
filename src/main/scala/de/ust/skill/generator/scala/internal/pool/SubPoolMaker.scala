package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait SubPoolMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/pool/SubPool.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.pool

import ${packagePrefix}api.KnownType
import ${packagePrefix}internal.{ SkillException, UserType, SerializableState }

/**
 * provides common funcionality for sub type pools, i.e. for pools where B!=T.
 *
 * @author Timm Felden
 */
abstract class SubPool[T <: B, B <: KnownType](
  userType: UserType,
  superPool: KnownPool[B, B],
  σ: SerializableState,
  blockCount: Int)
    extends KnownPool[T, B](userType, Some(superPool), blockCount) {
  /**
   * the super base pool; note that this requires construction of pools in a top-down order
   */
  final override def basePool = superPool.basePool

  /**
   * get is deferred to the base pool
   */
  def getByID(index: Long): T = try { basePool.getByID(index).asInstanceOf[T] } catch {
    case e: ClassCastException ⇒ SkillException(
      s""\"tried to access a "$$name" at index $$index, but it was actually a $${
        basePool.data(index.toInt - 1).getClass().getName()
      }""\", e
    )
  }

  /**
   * construct instances of the pool in post-order, i.e. bottom-up
   */
  final override def constructPool() {
    val data = basePool.data
    // construct data in a bottom up order
    subPools.collect { case p: KnownPool[_, _] ⇒ p }.foreach(_.constructPool)
    userType.blockInfos.values.foreach({ b ⇒
      for (i ← b.bpsi - 1 until b.bpsi + b.count - 1)
        if (null == data(i.toInt))
          data(i.toInt) = newInstance
    })
  }
}""")

    //class prefix
    out.close()
  }
}