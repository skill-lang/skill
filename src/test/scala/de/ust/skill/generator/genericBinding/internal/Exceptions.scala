/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 27.01.2015                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import scala.language.existentials

import de.ust.skill.common.jvm.streams.InStream

/**
 * The top class for exceptions thrown by the SKilL API.
 *
 * @author Timm Felden
 */
class SkillException(msg : String, cause : Throwable) extends Exception(msg, cause) {
  def this(cause : Throwable) = this(cause.getMessage(), cause)
  def this(msg : String) = this(msg, null)
}

/**
 * Exception implementing this trait are expectable, when reading or writing skill files. They correspond to §Appendix.B
 *
 * @author Timm Felden
 */
trait ExpectableSkillException {}

/**
 * Thrown on invalid generic access.
 *
 * @author Timm Felden
 */
case class GenericAccessException(v : SkillType, t : String, f : String, cause : Throwable) extends SkillException(s"$v has no value in $t.$f", cause) with ExpectableSkillException;

/**
 * This exception is used if byte stream related errors occur.
 *
 * @author Timm Felden
 */
case class ParseException(in : InStream, block : Int, msg : String, cause : Throwable) extends SkillException(
  s"In block ${block + 1} @0x${in.position.toHexString}: $msg",
  cause
);

/**
 * This exception is used if byte stream related errors occur.
 *
 * @author Timm Felden
 */
class StreamException(msg : String, cause : Throwable) extends SkillException(msg, cause) {}

/**
 * Thrown, if an index into a pool is invalid.
 *
 * @author Timm Felden
 */
case class InvalidPoolIndex(index : Long, size : Long, pool : String)
  extends SkillException(s"invalid index $index into pool $pool(size:$size)")
  with ExpectableSkillException {}

/**
 * Thrown, if field deserialization consumes less bytes then specified by the header.
 *
 * @author Timm Felden
 */
case class PoolSizeMissmatchError(block : Int, begin : Long, end : Long, field : FieldDeclaration[_])
  extends SkillException(s"Corrupted data chunk in block ${block + 1} between 0x${begin.toHexString} and 0x${end.toHexString} in Field ${field.owner.name}.${field.name} of type ${field.t.toString}")
  with ExpectableSkillException {}

/**
 * Thrown in case of a type miss-match on a field type.
 *
 * @author Timm Felden
 */
case class TypeMissmatchError(t : FieldType[_], expected : String, fieldName : String, poolName : String)
  extends SkillException(s"""During construction of $poolName.$fieldName: Encountered incompatible type "$t" (expected: $expected)""")
  with ExpectableSkillException {}
