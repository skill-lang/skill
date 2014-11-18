/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait RestrictionsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/Restrictions.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal
""")

    out.write("""
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
    def check(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[T]) : Boolean;
  }

  /**
   * A nonnull restricition. It will ensure that field data is non null.
   */
  object NonNull extends FieldRestriction[SkillType] {

    override def check(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[SkillType]) = {
      p.all.forall { x ⇒ x.get(f) != null }
    }
  }

  /**
   * manual specialization because Scala wont help us
   */
  object Range {
    class RangeI8(min : Byte, max : Byte) extends FieldRestriction[Byte] {
      override def check(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[Byte]) = {
        p.all.forall { i ⇒ val v = i.get(f); min <= v && v <= max }
      }
    }
    def apply(min : Byte, max : Byte) = new RangeI8(min, max)

    class RangeI16(min : Short, max : Short) extends FieldRestriction[Short] {
      override def check(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[Short]) = {
        p.all.forall { i ⇒ val v = i.get(f); min <= v && v <= max }
      }
    }
    def apply(min : Short, max : Short) = new RangeI16(min, max)

    class RangeI32(min : Int, max : Int) extends FieldRestriction[Int] {
      override def check(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[Int]) = {
        p.all.forall { i ⇒ val v = i.get(f); min <= v && v <= max }
      }
    }
    def apply(min : Int, max : Int) = new RangeI32(min, max)

    class RangeI64(min : Long, max : Long) extends FieldRestriction[Long] {
      override def check(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[Long]) = {
        p.all.forall { i ⇒ val v = i.get(f); min <= v && v <= max }
      }
    }
    def apply(min : Long, max : Long) = new RangeI64(min, max)

    class RangeF32(min : Float, max : Float) extends FieldRestriction[Float] {
      override def check(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[Float]) = {
        p.all.forall { i ⇒ val v = i.get(f); min <= v && v <= max }
      }
    }
    def apply(min : Float, max : Float) = new RangeF32(min, max)

    class RangeF64(min : Double, max : Double) extends FieldRestriction[Double] {
      override def check(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[Double]) = {
        p.all.forall { i ⇒ val v = i.get(f); min <= v && v <= max }
      }
    }
    def apply(min : Double, max : Double) = new RangeF64(min, max)
  }
}
""")

    //class prefix
    out.close()
  }
}
