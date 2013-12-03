/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait SkillExceptionMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/SkillException.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import ${packagePrefix}internal.parsers.ByteReader""")

    out.write("""

/**
 * The top class for exceptions thrown by the SKilL API.
 *
 * @author Timm Felden
 */
class SkillException(msg: String, cause: Throwable) extends Exception(msg, cause) {
  def this(cause: Throwable) = this(cause.getMessage(), cause)
  def this(msg: String) = this(msg, null)
}

/**
 * Exception implementing this trait are expectable, when reading or writing skill files. They correspond to §Appendix.B
 *
 * @author Timm Felden
 */
trait ExpectableSkillException {}

/**
 * This exception is used if byte stream related errors occur.
 *
 * @author Timm Felden
 */
case class ParseException(in: ByteReader, block: Int, msg: String) extends SkillException(
  s"In block ${block + 1} @${in.position}: $msg"
) {}

/**
 * This exception is used if byte stream related errors occur.
 *
 * @author Timm Felden
 */
class ByteReaderException(msg: String, cause: Throwable) extends SkillException(msg, cause) {}

/**
 * Thrown, if the end of file is reached at an illegal point, i.e. inside a block.
 *
 * @author Timm Felden
 */
case class UnexpectedEOF(msg: String, cause: Throwable)
  extends ByteReaderException(msg, cause)
  with ExpectableSkillException {}

/**
 * Thrown, if an index into a pool is invalid.
 *
 * @author Timm Felden
 */
case class InvalidPoolIndex(index: Long, size: Long, pool: String)
  extends SkillException(s"invalid index $index into pool $pool(size:$size)")
  with ExpectableSkillException {}

/**
 * Thrown, if field deserialization consumes less bytes then specified by the header.
 *
 * @author Timm Felden
 */
case class PoolSizeMissmatchError(expected: Long, actual: Long, t: String)
  extends SkillException(s"expected: $expected, was: $actual, field type: $t")
  with ExpectableSkillException {}

/**
 * Thrown in case of a type miss-match on a field type.
 *
 * @author Timm Felden
 */
case class TypeMissmatchError(t: TypeInfo, expected: String, fieldName: String, poolName: String)
  extends SkillException(s"""+"\"\"\""+"""During construction of $poolName.$fieldName: Encountered incompatible type "$t" (expected: $expected)"""+"\"\"\""+""")
  with ExpectableSkillException {}

/**
 * Thrown in case of a type miss-match on an annotation access.
 *
 * @author Timm Felden
 */
case class AnnotationTypeCastException(expected: String, actual: String)
  extends SkillException(s"Tried to access annotation of type $actual expecting a type $expected.")
  with ExpectableSkillException;
""")

    //class prefix
    out.close()
  }
}
