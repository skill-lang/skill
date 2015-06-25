/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait ContainersMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/Containers.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import scala.language.implicitConversions
import scala.collection.GenTraversableOnce
import scala.collection.mutable._
import scala.collection.generic.{ GenericTraversableTemplate, GenericSetTemplate }
import scala.collection.generic.{ SeqFactory, MutableSetFactory, MutableMapFactory }
import scala.collection.parallel.mutable.{ ParArray, ParHashSet, ParHashMap }
import scala.collection.CustomParallelizable
import scala.reflect.ClassTag""")

    out.write("""

/**
 * Wrapper class for an array of any 8 byte large type where implicit converters to and from Long exist.
 * Used as a performance hack. First array entry is misused for real size.
 * 
 * Even if this class is immutable (because of several reasons, e.g. AnyVal),
 * treat it as if it was mutable, because the array is reused if possible, and use a var.
 * 
 * @author Jonathan Roth
 */
class LongArrayBuffer[T] private (private val _array : Array[Long]) extends AnyVal {
  @inline def apply(index : Int)(implicit convert: LongConverter[T]) : T = if (index < length) convert.from(_array(index + 1)) else null.asInstanceOf[T]
  @inline def update(index : Int, t : T)(implicit convert: LongConverter[T]) = if (index < length) _array(index + 1) = convert.to(t)
  @inline def length = _array(0).toInt
  @inline private def length_=(value : Int) = _array(0) = value
  @inline def size = length
  @inline def isEmpty = _array(0) == 0
  @inline def head(implicit convert: LongConverter[T]) = convert.from(_array(1))
  @inline def head_=(value : T)(implicit convert: LongConverter[T]) = _array(1) = convert.to(value)
  @inline def last(implicit convert: LongConverter[T]) = convert.from(_array(length))
  @inline def last_=(value : T)(implicit convert: LongConverter[T]) = _array(length) = convert.to(value)
  @inline private def start = 1
  @inline private def end = length + 1
  
  @inline private def mustResize = length >= _array.length
  @inline private def resizeConstant = 128
    
  // use as +=
  def +(value : T)(implicit convert: LongConverter[T]) = {
    length += 1
    if (mustResize) {
      // resize
      val res = new LongArrayBuffer[T](new Array[Long](_array.length + resizeConstant))
      _array.copyToArray(res._array)
      res.last = value
      res
    }
    else {
      last = value
      new LongArrayBuffer[T](_array)
    }
  }
  // @note: strange syntax, but allows /-= cap to clear buffer and set new capacity
  def /-(cap : Int) = {
    if (cap + 1 == _array.length) {
      length = 0
      new LongArrayBuffer[T](_array)
    }
    else
      LongArrayBuffer[T](cap)
  }
    
  @inline def foreach[U](f : (T) ⇒ U)(implicit convert: LongConverter[T]) {
    for (i ← start until end)
      f(convert.from(_array(i)))
  }
  @inline def filter(f : (T) ⇒ Boolean)(implicit convert: LongConverter[T]) = {
    new LongArrayBuffer[T](_array.view(start, end).filter(l ⇒ f(convert.from(l))).toArray)
  }
  
  @inline def count(f : (T) ⇒ Boolean)(implicit convert: LongConverter[T]) = {
    var sum = 0
    for (i ← start until end if f(convert.from(_array(i)))) sum += 1
    sum
  }
  @inline def map[B : ClassTag](f : (T) ⇒ B)(implicit convert: LongConverter[T]) = {
    val res = new Array[B](length)
    for (i ← start until end)
      res(i - start) = f(convert.from(_array(i)))
    res
  }
  @inline def flatMap[B](f : (T) ⇒ GenTraversableOnce[B])(implicit convert: LongConverter[T]) =
    _array.view(start, end).flatMap(l ⇒ f(convert.from(l)))
  @inline def iterator(implicit convert: LongConverter[T]) = _array.view(start, end).iterator.map(l ⇒ convert.from(l))
  
  /**
   * sorts the underlying array with respect to the given lower than function
   */
  @inline def sort(lt : (T, T) ⇒ Boolean)(implicit convert: LongConverter[T]) =
    _array.view(start, end).sortWith((l, r) ⇒ lt(convert.from(l), convert.from(r))).copyToArray(_array, 1)
}
object LongArrayBuffer {
  def apply[T](capacity : Int) = {
    val array = new Array[Long](capacity + 1)
    array(0) = 0
    new LongArrayBuffer[T](array)
  }
}

/**
 * converters for LongArrayBuffers.
 * 
 * @tparam T the visible (interface) type
 * 
 * @author Jonathan Roth
 */
trait LongConverter[T] {
  def from(l : Long) : T
  def to(t : T) : Long
}

object LongConverter {
  implicit object BlockInfoLongConverter extends LongConverter[BlockInfo] {
    @inline def from(l : Long) = BlockInfo((l >> 32).toInt, l.toInt)
    @inline def to(bi : BlockInfo) = (bi.bpsi.toLong << 32) | bi.count
  }
  
  implicit object RemappingInfoLongConverter extends LongConverter[RemappingInfo] {
    @inline def from(l : Long) = new RemappingInfo(l)
    @inline def to(ri : RemappingInfo) = ri.data
  }
}

/**
 * Space-optimized array for booleans. A boolean uses only 1 bit
 * 
 * @author Jonathan Roth
 */
final class BooleanArray(val _length : Int)
  extends IndexedSeq[Boolean] with ArrayLike[Boolean, BooleanArray] {
  private def this(length : Int, _data : Array[Int]) {
    this(length)
    _data.copyToArray(data)
  }
  
  private val data = new Array[Int]((_length + 31) / 32)
  
  def apply(index : Int) : Boolean = {
    if (index < _length) {
      val sub = index % 32
      val i = index / 32
      (data(i) & (1 << sub)) != 0
    }
    else
      false
  }
  def update(index : Int, value : Boolean) = {
    if (index < _length) {
      val sub = 1 << (index % 32)
      val i = index / 32
      if (value)
        data(i) |= sub
      else
        data(i) &= ~sub
    }
  }
  @inline def length = _length
  @inline override def clone = new BooleanArray(_length, data)
  
  @inline def copyToArray(other : BooleanArray) {
    data.copyToArray(other.data)
  }
  
  protected[this] override def newBuilder = ???
}

/**
 * View for a fixed-size array of references. Only used for fixed-size array fields in proxies
 * 
 * @author Jonathan Roth
 */
final class RefArray[T <: SkillType] private[internal] (private[internal] val data : Array[Int], private val resolve : (Int) ⇒ T)
  extends IndexedSeq[T] with ArrayLike[T, RefArray[T]] {
  @inline override def apply(index : Int) : T = resolve(data(index))
  @inline override def update(index : Int, value : T) = data(index) = value.reference
  @inline override def length = data.length
  
  protected[this] override def newBuilder = ???
}

object RefArray {
  @inline private[internal] def apply[T <: SkillType](length : Int, resolve : (Int) ⇒ T) = new RefArray[T](new Array[Int](length), resolve)
}

/**
 * View for a fixed-size array of annotations. Only used for fixed-size array fields in proxies
 * 
 * @author Jonathan Roth
 */
final class AnnotationArray private[internal] (private[internal] val data : Array[Long], private val resolve : (Long) ⇒ SkillType)
  extends IndexedSeq[SkillType] with ArrayLike[SkillType, AnnotationArray] {
  @inline override def apply(index : Int) : SkillType = resolve(data(index))
  @inline override def update(index : Int, value : SkillType) = data(index) = value.annotation
  @inline override def length = data.length
  
  protected[this] override def newBuilder = ???
}

object AnnotationArray {
  @inline private[internal] def apply(length : Int, resolve : (Long) ⇒ SkillType) = new AnnotationArray(new Array[Long](length), resolve)
}

/**
 * This trait solves a proplem with scala.collection.generic.GenericTraversableTemplate. Some traits
 * like Buffer and ResizableArray use it, but their type parameters are different, what is forbidden
 * on the JVM. Also, the type parameter of VariableRefArray is constrained and VariableAnnotationArray
 * has no type parameter, so they can't solve the problem.
 */
sealed trait VariableArray[T]
  // copied from scala.collection.mutable.ArrayBuffer
  extends AbstractBuffer[T] with Buffer[T] with GenericTraversableTemplate[T, VariableArray] with BufferLike[T, VariableArray[T]]
  with IndexedSeqOptimized[T, VariableArray[T]] with Builder[T, VariableArray[T]] with ResizableArray[T]
  with CustomParallelizable[T, ParArray[T]] with Serializable {
  
  @inline override def companion = VariableArray
}
object VariableArray extends SeqFactory[VariableArray] with Serializable {
  @inline override def newBuilder[T] = ???
}

/**
 * View for a variable-size array of references. Only used for variable-size array fields in proxies
 * 
 * @note may not be optimal to use ArrayBuffers. Perhaps self-implemented specializations should be used.
 * 
 * @author Jonathan Roth
 */
final class RefArrayBuffer[T <: SkillType] private[internal] (private[internal] val data : ArrayBuffer[Int], private val resolve : (Int) ⇒ T)
  extends VariableArray[T] {
  
  // define all functions implemented by ArrayBuffer
  @inline override def ++=(xs : TraversableOnce[T]) = { data ++= xs.map(x ⇒ x.reference); this }
  @inline override def ++=:(xs : TraversableOnce[T]) = { xs.map(x ⇒ x.reference) ++=: data; this }
  @inline override def +=(elem : T) = { data += elem.reference; this }
  @inline override def +=:(elem : T) = { elem.reference +=: data; this }
  @inline override def apply(index : Int) = resolve(data(index))
  @inline override def clear = data.clear
  @inline override def insertAll(n : Int, elems : scala.collection.Traversable[T]) = data.insertAll(n, elems.map(x ⇒ x.reference))
  @inline override def iterator = data.iterator.map(resolve)
  @inline override def length = data.length
  @inline override def par = data.par.map(resolve)
  @inline override def remove(index : Int) = resolve(data.remove(index))
  @inline override def remove(index : Int, count : Int) = data.remove(index, count)
  @inline override def result = this
  @inline override def sizeHint(len : Int) = data.sizeHint(len)
  @inline override def update(index : Int, newelem : T) = data(index) = newelem.reference
}

object RefArrayBuffer {
  @inline private[internal] def apply[T <: SkillType](resolve : (Int) ⇒ T) = new RefArrayBuffer[T](new ArrayBuffer[Int](), resolve)
}

/**
 * View for a variable-size array of annotations. Only used for variable-size array fields in proxies
 * 
 * @note may not be optimal to use ArrayBuffers. Perhaps self-implemented specializations should be used.
 * 
 * @author Jonathan Roth
 */
final class AnnotationArrayBuffer private[internal] (private[internal] val data : ArrayBuffer[Long], private val resolve : (Long) ⇒ SkillType)
  extends VariableArray[SkillType] {
  
  // define all functions implemented by ArrayBuffer
  @inline override def ++=(xs : TraversableOnce[SkillType]) = { data ++= xs.map(x ⇒ x.annotation); this }
  @inline override def ++=:(xs : TraversableOnce[SkillType]) = { xs.map(x ⇒ x.annotation) ++=: data; this }
  @inline override def +=(elem : SkillType) = { data += elem.annotation; this }
  @inline override def +=:(elem : SkillType) = { elem.annotation +=: data; this }
  @inline override def apply(index : Int) = resolve(data(index))
  @inline override def clear = data.clear
  @inline override def insertAll(n : Int, elems : scala.collection.Traversable[SkillType]) = data.insertAll(n, elems.map(x ⇒ x.annotation))
  @inline override def iterator = data.iterator.map(resolve)
  @inline override def length = data.length
  @inline override def par = data.par.map(resolve)
  @inline override def remove(index : Int) = resolve(data.remove(index))
  @inline override def remove(index : Int, count : Int) = data.remove(index, count)
  @inline override def result = this
  @inline override def sizeHint(len : Int) = data.sizeHint(len)
  @inline override def update(index : Int, newelem : SkillType) = data(index) = newelem.annotation
}

object AnnotationArrayBuffer {
  @inline private[internal] def apply(resolve : (Long) ⇒ SkillType) = new AnnotationArrayBuffer(new ArrayBuffer[Long](), resolve)
}

sealed trait ListView[T]
  // copied from scala.collection.mutable.ListBuffer
  extends AbstractBuffer[T] with Buffer[T] with GenericTraversableTemplate[T, ListView] with BufferLike[T, ListView[T]]
  with Builder[T, scala.collection.immutable.List[T]] with java.io.Serializable {
  
  @inline override def companion = ListView
}
object ListView extends SeqFactory[ListView] with Serializable {
  @inline override def newBuilder[T] = ???
}

/**
 * View for a list of references. Only used for list fields in proxies
 * 
 * @author Jonathan Roth
 */
final class RefListBuffer[T <: SkillType] private[internal] (private[internal] val data : ListBuffer[Int], private val resolve : (Int) ⇒ T)
  extends ListView[T] {
  
  // define all functions implemented by ListBuffer
  @inline override def ++=(xs : TraversableOnce[T]) = { data ++= xs.map(x ⇒ x.reference); this }
  @inline override def ++=:(xs : TraversableOnce[T]) = { xs.map(x ⇒ x.reference) ++=: data; this }
  @inline override def +=(elem : T) = { data += elem.reference; this }
  @inline override def +=:(elem : T) = { elem.reference +=: data; this }
  @inline override def apply(index : Int) = resolve(data(index))
  @inline override def clear = data.clear
  @inline override def clone = new RefListBuffer[T](data.clone, resolve)
  @inline override def insertAll(n : Int, elems : scala.collection.Traversable[T]) = data.insertAll(n, elems.map(x ⇒ x.reference))
  @inline override def iterator = data.iterator.map(resolve)
  @inline override def length = data.length
  @inline def prependToList(xs : scala.collection.immutable.List[T]) = data.map(resolve).prependToList(xs)
  @inline override def remove(index : Int) = resolve(data.remove(index))
  @inline override def remove(index : Int, count : Int) = data.remove(index, count)
  @inline override def result = data.result.map(resolve)
  @inline override def size = data.size
  @inline override def toList = data.toList.map(resolve)
  @inline override def update(index : Int, newelem : T) = data(index) = newelem.reference
}

object RefListBuffer {
  @inline private[internal] def apply[T <: SkillType](resolve : (Int) ⇒ T) = new RefListBuffer[T](new ListBuffer[Int](), resolve)
}

/**
 * View for a list of annotations. Only used for list fields in proxies
 * 
 * @author Jonathan Roth
 */
final class AnnotationListBuffer private[internal] (private[internal] val data : ListBuffer[Long], private val resolve : (Long) ⇒ SkillType)
  extends ListView[SkillType] {
  
  // define all functions implemented by ListBuffer
  @inline override def ++=(xs : TraversableOnce[SkillType]) = { data ++= xs.map(x ⇒ x.annotation); this }
  @inline override def ++=:(xs : TraversableOnce[SkillType]) = { xs.map(x ⇒ x.annotation) ++=: data; this }
  @inline override def +=(elem : SkillType) = { data += elem.annotation; this }
  @inline override def +=:(elem : SkillType) = { elem.annotation +=: data; this }
  @inline override def apply(index : Int) = resolve(data(index))
  @inline override def clear = data.clear
  @inline override def clone = new AnnotationListBuffer(data.clone, resolve)
  @inline override def insertAll(n : Int, elems : scala.collection.Traversable[SkillType]) = data.insertAll(n, elems.map(x ⇒ x.annotation))
  @inline override def iterator = data.iterator.map(resolve)
  @inline override def length = data.length
  @inline def prependToList(xs : scala.collection.immutable.List[SkillType]) = data.map(resolve).prependToList(xs)
  @inline override def remove(index : Int) = resolve(data.remove(index))
  @inline override def remove(index : Int, count : Int) = data.remove(index, count)
  @inline override def result = data.result.map(resolve)
  @inline override def size = data.size
  @inline override def toList = data.toList.map(resolve)
  @inline override def update(index : Int, newelem : SkillType) = data(index) = newelem.annotation
}

object AnnotationListBuffer {
  @inline private[internal] def apply(resolve : (Long) ⇒ SkillType) = new AnnotationListBuffer(new ListBuffer[Long](), resolve)
}

sealed trait SetView[T]
  // copied from scala.collection.mutable.HashSet
  extends AbstractSet[T] with Set[T] with GenericSetTemplate[T, SetView] with SetLike[T, SetView[T]] with FlatHashTable[T]
  with CustomParallelizable[T, ParHashSet[T]] with Serializable {
  
  // dummy implementation; FlatHashTable[T].iterator has weaker access than AbstractSet[T].iterator,
  // so it has to be overridden
  @inline override def iterator : Iterator[T] = ???
  @inline override def companion = SetView
}
object SetView extends MutableSetFactory[SetView] with Serializable {
  @inline override def empty[T] = ???
}

/**
 * View for a set of references. Only used for set fields in proxies
 * 
 * @author Jonathan Roth
 */
final class RefHashSet[T <: SkillType] private[internal] (private[internal] val data : HashSet[Int], private val resolve : (Int) ⇒ T)
  extends SetView[T] {
  
  // define all functions implemented by HashSet
  @inline override def +=(elem : T) = { data += elem.reference; this }
  @inline override def -=(elem : T) = { data -= elem.reference; this }
  @inline override def add(elem : T) = data.add(elem.reference)
  @inline override def clear = data.clear
  @inline override def clone = new RefHashSet[T](data.clone, resolve)
  @inline override def contains(elem : T) = data.contains(elem.reference)
  @inline override def foreach[U](f : (T) ⇒ U) = data.foreach(x ⇒ f(resolve(x)))
  @inline override def iterator = data.iterator.map(resolve)
  @inline override def par = data.par.map(resolve)
  @inline override def remove(elem : T) = data.remove(elem.reference)
  @inline override def size = data.size
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
}

object RefHashSet {
  @inline private[internal] def apply[T <: SkillType](resolve : (Int) ⇒ T) = new RefHashSet[T](new HashSet[Int](), resolve)
}

/**
 * View for a set of annotations. Only used for set fields in proxies
 * 
 * @author Jonathan Roth
 */
final class AnnotationHashSet private[internal] (private[internal] val data : HashSet[Long], private val resolve : (Long) ⇒ SkillType)
  extends SetView[SkillType] {
  
  // define all functions implemented by HashSet
  @inline override def +=(elem : SkillType) = { data += elem.annotation; this }
  @inline override def -=(elem : SkillType) = { data -= elem.annotation; this }
  @inline override def add(elem : SkillType) = data.add(elem.annotation)
  @inline override def clear = data.clear
  @inline override def clone = new AnnotationHashSet(data.clone, resolve)
  @inline override def contains(elem : SkillType) = data.contains(elem.annotation)
  @inline override def foreach[U](f : (SkillType) ⇒ U) = data.foreach(x ⇒ f(resolve(x)))
  @inline override def iterator = data.iterator.map(resolve)
  @inline override def par = data.par.map(resolve)
  @inline override def remove(elem : SkillType) = data.remove(elem.annotation)
  @inline override def size = data.size
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
}

object AnnotationHashSet {
  @inline private[internal] def apply(resolve : (Long) ⇒ SkillType) = new AnnotationHashSet(new HashSet[Long](), resolve)
}

/**
 * View for a map of any types. Only used for map fields in proxies
 * 
 * @author Jonathan Roth
 */
sealed abstract class MapView[A, B]
  // copied from scala.collection.mutable.HashMap
  extends AbstractMap[A, B] with Map[A, B] with MapLike[A, B, MapView[A, B]] with HashTable[A, DefaultEntry[A, B]]
  with CustomParallelizable[(A, B), ParHashMap[A, B]] with Serializable {
  /**
   * the type of the internal HashMap. Used by nested MapViews
   */
  type M <: HashMap[_, _]
  private[internal] def getData : M
  private[internal] def wrap(map : M) : MapView[A, B]
  // dummy implementation; MapLike[A, B, MapView[A, B]].empty and Map[A, B].empty have incompatible types
  @inline override def empty : MapView[A, B] = ???
}

/**
 * Simple wrapper around a HashMap that forwards all operations. Used for primitive types
 * 
 * @author Jonathan Roth
 */
final class BasicMapView[A, B] private[internal] (private val data : HashMap[A, B]) extends MapView[A, B] {
  type M = HashMap[A, B]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new BasicMapView[A, B](map)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (A, B)) = { data += kv; this }
  @inline override def -=(key : A) = { data -= key; this }
  @inline override def apply(key : A) = data(key)
  @inline override def clear = data.clear
  @inline override def contains(key : A) = data.contains(key)
  @inline protected override def createNewEntry[B1](key : A, value : B1) = new DefaultEntry(key, value.asInstanceOf[B])
  @inline override def empty = new BasicMapView[A, B](data.empty)
  @inline override def foreach[C](f : ((A, B)) ⇒ C) = data.foreach(f)
  @inline override def get(key : A) = data.get(key)
  @inline override def iterator = data.iterator
  @inline override def keySet = data.keySet
  @inline override def keysIterator = data.keysIterator
  @inline override def par = data.par
  @inline override def put(key : A, value : B) = data.put(key, value)
  @inline override def remove(key : A) = data.remove(key)
  @inline override def size = data.size
  @inline override def update(key : A, value : B) = data(key) = value
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values
  @inline override def valuesIterator = data.valuesIterator
}

/**
 * Wrapper around a HashMap with a reference type as key type and a primitive value type
 * 
 * @author Jonathan Roth
 */
final class RefBasicMapView[A <: SkillType, B] private[internal] (private val data : HashMap[Int, B], private val resolve : (Int) ⇒ A)
  extends MapView[A, B] {
  type M = HashMap[Int, B]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new RefBasicMapView[A, B](map, resolve)
  @inline private implicit def resolvePair(kv : (Int, B)) = (resolve(kv._1), kv._2)
  @inline private implicit def unresolvePair(kv : (A, B)) = (kv._1.reference, kv._2)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (A, B)) = { data += kv; this }
  @inline override def -=(key : A) = { data -= key.reference; this }
  @inline override def apply(key : A) = data(key.reference)
  @inline override def clear = data.clear
  @inline override def contains(key : A) = data.contains(key.reference)
  @inline protected override def createNewEntry[B1](key : A, value : B1) = new DefaultEntry(key, value.asInstanceOf[B])
  @inline override def empty = new RefBasicMapView[A, B](data.empty, resolve)
  @inline override def foreach[C](f : ((A, B)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : A) = data.get(key.reference)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet.map(resolve)
  @inline override def keysIterator = data.keysIterator.map(resolve)
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : A, value : B) = data.put(key.reference, value)
  @inline override def remove(key : A) = data.remove(key.reference)
  @inline override def size = data.size
  @inline override def update(key : A, value : B) = data(key.reference) = value
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values
  @inline override def valuesIterator = data.valuesIterator
}

/**
 * Wrapper around a HashMap with an annotation as key type and a primitive value type
 * 
 * @author Jonathan Roth
 */
final class AnnotationBasicMapView[B] private[internal] (private val data : HashMap[Long, B], private val resolve : (Long) ⇒ SkillType)
  extends MapView[SkillType, B] {
  type M = HashMap[Long, B]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new AnnotationBasicMapView[B](map, resolve)
  @inline private implicit def resolvePair(kv : (Long, B)) = (resolve(kv._1), kv._2)
  @inline private implicit def unresolvePair(kv : (SkillType, B)) = (kv._1.annotation, kv._2)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (SkillType, B)) = { data += kv; this }
  @inline override def -=(key : SkillType) = { data -= key.annotation; this }
  @inline override def apply(key : SkillType) = data(key.annotation)
  @inline override def clear = data.clear
  @inline override def contains(key : SkillType) = data.contains(key.annotation)
  @inline protected override def createNewEntry[B1](key : SkillType, value : B1) = new DefaultEntry(key, value.asInstanceOf[B])
  @inline override def empty = new AnnotationBasicMapView[B](data.empty, resolve)
  @inline override def foreach[C](f : ((SkillType, B)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : SkillType) = data.get(key.annotation)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet.map(resolve)
  @inline override def keysIterator = data.keysIterator.map(resolve)
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : SkillType, value : B) = data.put(key.annotation, value)
  @inline override def remove(key : SkillType) = data.remove(key.annotation)
  @inline override def size = data.size
  @inline override def update(key : SkillType, value : B) = data(key.annotation) = value
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values
  @inline override def valuesIterator = data.valuesIterator
}

/**
 * Wrapper around a HashMap with a primitive key type and a reference type as value type
 * 
 * @author Jonathan Roth
 */
final class BasicRefMapView[A, B <: SkillType] private[internal] (private val data : HashMap[A, Int], private val resolve : (Int) ⇒ B)
  extends MapView[A, B] {
  type M = HashMap[A, Int]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new BasicRefMapView[A, B](map, resolve)
  @inline private implicit def resolvePair(kv : (A, Int)) = (kv._1, resolve(kv._2))
  @inline private implicit def unresolvePair(kv : (A, B)) = (kv._1, kv._2.reference)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (A, B)) = { data += kv; this }
  @inline override def -=(key : A) = { data -= key; this }
  @inline override def apply(key : A) = resolve(data(key))
  @inline override def clear = data.clear
  @inline override def contains(key : A) = data.contains(key)
  @inline protected override def createNewEntry[B1](key : A, value : B1) = new DefaultEntry(key, value.asInstanceOf[B])
  @inline override def empty = new BasicRefMapView[A, B](data.empty, resolve)
  @inline override def foreach[C](f : ((A, B)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : A) = data.get(key).map(resolve)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet
  @inline override def keysIterator = data.keysIterator
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : A, value : B) = data.put(key, value.reference).map(resolve)
  @inline override def remove(key : A) = data.remove(key).map(resolve)
  @inline override def size = data.size
  @inline override def update(key : A, value : B) = data(key) = value.reference
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values.map(resolve)
  @inline override def valuesIterator = data.valuesIterator.map(resolve)
}

/**
 * Wrapper around a HashMap with a reference type as key and value type
 * 
 * @author Jonathan Roth
 */
final class RefMapView[A <: SkillType, B <: SkillType] private[internal]
  (private val data : HashMap[Int, Int], val resolveKey : (Int) ⇒ A, val resolveValue : (Int) ⇒ B)
  extends MapView[A, B] {
  type M = HashMap[Int, Int]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new RefMapView[A, B](map, resolveKey, resolveValue)
  @inline private implicit def resolvePair(kv : (Int, Int)) = (resolveKey(kv._1), resolveValue(kv._2))
  @inline private implicit def unresolvePair(kv : (A, B)) = (kv._1.reference, kv._2.reference)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (A, B)) = { data += kv; this }
  @inline override def -=(key : A) = { data -= key.reference; this }
  @inline override def apply(key : A) = resolveValue(data(key.reference))
  @inline override def clear = data.clear
  @inline override def contains(key : A) = data.contains(key.reference)
  @inline protected override def createNewEntry[B1](key : A, value : B1) = new DefaultEntry(key, value.asInstanceOf[B])
  @inline override def empty = new RefMapView[A, B](data.empty, resolveKey, resolveValue)
  @inline override def foreach[C](f : ((A, B)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : A) = data.get(key.reference).map(resolveValue)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet.map(resolveKey)
  @inline override def keysIterator = data.keysIterator.map(resolveKey)
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : A, value : B) = data.put(key.reference, value.reference).map(resolveValue)
  @inline override def remove(key : A) = data.remove(key.reference).map(resolveValue)
  @inline override def size = data.size
  @inline override def update(key : A, value : B) = data(key.reference) = value.reference
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values.map(resolveValue)
  @inline override def valuesIterator = data.valuesIterator.map(resolveValue)
}

/**
 * Wrapper around a HashMap with an annotation as key type and a reference type as value type
 * 
 * @author Jonathan Roth
 */
final class AnnotationRefMapView[B <: SkillType] private[internal]
  (private val data : HashMap[Long, Int], private val resolveKey : (Long) ⇒ SkillType, private val resolveValue : (Int) ⇒ B)
  extends MapView[SkillType, B] {
  type M = HashMap[Long, Int]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new AnnotationRefMapView[B](map, resolveKey, resolveValue)
  @inline private implicit def resolvePair(kv : (Long, Int)) = (resolveKey(kv._1), resolveValue(kv._2))
  @inline private implicit def unresolvePair(kv : (SkillType, B)) = (kv._1.annotation, kv._2.reference)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (SkillType, B)) = { data += kv; this }
  @inline override def -=(key : SkillType) = { data -= key.annotation; this }
  @inline override def apply(key : SkillType) = resolveValue(data(key.annotation))
  @inline override def clear = data.clear
  @inline override def contains(key : SkillType) = data.contains(key.annotation)
  @inline protected override def createNewEntry[B1](key : SkillType, value : B1) = new DefaultEntry(key, value.asInstanceOf[B])
  @inline override def empty = new AnnotationRefMapView[B](data.empty, resolveKey, resolveValue)
  @inline override def foreach[C](f : ((SkillType, B)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : SkillType) = data.get(key.annotation).map(resolveValue)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet.map(resolveKey)
  @inline override def keysIterator = data.keysIterator.map(resolveKey)
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : SkillType, value : B) = data.put(key.annotation, value.reference).map(resolveValue)
  @inline override def remove(key : SkillType) = data.remove(key.annotation).map(resolveValue)
  @inline override def size = data.size
  @inline override def update(key : SkillType, value : B) = data(key.annotation) = value.reference
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values.map(resolveValue)
  @inline override def valuesIterator = data.valuesIterator.map(resolveValue)
}

/**
 * Wrapper around a HashMap with a primitive key type and an annotation as value type
 * 
 * @author Jonathan Roth
 */
final class BasicAnnotationMapView[A] private[internal] (private val data : HashMap[A, Long], private val resolve : (Long) ⇒ SkillType)
  extends MapView[A, SkillType] {
  type M = HashMap[A, Long]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new BasicAnnotationMapView[A](map, resolve)
  @inline private implicit def resolvePair(kv : (A, Long)) = (kv._1, resolve(kv._2))
  @inline private implicit def unresolvePair(kv : (A, SkillType)) = (kv._1, kv._2.annotation)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (A, SkillType)) = { data += kv; this }
  @inline override def -=(key : A) = { data -= key; this }
  @inline override def apply(key : A) = resolve(data(key))
  @inline override def clear = data.clear
  @inline override def contains(key : A) = data.contains(key)
  @inline protected override def createNewEntry[B1](key : A, value : B1) = new DefaultEntry(key, value.asInstanceOf[SkillType])
  @inline override def empty = new BasicAnnotationMapView[A](data.empty, resolve)
  @inline override def foreach[C](f : ((A, SkillType)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : A) = data.get(key).map(resolve)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet
  @inline override def keysIterator = data.keysIterator
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : A, value : SkillType) = data.put(key, value.annotation).map(resolve)
  @inline override def remove(key : A) = data.remove(key).map(resolve)
  @inline override def size = data.size
  @inline override def update(key : A, value : SkillType) = data(key) = value.annotation
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values.map(resolve)
  @inline override def valuesIterator = data.valuesIterator.map(resolve)
}

/**
 * Wrapper around a HashMap with a reference type as key type and an annotation as value type
 * 
 * @author Jonathan Roth
 */
final class RefAnnotationMapView[A <: SkillType] private[internal]
  (private val data : HashMap[Int, Long], val resolveKey : (Int) ⇒ A, val resolveValue : (Long) ⇒ SkillType)
  extends MapView[A, SkillType] {
  type M = HashMap[Int, Long]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new RefAnnotationMapView[A](map, resolveKey, resolveValue)
  @inline private implicit def resolvePair(kv : (Int, Long)) = (resolveKey(kv._1), resolveValue(kv._2))
  @inline private implicit def unresolvePair(kv : (A, SkillType)) = (kv._1.reference, kv._2.annotation)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (A, SkillType)) = { data += kv; this }
  @inline override def -=(key : A) = { data -= key.reference; this }
  @inline override def apply(key : A) = resolveValue(data(key.reference))
  @inline override def clear = data.clear
  @inline override def contains(key : A) = data.contains(key.reference)
  @inline protected override def createNewEntry[B1](key : A, value : B1) = new DefaultEntry(key, value.asInstanceOf[SkillType])
  @inline override def empty = new RefAnnotationMapView[A](data.empty, resolveKey, resolveValue)
  @inline override def foreach[C](f : ((A, SkillType)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : A) = data.get(key.reference).map(resolveValue)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet.map(resolveKey)
  @inline override def keysIterator = data.keysIterator.map(resolveKey)
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : A, value : SkillType) = data.put(key.reference, value.annotation).map(resolveValue)
  @inline override def remove(key : A) = data.remove(key.reference).map(resolveValue)
  @inline override def size = data.size
  @inline override def update(key : A, value : SkillType) = data(key.reference) = value.annotation
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values.map(resolveValue)
  @inline override def valuesIterator = data.valuesIterator.map(resolveValue)
}

/**
 * Wrapper around a HashMap with an annotation as key and value type
 * 
 * @author Jonathan Roth
 */
final class AnnotationMapView private[internal] (private val data : HashMap[Long, Long], private val resolve : (Long) ⇒ SkillType)
  extends MapView[SkillType, SkillType] {
  type M = HashMap[Long, Long]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new AnnotationMapView(map, resolve)
  @inline private implicit def resolvePair(kv : (Long, Long)) = (resolve(kv._1), resolve(kv._2))
  @inline private implicit def unresolvePair(kv : (SkillType, SkillType)) = (kv._1.annotation, kv._2.annotation)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (SkillType, SkillType)) = { data += kv; this }
  @inline override def -=(key : SkillType) = { data -= key.annotation; this }
  @inline override def apply(key : SkillType) = resolve(data(key.annotation))
  @inline override def clear = data.clear
  @inline override def contains(key : SkillType) = data.contains(key.annotation)
  @inline protected override def createNewEntry[B1](key : SkillType, value : B1) = new DefaultEntry(key, value.asInstanceOf[SkillType])
  @inline override def empty = new AnnotationMapView(data.empty, resolve)
  @inline override def foreach[C](f : ((SkillType, SkillType)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : SkillType) = data.get(key.annotation).map(resolve)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet.map(resolve)
  @inline override def keysIterator = data.keysIterator.map(resolve)
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : SkillType, value : SkillType) = data.put(key.annotation, value.annotation).map(resolve)
  @inline override def remove(key : SkillType) = data.remove(key.annotation).map(resolve)
  @inline override def size = data.size
  @inline override def update(key : SkillType, value : SkillType) = data(key.annotation) = value.annotation
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values.map(resolve)
  @inline override def valuesIterator = data.valuesIterator.map(resolve)
}

/**
 * Wrapper around a HashMap with a primitive key type and a map as value type
 * 
 * @author Jonathan Roth
 */
final class BasicMapMapView[A, B <: MapView[_, _]] private[internal]
  (private val data : HashMap[A, B#M], private val resolve : (B#M) ⇒ B)
  extends MapView[A, B] {
  type M = HashMap[A, B#M]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new BasicMapMapView(map, resolve)
  @inline private implicit def resolvePair(kv : (A, B#M)) = (kv._1, resolve(kv._2))
  @inline private implicit def unresolvePair(kv : (A, B)) : (A, B#M) = (kv._1, kv._2.getData)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (A, B)) = { data += kv; this }
  @inline override def -=(key : A) = { data -= key; this }
  @inline override def apply(key : A) = resolve(data(key))
  @inline override def clear = data.clear
  @inline override def contains(key : A) = data.contains(key)
  @inline protected override def createNewEntry[B1](key : A, value : B1) = new DefaultEntry(key, value.asInstanceOf[B])
  @inline override def empty = new BasicMapMapView[A, B](data.empty, resolve)
  @inline override def foreach[C](f : ((A, B)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : A) = data.get(key).map(resolve)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet
  @inline override def keysIterator = data.keysIterator
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : A, value : B) = data.put(key, value.getData).map(resolve)
  @inline override def remove(key : A) = data.remove(key).map(resolve)
  @inline override def size = data.size
  @inline override def update(key : A, value : B) = data(key) = value.getData
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values.map(resolve)
  @inline override def valuesIterator = data.valuesIterator.map(resolve)
}

/**
 * Wrapper around a HashMap with a reference type as key type and a map as value type
 * 
 * @author Jonathan Roth
 */
final class RefMapMapView[A <: SkillType, B <: MapView[_, _]] private[internal]
  (private val data : HashMap[Int, B#M], private val resolveKey : (Int) ⇒ A, private val resolveValue : (B#M) ⇒ B)
  extends MapView[A, B] {
  type M = HashMap[Int, B#M]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new RefMapMapView(map, resolveKey, resolveValue)
  @inline implicit def resolvePair(kv : (Int, B#M)) = (resolveKey(kv._1), resolveValue(kv._2))
  @inline implicit def unresolvePair(kv : (A, B)) : (Int, B#M) = (kv._1.reference, kv._2.getData)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (A, B)) = { data += kv; this }
  @inline override def -=(key : A) = { data -= key.reference; this }
  @inline override def apply(key : A) = resolveValue(data(key.reference))
  @inline override def clear = data.clear
  @inline override def contains(key : A) = data.contains(key.reference)
  @inline protected override def createNewEntry[B1](key : A, value : B1) = new DefaultEntry(key, value.asInstanceOf[B])
  @inline override def empty = new RefMapMapView[A, B](data.empty, resolveKey, resolveValue)
  @inline override def foreach[C](f : ((A, B)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : A) = data.get(key.reference).map(resolveValue)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet.map(resolveKey)
  @inline override def keysIterator = data.keysIterator.map(resolveKey)
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : A, value : B) = data.put(key.reference, value.getData).map(resolveValue)
  @inline override def remove(key : A) = data.remove(key.reference).map(resolveValue)
  @inline override def size = data.size
  @inline override def update(key : A, value : B) = data(key.reference) = value.getData
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values.map(resolveValue)
  @inline override def valuesIterator = data.valuesIterator.map(resolveValue)
}

/**
 * Wrapper around a HashMap with an annotation as key type and a map as value type
 * 
 * @author Jonathan Roth
 */
final class AnnotationMapMapView[B <: MapView[_, _]] private[internal]
  (private val data : HashMap[Long, B#M], private val resolveKey : (Long) ⇒ SkillType, private val resolveValue : (B#M) ⇒ B)
  extends MapView[SkillType, B] {
  type M = HashMap[Long, B#M]
  @inline private[internal] override def getData = data
  @inline private[internal] override def wrap(map : M) = new AnnotationMapMapView(map, resolveKey, resolveValue)
  @inline implicit def resolvePair(kv : (Long, B#M)) = (resolveKey(kv._1), resolveValue(kv._2))
  @inline implicit def unresolvePair(kv : (SkillType, B)) : (Long, B#M) = (kv._1.annotation, kv._2.getData)
  // define all functions implemented by HashMap
  @inline override def +=(kv : (SkillType, B)) = { data += kv; this }
  @inline override def -=(key : SkillType) = { data -= key.annotation; this }
  @inline override def apply(key : SkillType) = resolveValue(data(key.annotation))
  @inline override def clear = data.clear
  @inline override def contains(key : SkillType) = data.contains(key.annotation)
  @inline protected override def createNewEntry[B1](key : SkillType, value : B1) = new DefaultEntry(key, value.asInstanceOf[B])
  @inline override def empty = new AnnotationMapMapView[B](data.empty, resolveKey, resolveValue)
  @inline override def foreach[C](f : ((SkillType, B)) ⇒ C) = data.foreach(kv ⇒ f(kv))
  @inline override def get(key : SkillType) = data.get(key.annotation).map(resolveValue)
  @inline override def iterator = data.iterator.map(resolvePair)
  @inline override def keySet = data.keySet.map(resolveKey)
  @inline override def keysIterator = data.keysIterator.map(resolveKey)
  @inline override def par = data.par.map(resolvePair)
  @inline override def put(key : SkillType, value : B) = data.put(key.annotation, value.getData).map(resolveValue)
  @inline override def remove(key : SkillType) = data.remove(key.annotation).map(resolveValue)
  @inline override def size = data.size
  @inline override def update(key : SkillType, value : B) = data(key.annotation) = value.getData
  @inline def useSizeMap(t : Boolean) = data.useSizeMap(t)
  @inline override def values = data.values.map(resolveValue)
  @inline override def valuesIterator = data.valuesIterator.map(resolveValue)
}
""")
    //class prefix
    out.close()
  }
}