/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait SubPoolIndexedIteratorMaker extends GeneralOutputMaker{
  abstract override def make {
    super.make
    val out = open("internal/SubPoolIndexIterator.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import ${packagePrefix}api.KnownType
import ${packagePrefix}internal.pool.SubPool

/**
 * Implements iteration over members of a storage pool. This class is restricted to sub pools, because base pools
 * have a simple and more efficient implementation.
 *
 * @param T is the API type of the objects stored in the static type storage pool
 * @author Timm Felden
 */
final class SubPoolIndexIterator[T <: KnownType] private[internal] (val staticType: SubPool[T, _]) extends Iterator[T] {

  // pools are usually separated into several blocks and new instances; block -2 means the iterator is invalid
  private var currentBlock = -2
  private val blocks = staticType.userType.blockInfos
  // the index in the dynamic type pool
  private var index = 0L
  // move to the first valid position
  reset

  // the current type we are looking into
  private var dynamicType: SubPool[_ <: T, _] = staticType

  /**
   * note that this iterator is based on the current index, therefore it might behave in an unexpected way in case of
   *  parallel modification.
   *
   * @pre valid
   * @return the current element
   */
  @inline private def get(): T = {
    if (-1 == currentBlock)
      dynamicType.newObjects(index.toInt)
    else
      staticType.getByID(index)
  }

  /**
   * move to the next valid element, if any exists; invalidate the iterator otherwise
   */
  @inline override def next(): T = {
    val result = get

    // are we still in the block part?
    if (-1 != currentBlock && blocks.contains(currentBlock)) {
      // assume the next position is the next index
      index += 1

      // check if there is another instance in the current block
      if (blocks(currentBlock).bpsi + blocks(currentBlock).count > index)
        return result
      else {
        // check if there is another instane in a block
        for (i ← currentBlock + 1 until staticType.blockCount) {
          if (blocks.contains(i) && blocks(i).count > 0) {
            index = blocks(i).bpsi
            currentBlock = i
            return result
          }
        }
      }
      // we iterated over all instances from the deserialized file; invalidate the currentBlock
      currentBlock = -1
      index = -1
    }

    // we are in the newObjects part; reset index and abuse it to move through the dynamic type stack
    if (-1 == currentBlock) {
      index += 1
      if (index < dynamicType.newObjects.length) {
        return result
      }

      // TODO check if next points to a type above staticType

      //advance to next pool
      var next = dynamicType.next
      index = 0
      while (null != next && 0 == index) {

        if (next.isInstanceOf[SubPool[_ <: T, _]]) {
          dynamicType = next.asInstanceOf[SubPool[_ <: T, _]]
          if (0 != dynamicType.newObjects.length) {
            index = 0
            return result
          }
        }

        next = next.next
      }
    }

    // invalidate the iterator, because there are no more instances
    currentBlock = -2
    return result
  }

  /**
   * check if the iterator is still valid; the obvious way of invalidating an iterator is to walk over all iterable
   *  elemnts.
   */
  @inline def hasNext = -2 != currentBlock

  /**
   * resets the cursor to the first valid position, if any
   *
   * TODO move through newObjects as well; this is required if the loaded file did not contain any instances of
   *  static type
   */
  def reset() {
    currentBlock = 0
    val max = staticType.blockCount
    for (i ← 0 until max) {
      if (blocks.contains(i) && blocks(i).count > 0) {
        index = blocks(i).bpsi
        currentBlock = i
        return
      }
    }
    // there are no instances inside the part that is read from file
    currentBlock = if (staticType.dynamicSize > 0) -1 else -2
  }
}
""")

    //class prefix
    out.close()
  }
}
