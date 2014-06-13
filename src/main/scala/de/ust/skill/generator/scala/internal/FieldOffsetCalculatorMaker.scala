/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait FieldOffsetCalculatorMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/FieldOffsetCalculator.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

/**
 * Utility object to calculate field offsets by type.
 *
 * @author Timm Felden
 */
object FieldOffsetCalculator {
  def offset(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[_]) : Long = f.t match {
    case ConstantI8(_) | ConstantI16(_) | ConstantI32(_) | ConstantI64(_) | ConstantV64(_) ⇒ 0
    case BoolType | I8 ⇒ p.blockInfos.last.count
    case I16 ⇒ 2 * p.blockInfos.last.count
    case F32 | I32 ⇒ 4 * p.blockInfos.last.count
    case F64 | I64 ⇒ 8 * p.blockInfos.last.count

    case V64 ⇒ encodeV64(p, f)

    case s : StoragePool[_, _] ⇒
      if (p.blockInfos.last.count < 128) p.blockInfos.last.count // quick solution for small pools
      else {
        encodeRefs(p, f)
      }

    case SetType(t) ⇒
      val b = p.blockInfos.last
      p.basePool.data.view(b.bpsi.toInt, (b.bpsi + b.count).toInt).foldLeft(0L) {
        case (sum, i) ⇒
          val xs = i.get(f).asInstanceOf[Iterable[_]];
          sum + encodeSingleV64(xs.size) + encode(xs, t)
      }

    case TypeDefinitionIndex(_) | TypeDefinitionName(_) ⇒ ??? // should have been eliminated already
  }

  @inline private[this] def encodeSingleV64(v : Long) : Long = {
    if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
      1
    } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
      2
    } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
      3
    } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
      4
    } else if (0L == (v & 0xFFFFFFF800000000L)) {
      5
    } else if (0L == (v & 0xFFFFFC0000000000L)) {
      6
    } else if (0L == (v & 0xFFFE000000000000L)) {
      7
    } else if (0L == (v & 0xFF00000000000000L)) {
      8
    } else {
      9
    }
  }

  @inline private[this] def encodeV64(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[_]) : Long = {
    var result = 0L
    for (i ← p.all) {
      val v = i.get(f).asInstanceOf[Long]
      if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
        result += 1
      } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
        result += 2
      } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
        result += 3
      } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
        result += 4
      } else if (0L == (v & 0xFFFFFFF800000000L)) {
        result += 5
      } else if (0L == (v & 0xFFFFFC0000000000L)) {
        result += 6
      } else if (0L == (v & 0xFFFE000000000000L)) {
        result += 7
      } else if (0L == (v & 0xFF00000000000000L)) {
        result += 8
      } else {
        result += 9
      }
    }
    result
  }

  @inline private[this] def encodeV64(p : Iterable[Long]) : Long = {
    var result = 0L
    for (v ← p) {
      if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
        result += 1
      } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
        result += 2
      } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
        result += 3
      } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
        result += 4
      } else if (0L == (v & 0xFFFFFFF800000000L)) {
        result += 5
      } else if (0L == (v & 0xFFFFFC0000000000L)) {
        result += 6
      } else if (0L == (v & 0xFFFE000000000000L)) {
        result += 7
      } else if (0L == (v & 0xFF00000000000000L)) {
        result += 8
      } else {
        result += 9
      }
    }
    result
  }

  @inline private[this] def encodeRefs(p : StoragePool[_ <: SkillType, _ <: SkillType], f : FieldDeclaration[_]) : Long = {
    var result = 0L
    val b = p.blockInfos.last
    for (i ← p.basePool.data.view(b.bpsi.toInt, (b.bpsi + b.count).toInt)) {
      val v = i.get(f).asInstanceOf[SkillType].getSkillID
      if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
        result += 1
      } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
        result += 2
      } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
        result += 3
      } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
        result += 4
      } else if (0L == (v & 0xFFFFFFF800000000L)) {
        result += 5
      } else if (0L == (v & 0xFFFFFC0000000000L)) {
        result += 6
      } else if (0L == (v & 0xFFFE000000000000L)) {
        result += 7
      } else if (0L == (v & 0xFF00000000000000L)) {
        result += 8
      } else {
        result += 9
      }
    }
    result
  }

  @inline private[this] def encodeRefs(p : Iterable[SkillType]) : Long = {
    var result = 0L
    for (i ← p) {
      val v = i.getSkillID
      if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
        result += 1
      } else if (0L == (v & 0xFFFFFFFFFFFFC000L)) {
        result += 2
      } else if (0L == (v & 0xFFFFFFFFFFE00000L)) {
        result += 3
      } else if (0L == (v & 0xFFFFFFFFF0000000L)) {
        result += 4
      } else if (0L == (v & 0xFFFFFFF800000000L)) {
        result += 5
      } else if (0L == (v & 0xFFFFFC0000000000L)) {
        result += 6
      } else if (0L == (v & 0xFFFE000000000000L)) {
        result += 7
      } else if (0L == (v & 0xFF00000000000000L)) {
        result += 8
      } else {
        result += 9
      }
    }
    result
  }

  private[this] def encode[T](xs : Iterable[T], t : FieldType[_]) : Long = t match {
    case ConstantI8(_) | ConstantI16(_) | ConstantI32(_) | ConstantI64(_) | ConstantV64(_) ⇒ 0
    case BoolType | I8 ⇒ xs.size
    case I16 ⇒ 2 * xs.size
    case F32 | I32 ⇒ 4 * xs.size
    case F64 | I64 ⇒ 8 * xs.size

    case V64 ⇒ encodeV64(xs.asInstanceOf[Iterable[Long]])

    case t : StoragePool[_, _] ⇒
      if (t.blockInfos.last.count < 128) xs.size // quick solution for small pools
      else encodeRefs(xs.asInstanceOf[Iterable[SkillType]])

    case SetType(t)                                     ⇒ 0

    case TypeDefinitionIndex(_) | TypeDefinitionName(_) ⇒ ??? // should have been eliminated already
  }
}
""")

    //class prefix
    out.close()
  }
}
