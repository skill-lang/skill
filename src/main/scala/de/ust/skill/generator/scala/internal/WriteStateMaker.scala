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
import ${packagePrefix}internal.pool.KnownPool

/**
 * holds state of a write operation
 * @author Timm Felden
 */
private[internal] final class WriteState(state: SerializableState) extends SerializationFunctions(state) {
  import SerializationFunctions._
  import state._

  def getByID[T <: SkillType](typeName: String, id: Long): T = d(typeName)(id.toInt).asInstanceOf[T]

  /**
   * prepares a state, i.e. calculates d-map and lpbsi-map
   */
  val d = new HashMap[String, ArrayBuffer[SkillType]]
  // store static instances in d
  knownPools.foreach { p ⇒
    val ab = new ArrayBuffer[SkillType](p.staticSize.toInt);
    p.staticInstances.foreach { i ⇒
      ab += i
      // TODO i.setID(ab.size)
    }
    d.put(p.name, ab)
  }

  // make lbpsi map and update d-maps
  val lbpsiMap = new HashMap[String, Long]
  pools.values.foreach {
    case p: BasePool[_] ⇒
      p.makeLBPSIMap(lbpsiMap, 0, { s ⇒ d(s).size })
      p.concatenateDMap(d)
    case _ ⇒
  }

  override def annotation(ref: SkillType): List[Array[Byte]] = {
    val baseName = state.pools(ref.getClass.getSimpleName.toLowerCase).asInstanceOf[KnownPool[_, _]].basePool.name

    List(v64(state.strings.serializationIDs(baseName)), v64(ref.getSkillID))
  }
}
""")

    //class prefix
    out.close()
  }
}
