/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait WriteStateMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/WriteState.scala")
    //package
    out.write(s"""package ${packagePrefix}internal

import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer

import ${packagePrefix}api.SkillType
import ${packagePrefix}api.SkillState
import ${packagePrefix}internal.pool.BasePool

/**
 * holds state of a write operation
 * @author Timm Felden
 */
private[internal] final class WriteState(state: SerializableState) {
  import state._

  def getByID[T <: SkillType](typeName: String, id: Long): T = d(typeName)(id.toInt).asInstanceOf[T]
  def getByRef[T <: SkillType](typeName: String, ref: T): Long = d(typeName).indexOf(ref)

  /**
   * prepares a state, i.e. calculates d-map and lpbsi-map
   */
  val d = new HashMap[String, ArrayBuffer[SkillType]]
  // store static instances in d
  knownPools.foreach { p ⇒
    d.put(p.name, new ArrayBuffer[SkillType])
  }
  knownPools.foreach {
    case p: BasePool[_] ⇒ p.foreach { i ⇒
      d(i.getClass.getSimpleName.toLowerCase) += i
    }
    case _ ⇒
  }

  // make lbpsi map and update d-maps
  val lbpsiMap = new HashMap[String, Long]
  pools.values.foreach {
    case p: BasePool[_] ⇒
      p.makeLBPSIMap(lbpsiMap, 0, { s ⇒ d(s).size })
      p.concatenateDMap(d)
    case _ ⇒
  }
}
""")

    //class prefix
    out.close()
  }
}
