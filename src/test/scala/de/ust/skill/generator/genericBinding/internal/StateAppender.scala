/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 29.10.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import java.util.Arrays
import java.nio.ByteBuffer

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import de.ust.skill.generator.genericBinding._
import de.ust.skill.generator.genericBinding.internal.streams.FileOutputStream
import de.ust.skill.generator.genericBinding.internal.streams.OutStream

/**
 * Holds state of a write operation.
 *
 * @see SKilL §6
 * @author Timm Felden
 */
private[internal] final class StateAppender(state : SerializableState, out : FileOutputStream) extends SerializationFunctions(state) {
  import SerializationFunctions._

  // save the index of the first new pool
  val newPoolIndex = state.pools.indexWhere(_.blockInfos.isEmpty) match {
    case -1L ⇒ state.pools.size + 1 // ensure that no pool is marked as *new*
    case i   ⇒ i.toLong
  }

  // make lbpsi map, update data map to contain dynamic instances and create serialization skill IDs for serialization
  // index → bpsi
  val lbpsiMap = new Array[Long](state.pools.length)
  val chunkMap = HashMap[FieldDeclaration[_], ChunkInfo]()
  state.pools.foreach {
    case p : BasePool[_] ⇒
      makeLBPSIMap(p, lbpsiMap, 1, { s ⇒ state.poolByName(s).newObjects.size })
      //@note it is very important to prepare after the creation of the lbpsi map
      p.prepareAppend(chunkMap)
    case _ ⇒
  }

  /**
   * ****************
   * PHASE 3: WRITE *
   * ****************
   */

  // write string block
  state.String.asInstanceOf[StringPool].prepareAndAppend(out, this)

  // write count of the type block
  v64(state.pools.filter { p ⇒
    p.poolIndex >= newPoolIndex || (p.dynamicSize > 0 && p.fields.exists(chunkMap.contains(_)))
  }.size, out)

  // TODO see code generator

  out.close
}
