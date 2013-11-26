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

import ${packagePrefix}internal.SerializableState
import ${packagePrefix}internal.SerializationFunctions

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

  /**
   * only used during serialization!!!
   */
  private[internal] var serializationIDs = new HashMap[String, Long]

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
  private[internal] def prepareSerialization(σ: SerializableState) {
    // ensure all strings are present
    stringPositions.keySet.foreach(σ.getString(_))

    // create inverse map
    idMap.foreach({ case (k, v) ⇒ serializationIDs.put(v, k) })

    // instert new strings to the map;
    //  this is the place where duplications with lazy strings will be detected and eliminated
    for (s ← newStrings)
      if (!serializationIDs.contains(s)) {
        idMap.put(idMap.size + 1, s)
        serializationIDs.put(s, idMap.size)
      }
  }

  /**
   * helper for the implementation of serialization of strings, which returns a ByteBuffer containing an index refering
   * to the argument string
   */
  private[internal] def serializedStringReference(s: String): ByteBuffer = {
    ByteBuffer.wrap(v64(serializationIDs(s)))
  }

  /**
   * writes the contents of the pool to the stream
   */
  private[internal] def write(out: FileChannel, σ: SerializableState) {
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
}
""")
    out.close()
  }
}
