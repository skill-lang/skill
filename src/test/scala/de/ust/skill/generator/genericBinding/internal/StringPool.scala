/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 19.11.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import _root_.de.ust.skill.generator.genericBinding.api._
import _root_.de.ust.skill.generator.genericBinding.internal.streams.FileOutputStream
import _root_.de.ust.skill.generator.genericBinding.internal.streams.InStream

final class StringPool(in : InStream) extends StringAccess {
  import SerializationFunctions.v64

  /**
   * the set of new strings, i.e. strings which do not have an ID
   */
  private[internal] var newStrings = new HashSet[String];

  /**
   * ID ⇀ (absolute offset, length)
   *
   * will be used if idMap contains a null reference
   *
   * @note there is a fake entry at ID 0
   */
  private[internal] var stringPositions = ArrayBuffer[(Long, Int)]((-1L, -1));

  /**
   * get string by ID
   */
  private[internal] var idMap = ArrayBuffer[String](null)

  override def get(index : Long) : String = {
    if (0L == index)
      return null;

    try {
      idMap(index.toInt) match {
        case null ⇒ {
          if (index > stringPositions.size)
            throw InvalidPoolIndex(index, stringPositions.size, "string")

          val off = stringPositions(index.toInt)
          in.push(off._1)
          var chars = in.bytes(off._2)
          in.pop

          val result = new String(chars, "UTF-8")
          idMap(index.toInt) = result
          result
        }
        case s ⇒ s;
      }
    } catch {
      case e : IndexOutOfBoundsException ⇒ throw InvalidPoolIndex(index, stringPositions.size, "string")
    }
  }
  override def add(string : String) = newStrings += string
  override def all : Iterator[String] = (1 until stringPositions.size).map(get(_)).iterator ++ newStrings.iterator
  override def size = stringPositions.size + newStrings.size

  /**
   * prepares serialization of the string pool by adding new strings to the idMap
   *
   * writes the string block and provides implementation of the ws.string(Long ⇀ String) function
   */
  private[internal] def prepareAndWrite(out : FileOutputStream, ws : StateWriter) {
    val serializationIDs = ws.stringIDs

    // ensure all strings are present
    for (k ← 1 until stringPositions.size) {
      get(k)
    }

    // create inverse map
    for (i ← 1 until idMap.size) {
      serializationIDs.put(idMap(i), i)
    }

    // instert new strings to the map;
    //  this is the place where duplications with lazy strings will be detected and eliminated
    for (s ← newStrings) {
      if (!serializationIDs.contains(s)) {
        serializationIDs.put(s, idMap.size)
        idMap += s
      }
    }

    //count
    //@note idMap access performance hack
    v64(idMap.size - 1, out)

    //@note idMap access performance hack
    if (1 != idMap.size) {
      // offsets
      val end = ByteBuffer.allocate(4 * (idMap.size - 1))
      var off = 0
      for (i ← 1 until idMap.size) {
        off += idMap(i).getBytes.length
        end.putInt(off)
      }
      out.put(end.array)

      // data
      for (i ← 1 until idMap.size)
        out.put(idMap(i).getBytes)
    }
  }

  /**
   * prepares serialization of the string pool and appends new Strings to the output stream.
   */
  private[internal] def prepareAndAppend(out : FileOutputStream, as : StateAppender) {
    val serializationIDs = as.stringIDs

    // ensure all strings are present
    for (k ← 1 until stringPositions.size)
      get(k)

    // create inverse map
    for (i ← 1 until idMap.size) {
      serializationIDs.put(idMap(i), i)
    }

    var todo = ArrayBuffer[Array[Byte]]()

    // instert new strings to the map;
    //  this is the place where duplications with lazy strings will be detected and eliminated
    //  this is also the place, where new instances are appended to the output file
    for (s ← newStrings)
      if (!serializationIDs.contains(s)) {
        serializationIDs.put(s, idMap.size)
        idMap += s
        todo += s.getBytes
      }

    // count
    val count = todo.size
    out.v64(count)

    var off = 0
    // end
    val end = ByteBuffer.allocate(4 * count)
    for (s ← todo) {
      off += s.length
      end.putInt(off)
    }
    out.put(end.array)

    // data
    for (s ← todo)
      out.put(s)
  }
}
