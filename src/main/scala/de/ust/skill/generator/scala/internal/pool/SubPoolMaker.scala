/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait SubPoolMaker extends GeneralOutputMaker {
  abstract override def make {
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
  val _superPool: KnownPool[_ >: T <: B, B],
  σ: SerializableState,
  blockCount: Int)
    extends KnownPool[T, B](userType, blockCount) {
  final override private[internal] def superPool: Option[KnownPool[_ >: T <: B, B]] = Some(_superPool)

  /**
   * the super base pool; note that this requires construction of pools in a top-down order
   */
  final override def basePool = _superPool.basePool

  /**
   * the base type data store
   */
  private[pool] var data:Array[B] = basePool.data

  /**
   * get is deferred to the base pool
   */
  def getByID(index: Long): T = try { basePool.getByID(index).asInstanceOf[T] } catch {
    case e: ClassCastException ⇒ throw new SkillException(
      s""\"tried to access a "$$name" at index $$index, but it was actually a $${
        basePool.data(index.toInt - 1).getClass().getName()
      }""\", e
    )
  }
}
""")

    //class prefix
    out.close()
  }
}
