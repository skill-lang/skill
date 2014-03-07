/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait StringPoolMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/StringPool.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import ${packagePrefix}api._
import ${packagePrefix}internal.streams.InStream
import ${packagePrefix}internal.streams.OutStream
import ${packagePrefix}internal.streams.OutBuffer
""")

    out.write("""
final class StringPool(in: InStream) extends StringAccess {
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

  override def get(index: Long): String = {
    if (0L == index)
      return null

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
  }
  override def add(string: String) = newStrings += string
  override def all: Iterator[String] = (1 until stringPositions.size).map(get(_)).iterator ++ newStrings.iterator
  override def size = stringPositions.size + newStrings.size

  /**
   * prepares serialization of the string pool by adding new strings to the idMap
   *
   * writes the string block and provides implementation of the ws.string(Long ⇀ String) function
   */
  private[internal] def prepareAndWrite(out: OutStream, ws: StateWriter) {
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

    //@note OutBuffer performance hack
    //@note idMap access performance hack
    if (1 != idMap.size) {
      //end & data
      val end = ByteBuffer.allocate(4 * (idMap.size - 1))

      // unroll first loop step, to eliminate null checks in out buffer :)
      var off = idMap(1).length
      end.putInt(off)
      val data = new OutBuffer(idMap(1).getBytes)

      for (i ← 2 until idMap.size) {
        val s = idMap(i).getBytes
        off += s.length
        end.putInt(off)
        data.put(s)
      }

      //write back
      out.put(end.array)
      out.putAll(data)
    }
  }

  //  /**
  //   * prepares serialization of the string pool and appends new Strings to the output stream.
  //   */
  //  private[internal] def prepareAndAppend(out: FileChannel, as: AppendState) {
  //    val serializationIDs = as.serializationIDs
  //
  //    // ensure all strings are present
  //    for (k ← stringPositions.keySet)
  //      get(k)
  //
  //    // create inverse map
  //    idMap.foreach({ case (k, v) ⇒ serializationIDs.put(v, k) })
  //
  //    val data = new ByteArrayOutputStream
  //    var offsets = ArrayBuffer[Int]()
  //
  //    // instert new strings to the map;
  //    //  this is the place where duplications with lazy strings will be detected and eliminated
  //    //  this is also the place, where new instances are appended to the output file
  //    for (s ← newStrings)
  //      if (!serializationIDs.contains(s)) {
  //        idMap.put(idMap.size + 1, s)
  //        serializationIDs.put(s, idMap.size)
  //        data.write(s.getBytes)
  //        offsets += data.size
  //      }
  //
  //    //count
  //    val count = offsets.size
  //    out.write(ByteBuffer.wrap(v64(count)))
  //
  //    //end & data
  //    val end = ByteBuffer.allocate(4 * count)
  //    offsets.foreach(end.putInt(_))
  //
  //    //write back
  //    end.rewind()
  //    out.write(end)
  //    out.write(ByteBuffer.wrap(data.toByteArray()))
  //  }
}
""")

    //class prefix
    out.close()
  }
}
