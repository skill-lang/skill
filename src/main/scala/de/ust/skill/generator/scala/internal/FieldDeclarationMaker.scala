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

/**
 * @param position the position of the chunk inside the file
 * @param length the length of the chunk in bytes
 * @param count the number of instances stored in the chunk
 * @author Timm Felden
 */
case class ChunkInfo(position: Long, length: Long, count: Long) {}

/**
 * A field decalariation, as it occurs during parsing of a type blocks header.
 *
 * @author Timm Felden
 * @param t the actual type of the field; can be an intermediate type, while parsing a block
 * @param name the name of the field
 * @param end the end offset inside of the last block encountered; this field has no meaning after a block has been
 *            processed; the information is required, because the absolute offset can not be known until processing the
 *            whole header of a type chunk; can be used during serialization to store end offsets of new data
 */
class FieldDeclaration(
    var t: TypeInfo,
    val name: String,
    var end: Long) {

  /**
   *  Data chunk information, as it is required for later parsing.
   */
  var dataChunks = List[ChunkInfo]();

  override def toString = t.toString+" "+name
  override def equals(obj: Any) = obj match {
    case f: FieldDeclaration ⇒ name == f.name && t == f.t
    case _                   ⇒ false
  }
  override def hashCode = name.hashCode ^ t.hashCode
}
""")

    //class prefix
    out.close()
  }
}
