/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 27.01.2015                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

package restrictions {
  /**
   * An abstract type restriction that can be asked to check a whole type.
   *
   * @author Timm Felden
   */
  trait TypeRestriction;

  /**
   * An abstract field restriction that can be asked to check a field.
   *
   * @author Timm Felden
   */
  trait FieldRestriction[@specialized(Boolean, Byte, Char, Double, Float, Int, Long, Short) T] {
    def check(value : T) : Unit;
  }

  /**
   * A nonnull restricition. It will ensure that field data is non null.
   */
  object NonNull extends FieldRestriction[SkillType] {

    override def check(value : SkillType) {
      if (value == null)
        throw new SkillException("Null value violates @NonNull.")
    }
  }

  /**
   * manual specialization because Scala wont help us
   */
  object Range {
    case class RangeI8(min : Byte, max : Byte) extends FieldRestriction[Byte] {
      override def check(value : Byte) {
        if (value < min || max < value) throw new SkillException(s"$value is not in Range($min, $max)")
      }
    }
    def apply(min : Byte, max : Byte) = new RangeI8(min, max)

    case class RangeI16(min : Short, max : Short) extends FieldRestriction[Short] {
      override def check(value : Short) {
        if (value < min || max < value) throw new SkillException(s"$value is not in Range($min, $max)")
      }
    }
    def apply(min : Short, max : Short) = new RangeI16(min, max)

    case class RangeI32(min : Int, max : Int) extends FieldRestriction[Int] {
      override def check(value : Int) {
        if (value < min || max < value) throw new SkillException(s"$value is not in Range($min, $max)")
      }
    }
    def apply(min : Int, max : Int) = new RangeI32(min, max)

    case class RangeI64(min : Long, max : Long) extends FieldRestriction[Long] {
      override def check(value : Long) {
        if (value < min || max < value) throw new SkillException(s"$value is not in Range($min, $max)")
      }
    }
    def apply(min : Long, max : Long) = new RangeI64(min, max)

    case class RangeF32(min : Float, max : Float) extends FieldRestriction[Float] {
      override def check(value : Float) {
        if (value < min || max < value) throw new SkillException(s"$value is not in Range($min, $max)")
      }
    }
    def apply(min : Float, max : Float) = new RangeF32(min, max)

    case class RangeF64(min : Double, max : Double) extends FieldRestriction[Double] {
      override def check(value : Double) {
        if (value < min || max < value) throw new SkillException(s"$value is not in Range($min, $max)")
      }
    }
    def apply(min : Double, max : Double) = new RangeF64(min, max)
  }
}
