package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

/**
 * @author Jonathan
 */
trait RemappingFunctionsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/RemappingFunctions.scala")
    //package
    out.write(s"""package ${packagePrefix}internal

import scala.annotation.tailrec

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap

/**
 * Remappings contain data to remap ids in fields. This is a value type.
 * 
 * @author Jonathan Roth
 */
class RemappingInfo(val data : Long) extends AnyVal {
  def isDeleted = data < 0
  def start = ((data >> 32) & 0x7FFFFFFF).toInt
  def offset = data.toInt
}
object RemappingInfo {
  /**
   * creates a remapping info with given start and offset. skillIDs inside the described block are valid.
   */
  def apply(start : Int, offset : Int) = new RemappingInfo((start.toLong << 32) | (offset.toLong & 0xFFFFFFFFL)) // avoid sign extend
  /**
   * creates a remapping info with given start. skillIDs inside the described block are invalid (that is, mapped to 0).
   */ 
  def apply(start : Int) = new RemappingInfo((start.toLong << 32) | 0x8000000000000000L)
}

/**
 * This object contains helpers for remapping.
 * All public functions accept an array that contains references or annotations
 * (directly or indirectly) to be remapped and a remapping table.
 * The array given as parameter is reused, only its content is changed.
 */
object RemappingFunctions {
  // remapping of a single reference/annotation
  @inline private def remapReference(ref : Int, map : (LongArrayBuffer[RemappingInfo], Array[Int])) = {
    // remaps a reference to an old instance with the given map with
    // complexity O(log(n)) where n = map.length.
    @inline def mapOld(map : LongArrayBuffer[RemappingInfo], ref : Int) = {
      // search the map for the right range with binary search
      @inline @tailrec def searchMap(start : Int, end : Int) : RemappingInfo = {
        if (ref >= map(end).start)
          map(end)
        else {
          val mid = (start + end) / 2
          val min = map(mid).start
          if (min <= ref && ref < map(mid + 1).start)
            map(mid)
          else if (ref < min)
            searchMap(start, mid - 1)
          else
            searchMap(mid + 1, end)
        }
      }
      
      if (ref >= map.last.start) {
        if (map.last.isDeleted) 0 else ref + map.last.offset
      }
      else {
        val rmi = searchMap(0, map.length - 1)
        if (rmi.isDeleted) 0 else ref + rmi.offset
      }
    }

    if (ref < 0)
      map._2(-ref - 1)
    else if (ref > 0)
      mapOld(map._1, ref)
    else
      0
  }

  @inline private def remapReference(ref : Int, map : Array[Int]) = if (ref < 0) map(-ref - 1) else ref

  @inline private def remapAnnotation(ref : Long, map : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) = {
    // remaps an annotation to an old instance with the given map with
    // complexity O(log(n)) where n = map.length.
    @inline def mapOld(map : LongArrayBuffer[RemappingInfo], ref : Long) = {
      // search the map for the right range with binary search
      @inline @tailrec def searchMap(start : Int, end : Int) : RemappingInfo = {
        if (ref >= map(end).start)
          map(end)
        else {
          val mid = (start + end) / 2
          val min = map(mid).start
          if (min <= ref && ref < map(mid + 1).start)
            map(mid)
          else if (ref < min)
            searchMap(start, mid - 1)
          else
            searchMap(mid + 1, end)
        }
      }
      
      if (ref >= map.last.start) {
        if (map.last.isDeleted) 0 else ref + map.last.offset
      }
      else {
        val rmi = searchMap(0, map.length - 1)
        if (rmi.isDeleted) 0 else ref + rmi.offset
      }
    }

    val a = new AnnotationRef(ref)
    val pool = a.typeIndex
    a.skillID match {
      case id if id < 0 ⇒ AnnotationRef.build(pool, map(pool)._2(-id - 1))
      case id if id > 0 ⇒ mapOld(map(pool)._1, id)
      case _ ⇒ 0L
    }
  }

  @inline private def remapAnnotation(ref : Long, map : Array[Array[Int]]) = {
    val a = new AnnotationRef(ref)
    val pool = a.typeIndex
    a.skillID match {
      case id if id < 0 ⇒ AnnotationRef.build(pool, map(pool)(-id - 1))
      case _ ⇒ ref
    }
  }

  // remappings for different field types
  /**
   * remaps all references in a field of references.
   */
  @inline def remapField(data : Array[Int], map : (LongArrayBuffer[RemappingInfo], Array[Int])) =
    for (i ← data.indices) data(i) = remapReference(data(i), map)
  /**
   * remaps all references to new objects in a field of references.
   */
  @inline def remapField(data : Array[Int], map : Array[Int]) =
    for (i ← data.indices) data(i) = remapReference(data(i), map)
  /**
   * remaps all annotations in a field of annotations.
   */
  @inline def remapField(data : Array[Long], map : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) =
    for (i ← data.indices) data(i) = remapAnnotation(data(i), map)
  /**
   * remaps all annotations to new objects in a field of annotations.
   */
  @inline def remapField(data : Array[Long], map : Array[Array[Int]]) =
    for (i ← data.indices) data(i) = remapAnnotation(data(i), map)

  /**
   * remaps all references in an array of references.
   */
  @inline def remapArray(data : Array[Array[Int]], map : (LongArrayBuffer[RemappingInfo], Array[Int])) =
    for (i ← data.indices) remapField(data(i), map)
  /**
   * remaps all references to new objects in an array of references.
   */
  @inline def remapArray(data : Array[Array[Int]], map : Array[Int]) =
    for (i ← data.indices) remapField(data(i), map)
  /**
   * remaps all annotations in an array of annotations.
   */
  @inline def remapArray(data : Array[Array[Long]], map : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) =
    for (i ← data.indices) remapField(data(i), map)
  /**
   * remaps all annotations to new objects in an array of annotations.
   */
  @inline def remapArray(data : Array[Array[Long]], map : Array[Array[Int]]) =
    for (i ← data.indices) remapField(data(i), map)

  /**
   * remaps all references in an ArrayBuffer of references.
   */
  @inline def remapArrayBuffer(data : Array[ArrayBuffer[Int]], map : (LongArrayBuffer[RemappingInfo], Array[Int])) =
    for (buf ← data; i ← buf.indices) buf(i) = remapReference(buf(i), map)
  /**
   * remaps all references to new objects in an ArrayBuffer of references.
   */
  @inline def remapArrayBuffer(data : Array[ArrayBuffer[Int]], map : Array[Int]) =
    for (buf ← data; i ← buf.indices) buf(i) = remapReference(buf(i), map)
  /**
   * remaps all annotations in an ArrayBuffer of annotations.
   */
  @inline def remapArrayBuffer(data : Array[ArrayBuffer[Long]], map : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) =
    for (buf ← data; i ← buf.indices) buf(i) = remapAnnotation(buf(i), map)
  /**
   * remaps all annotations to new objects in an ArrayBuffer of annotations.
   */
  @inline def remapArrayBuffer(data : Array[ArrayBuffer[Long]], map : Array[Array[Int]]) =
    for (buf ← data; i ← buf.indices) buf(i) = remapAnnotation(buf(i), map)

  /**
   * remaps all references in a ListBuffer of references.
   */
  @inline def remapListBuffer(data : Array[ListBuffer[Int]], map : (LongArrayBuffer[RemappingInfo], Array[Int])) =
    for (i ← data.indices) {
      val res = ListBuffer[Int]()
      for (ref ← data(i))
        res += remapReference(ref, map)
      data(i) = res
    }
  /**
   * remaps all references to new objects in a ListBuffer of references.
   */
  @inline def remapListBuffer(data : Array[ListBuffer[Int]], map : Array[Int]) =
    for (i ← data.indices) {
      val res = ListBuffer[Int]()
      for (ref ← data(i))
        res += remapReference(ref, map)
      data(i) = res
    }
  /**
   * remaps all annotations in a ListBuffer of annotations.
   */
  @inline def remapListBuffer(data : Array[ListBuffer[Long]], map : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) =
    for (i ← data.indices) {
      val res = ListBuffer[Long]()
      for (ref ← data(i))
        res += remapAnnotation(ref, map)
      data(i) = res
    }
  /**
   * remaps all annotations to new objects in a ListBuffer of annotations.
   */
  @inline def remapListBuffer(data : Array[ListBuffer[Long]], map : Array[Array[Int]]) =
    for (i ← data.indices) {
      val res = ListBuffer[Long]()
      for (ref ← data(i))
        res += remapAnnotation(ref, map)
      data(i) = res
    }

  /**
   * remaps all references in a HashSet of references.
   */
  @inline def remapHashSet(data : Array[HashSet[Int]], map : (LongArrayBuffer[RemappingInfo], Array[Int])) =
    for (i ← data.indices) {
      val res = HashSet[Int]()
      for (ref ← data(i))
        res += remapReference(ref, map)
      data(i) = res
    }
  /**
   * remaps all references to new objects in a HashSet of references.
   */
  @inline def remapHashSet(data : Array[HashSet[Int]], map : Array[Int]) =
    for (i ← data.indices) {
      val res = HashSet[Int]()
      for (ref ← data(i))
        res += remapReference(ref, map)
      data(i) = res
    }
  /**
   * remaps all annotations in a HashSet of annotations.
   */
  @inline def remapHashSet(data : Array[HashSet[Long]], map : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) =
    for (i ← data.indices) {
      val res = HashSet[Long]()
      for (ref ← data(i))
        res += remapAnnotation(ref, map)
      data(i) = res
    }
  /**
   * remaps all annotations to new objects in a HashSet of annotations.
   */
  @inline def remapHashSet(data : Array[HashSet[Long]], map : Array[Array[Int]]) =
    for (i ← data.indices) {
      val res = HashSet[Long]()
      for (ref ← data(i))
        res += remapAnnotation(ref, map)
      data(i) = res
    }

  /**
   * remaps all reference keys in a HashMap.
   */
  @inline def remapHashMapKeys[B](data : Array[HashMap[Int, B]], map : (LongArrayBuffer[RemappingInfo], Array[Int])) =
    for (i ← data.indices) {
      val res = HashMap[Int, B]()
      for ((ref, value) ← data(i))
        res += ((remapReference(ref, map), value))
      data(i) = res
    }
  /**
   * remaps all reference to new object keys in a HashMap.
   */
  @inline def remapHashMapKeys[B](data : Array[HashMap[Int, B]], map : Array[Int]) =
    for (i ← data.indices) {
      val res = HashMap[Int, B]()
      for ((ref, value) ← data(i))
        res += ((remapReference(ref, map), value))
      data(i) = res
    }
  /**
   * remaps all annotation keys in a HashMap.
   */
  @inline def remapHashMapKeys[B](data : Array[HashMap[Long, B]], map : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) =
    for (i ← data.indices) {
      val res = HashMap[Long, B]()
      for ((ref, value) ← data(i))
        res += ((remapAnnotation(ref, map), value))
      data(i) = res
    }
  /**
   * remaps all annotation to new object keys in a HashMap.
   */
  @inline def remapHashMapKeys[B](data : Array[HashMap[Long, B]], map : Array[Array[Int]]) =
    for (i ← data.indices) {
      val res = HashMap[Long, B]()
      for ((ref, value) ← data(i))
        res += ((remapAnnotation(ref, map), value))
      data(i) = res
    }

  /**
   * remaps all reference values in a HashMap.
   */
  @inline def remapHashMapValues[A](data : Array[HashMap[A, Int]], map : (LongArrayBuffer[RemappingInfo], Array[Int])) =
    for (hm ← data; key ← hm.keys) hm(key) = remapReference(hm(key), map)
  /**
   * remaps all reference to new object values in a HashMap.
   */
  @inline def remapHashMapValues[A](data : Array[HashMap[A, Int]], map : Array[Int]) =
    for (hm ← data; key ← hm.keys) hm(key) = remapReference(hm(key), map)
  /**
   * remaps all annotation values in a HashMap.
   */
  @inline def remapHashMapValues[A](data : Array[HashMap[A, Long]], map : Array[(LongArrayBuffer[RemappingInfo], Array[Int])]) =
    for (hm ← data; key ← hm.keys) hm(key) = remapAnnotation(hm(key), map)
  /**
   * remaps all annotation to new object values in a HashMap.
   */
  @inline def remapHashMapValues[A](data : Array[HashMap[A, Long]], map : Array[Array[Int]]) =
    for (hm ← data; key ← hm.keys) hm(key) = remapAnnotation(hm(key), map)
}
""")

    //class prefix
    out.close()
  }
}