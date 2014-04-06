/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait FieldDeclarationMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/FieldDeclaration.scala")
    //package
    out.write(s"""package ${packagePrefix}internal

import scala.collection.mutable.ListBuffer

/**
 * Chunks contain information on where some field data can be found.
 *
 * @param begin position of the first byte of the first instance's data
 * @param end position of the last byte, i.e. the first byte that is not read
 * @param bpsi the index of the first instance
 * @param count the number of instances in this chunk
 *
 * @note indices of recipient of the field data is not necessarily continuous; make use of staticInstances!
 * @note begin and end are vars, because they will contain relative offsets while parsing a type block
 *
 * @author Timm Felden
 */
sealed abstract class ChunkInfo(var begin : Long, var end : Long, val count : Long);
final class SimpleChunkInfo(begin : Long, end : Long, val bpsi : Long, count : Long) extends ChunkInfo(begin, end, count);
final class BulkChunkInfo(begin : Long, end : Long, count : Long) extends ChunkInfo(begin, end, count);

/**
 * Blocks contain information about the type of an index range.
 *
 * @param bpsi the index of the first instance
 * @param count the number of instances in this chunk
 * @author Timm Felden
 */
case class BlockInfo(val bpsi : Long, val count : Long);

/**
 * A field decalariation, as it occurs during parsing of a type blocks header.
 *
 * @author Timm Felden
 * @param t the actual type of the field; can be an intermediate type, while parsing a block
 * @param name the name of the field
 * @param index the index of this field, starting from 0; required for append operations
 */
class FieldDeclaration(var t : FieldType, val name : String, val index : Long) {

  /**
   *  Data chunk information, as it is required for later parsing.
   */
  val dataChunks = ListBuffer[ChunkInfo]();

  override def toString = t.toString+" "+name
  override def equals(obj : Any) = obj match {
    case f : FieldDeclaration ⇒ name == f.name && t == f.t
    case _                    ⇒ false
  }
  override def hashCode = name.hashCode ^ t.hashCode
}
""")

    //class prefix
    out.close()
  }
}
