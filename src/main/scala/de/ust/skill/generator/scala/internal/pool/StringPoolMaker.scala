/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait StringPoolMaker extends GeneralOutputMaker{
  abstract override def make{
    super.make
    val out = open("internal/pool/StringPool.scala")

    out.write(s"""package ${packagePrefix}internal.pool

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import ${packagePrefix}internal.AppendState
import ${packagePrefix}internal.SerializableState
import ${packagePrefix}internal.SerializationFunctions
import ${packagePrefix}internal.WriteState

final class StringPool {
  import SerializationFunctions.v64

  /**
   * the set of new strings, i.e. strings which do not have an ID
   */
  private[internal] var newStrings = new HashSet[String];

  /**
   * ID ⇀ (absolute offset, length)
   *
   * will be used if idMap contains a null reference
   */
  private[internal] var stringPositions = new HashMap[Long, (Long, Int)];

  /**
   * get string by ID
   */
  private[internal] var idMap = new HashMap[Long, String]

  def write(out: OutputStream) {

    newStrings.foreach(s ⇒ {
      val b = s.getBytes("UTF-8")
      out.write(v64(b.length));
      out.write(b)
    });
  }

  def size = stringPositions.size + newStrings.size

  /**
   * prepares serialization of the string pool by adding new strings to the idMap
   *
   * @note TODO this solution is not correct if strings are not used anymore, i.e. it may produce garbage
   */
  private[internal] def prepareAndWrite(out: FileChannel, ws: WriteState) {
    val serializationIDs = ws.serializationIDs

    // ensure all strings are present
    stringPositions.keySet.foreach(ws.state.getString(_))

    // create inverse map
    idMap.foreach({ case (k, v) ⇒ serializationIDs.put(v, k) })

    // instert new strings to the map;
    //  this is the place where duplications with lazy strings will be detected and eliminated
    for (s ← newStrings)
      if (!serializationIDs.contains(s)) {
        idMap.put(idMap.size + 1, s)
        serializationIDs.put(s, idMap.size)
      }

    //count
    out.write(ByteBuffer.wrap(v64(idMap.size)))

    //end & data
    val end = ByteBuffer.allocate(4 * idMap.size)
    val data = new ByteArrayOutputStream

    var off = 0
    for (i ← 1 to idMap.size) {
      val s = idMap(i).getBytes()
      off += s.length
      end.putInt(off)
      data.write(s)
    }

    //write back
    end.rewind()
    out.write(end)
    out.write(ByteBuffer.wrap(data.toByteArray()))
  }

  /**
   * prepares serialization of the string pool and appends new Strings to the output stream.
   */
  private[internal] def prepareAndAppend(out: FileChannel, as: AppendState) {
    val serializationIDs = as.serializationIDs

    // ensure all strings are present
    stringPositions.keySet.foreach(as.state.getString(_))

    // create inverse map
    idMap.foreach({ case (k, v) ⇒ serializationIDs.put(v, k) })

    val data = new ByteArrayOutputStream
    var offsets = List[Int]()

    // instert new strings to the map;
    //  this is the place where duplications with lazy strings will be detected and eliminated
    //  this is also the place, where new instances are appended to the output file
    for (s ← newStrings)
      if (!serializationIDs.contains(s)) {
        idMap.put(idMap.size + 1, s)
        serializationIDs.put(s, idMap.size)
        data.write(s.getBytes)
        offsets ::= data.size
      }

    //count
    val count = offsets.size
    out.write(ByteBuffer.wrap(v64(count)))

    //end & data
    val end = ByteBuffer.allocate(4 * count)
    offsets.foreach(end.putInt(_))

    //write back
    end.rewind()
    out.write(end)
    out.write(ByteBuffer.wrap(data.toByteArray()))
  }
}
""")
    out.close()
  }
}
