/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api.internal

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker
import scala.collection.JavaConversions._
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.ListType
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Type
import de.ust.skill.ir.MapType
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.FloatRangeRestriction
import de.ust.skill.ir.restriction.NonNullRestriction
import de.ust.skill.ir.View
import de.ust.skill.ir.restriction.ConstantLengthPointerRestriction
import de.ust.skill.ir.ReferenceType
import de.ust.skill.ir.ContainerType
import de.ust.skill.ir.UserType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.Restriction
import de.ust.skill.ir.restriction.DefaultRestriction
import de.ust.skill.ir.restriction.IntDefaultRestriction
import de.ust.skill.ir.restriction.StringDefaultRestriction
import de.ust.skill.ir.restriction.FloatDefaultRestriction
import de.ust.skill.ir.restriction.NameDefaultRestriction
import de.ust.skill.ir.InterfaceType

trait FieldDeclarationMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    // project IR, so that reflection implements the binary file format correctly
    val IR = this.types.removeSpecialDeclarations.getUsertypes

    for (t ← IR; f ← t.getFields) {
      // the type without the interface projection
      val fieldActualType = this.types.removeTypedefs.removeEnums.get(t.getSkillName).asInstanceOf[UserType]
        .getAllFields.find(_.getName == f.getName).map(_.getType).map(mapType).get

      val accessField = s".asInstanceOf[${mapType(t)}].${escaped("Internal_" + f.getName.camel)}"

      val out = open(s"api/internal/${knownField(f)}.scala")
      //package
      out.write(s"""package ${packagePrefix}api.internal

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.WrappedArray

import java.nio.BufferUnderflowException
import java.nio.MappedByteBuffer
import java.util.Arrays

import de.ust.skill.common.jvm.streams.MappedInStream
import de.ust.skill.common.jvm.streams.MappedOutStream
import de.ust.skill.common.scala.api.PoolSizeMissmatchError
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.internal.AutoField
import de.ust.skill.common.scala.internal.BulkChunk
import de.ust.skill.common.scala.internal.Chunk
import de.ust.skill.common.scala.internal.DistributedField
import de.ust.skill.common.scala.internal.IgnoredField
import de.ust.skill.common.scala.internal.FieldDeclaration
import de.ust.skill.common.scala.internal.KnownField
import de.ust.skill.common.scala.internal.LazyField
import de.ust.skill.common.scala.internal.SimpleChunk
import de.ust.skill.common.scala.internal.SingletonStoragePool
import de.ust.skill.common.scala.internal.fieldTypes._
import de.ust.skill.common.scala.internal.restrictions._

/**
 * ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
 */
final class ${knownField(f)}(${
        if (f.isAuto()) ""
        else """
  _index : Int,"""
      }
  _owner : ${storagePool(t)},
  _type : FieldType[$fieldActualType]${
        if (f.getType.isInstanceOf[ReferenceType] || f.getType.isInstanceOf[ContainerType]) ""
        else s" = ${mapToFieldType(f.getType)}"
      })
    extends ${
        if (f.isAuto()) "AutoField"
        else if (f.isOnDemand()) "LazyField"
        else if (f.isDistributed()) "DistributedField"
        else "FieldDeclaration"
      }[$fieldActualType,${mapType(t)}](_type,
      "${f.getSkillName}",${
        if (f.isAuto()) """
      0,"""
        else """
      _index,"""
      }
      _owner)
    with KnownField[$fieldActualType,${mapType(t)}]${
        // mark ignored fields as ignored
        if (f.isIgnored()) s"""
    with IgnoredField[${mapType(f.getType)},${mapType(t)}]"""
        else ""
      } {
${
        if (f.isDistributed()) s"\n  _owner.${knownField(f)} = this"
        else ""
      }
      ${
        // TODO re-enable default restrictions 
        (for (r ← f.getRestrictions if !r.isInstanceOf[DefaultRestriction])
          yield s"""restrictions += ${mkFieldRestriction(f.getType, r)}${
          // add key to strings
          r match {
            case r : StringDefaultRestriction ⇒ s"""
    t.asInstanceOf[de.ust.skill.common.scala.internal.StringPool].add("${r.getValue}")"""
            case _ ⇒ ""
          }
        }""").mkString("""
  override def createKnownRestrictions : Unit = {
    """, """
    """, """
  }""")
      }${
        if (f.isAuto()) ""
        else s"""

  override def read(part : MappedInStream, target : Chunk) {${
          if (f.isConstant()) """
    // reading constants is O(0)"""
          else s"""
    val d = owner.data
    val in = part.view(target.begin.toInt, target.end.toInt)
${mapKnownReadType(f.getType)}
    try {
        target match {
          case c : SimpleChunk ⇒
            var i = c.bpo.toInt
            val high = i + c.count
            while (i != high) {
              d(i)$accessField = t.read(in).asInstanceOf[$fieldActualType]
              i += 1
            }
          case bci : BulkChunk ⇒
            val blocks = owner.blocks
            var blockIndex = 0
            while (blockIndex < bci.blockCount) {
              val b = blocks(blockIndex)
              blockIndex += 1
              var i = b.bpo
              val end = i + b.dynamicCount
              while (i != end) {
                d(i)$accessField = t.read(in).asInstanceOf[$fieldActualType]
                i += 1
              }
            }
        }
    } catch {
      case e : BufferUnderflowException ⇒
        throw new PoolSizeMissmatchError(dataChunks.size - 1,
          part.position() + target.begin,
          part.position() + target.end,
          this, in.position())
    }

    if(!in.eof())
      throw new PoolSizeMissmatchError(dataChunks.size - 1,
        part.position() + target.begin,
        part.position() + target.end,
        this, in.position())"""
        }
  }
${
          if (f.isDistributed) ""
          else s"""
  def offset: Unit = {
    val data = owner.data
    var result = 0L
    dataChunks.last match {
      case c : SimpleChunk ⇒
        var i = c.bpo.toInt
        val high = i + c.count
        while (i != high) {
          val v = data(i)$accessField
          ${offsetCode(f.getType)}
          i += 1
        }
      case bci : BulkChunk ⇒
        val blocks = owner.blocks
        var blockIndex = 0
        while (blockIndex < bci.blockCount) {
          val b = blocks(blockIndex)
          blockIndex += 1
          var i = b.bpo
          val end = i + b.dynamicCount
          while (i != end) {
          val v = data(i)$accessField
          ${offsetCode(f.getType)}
          i += 1
          }
        }
    }
    cachedOffset = result
  }

  def write(out: MappedOutStream): Unit = {
    val data = owner.data
    dataChunks.last match {
      case c : SimpleChunk ⇒
        var i = c.bpo.toInt
        val high = i + c.count
        while (i != high) {
          val v = data(i)$accessField
          ${writeCode(f.getType)}
          i += 1
        }
      case bci : BulkChunk ⇒
        val blocks = owner.blocks
        var blockIndex = 0
        while (blockIndex < bci.blockCount) {
          val b = blocks(blockIndex)
          blockIndex += 1
          var i = b.bpo
          val end = i + b.dynamicCount
          while (i != end) {
            val v = data(i)$accessField
            ${writeCode(f.getType)}
            i += 1
          }
        }
    }
  }"""
        }"""
      }${
        if (f.isDistributed) "" // inherited
        else s"""

  //override def get(i : ${mapType(t)}) = i.${escaped(f.getName.camel)}
  //override def set(i : ${mapType(t)}, v : ${mapType(f.getType)}) = ${
          if (f.isConstant()) s"""throw new IllegalAccessError("${f.getName.camel} is a constant!")"""
          else s"i.${escaped(f.getName.camel)} = v.asInstanceOf[$fieldActualType]"
        }

  // note: reflective field access will raise exception for ignored fields
  override def getR(i : SkillObject) : $fieldActualType = i.asInstanceOf[${mapType(t)}].${escaped(f.getName.camel)}
  override def setR(i : SkillObject, v : $fieldActualType) : Unit = ${
          if (f.isConstant()) s"""throw new IllegalAccessError("${f.getName.camel} is a constant!")"""
          else s"i.asInstanceOf[${mapType(t)}].${escaped(f.getName.camel)} = v.asInstanceOf[$fieldActualType]"
        }"""
      }
}
""")
      out.close()
    }
  }

  private def mapToFieldType(t : Type) : String = {
    //@note it is possible to pass <null> to the case classes, because they will be replaced anyway
    @inline def mapGroundType(t : Type) : String = t.getSkillName match {
      case "annotation" ⇒ "_type"
      case "bool"       ⇒ "BoolType"
      case "i8"         ⇒ "I8"
      case "i16"        ⇒ "I16"
      case "i32"        ⇒ "I32"
      case "i64"        ⇒ "I64"
      case "v64"        ⇒ "V64"
      case "f32"        ⇒ "F32"
      case "f64"        ⇒ "F64"
      case "string"     ⇒ "_type"

      case s            ⇒ "_type"
    }

    t match {
      case t : GroundType ⇒ mapGroundType(t)
      case _              ⇒ "_type"
    }
  }
  private def exactFieldType(t : Type) : String = {
    //@note it is possible to pass <null> to the case classes, because they will be replaced anyway
    @inline def mapGroundType(t : Type) : String = t.getSkillName match {
      case "annotation" ⇒ "AnnotationType"
      case "bool"       ⇒ "BoolType"
      case "i8"         ⇒ "I8"
      case "i16"        ⇒ "I16"
      case "i32"        ⇒ "I32"
      case "i64"        ⇒ "I64"
      case "v64"        ⇒ "V64"
      case "f32"        ⇒ "F32"
      case "f64"        ⇒ "F64"
      case "string"     ⇒ "StringType"
    }

    t match {
      case t : GroundType ⇒ mapGroundType(t)
      case t : UserType   ⇒ storagePool(t)
      case _              ⇒ "_"
    }
  }

  private def mkFieldRestriction(t : Type, r : Restriction) : String = r match {
    case r : NonNullRestriction ⇒ s"NonNull.apply[${mapType(t)}]"
    case r : IntRangeRestriction ⇒
      t.getSkillName match {
        case "i8" ⇒ s"Range(${
          Math.max(Byte.MinValue, r.getLow)
        }.toByte, ${
          Math.min(Byte.MaxValue, r.getHigh)
        }.toByte)"
        case "i16" ⇒ s"Range(${
          Math.max(Short.MinValue, r.getLow)
        }.toShort, ${
          Math.min(Short.MaxValue, r.getHigh)
        }.toShort)"
        case "i32" ⇒ s"Range(${
          Math.max(Int.MinValue, r.getLow)
        }, ${
          Math.min(Int.MaxValue, r.getHigh)
        })"
        case _ ⇒ s"Range(${r.getLow}L, ${r.getHigh}L)"
      }

    case r : FloatRangeRestriction ⇒ t.getSkillName match {
      case "f32" ⇒ s"Range(${r.getLowFloat}f, ${r.getHighFloat}f)"
      case "f64" ⇒ s"Range(${r.getLowDouble}, ${r.getHighDouble})"
      case t     ⇒ throw new IllegalStateException(s"parser should have rejected a float restriction on a field of type $t")
    }
    case r : ConstantLengthPointerRestriction ⇒ "ConstantLengthPointer"

    case r : IntDefaultRestriction ⇒ s"DefaultRestriction(${r.getValue}L.to${mapType(t)})"
    case r : FloatDefaultRestriction ⇒ s"DefaultRestriction(${r.getValue}.to${mapType(t)})"
    case r : NameDefaultRestriction if t.getSkillName.equals("bool") ⇒ s"DefaultRestriction(${r.getValue.head})"
    case r : NameDefaultRestriction ⇒ s"DefaultRestriction(_owner.basePool.owner(${r.getValue.mkString("\"", ":", "\"")}).asInstanceOf[SingletonStoragePool[_ <: de.ust.skill.common.scala.api.SkillObject, _ <: de.ust.skill.common.scala.api.SkillObject]].get)"
    case r : StringDefaultRestriction ⇒ s"""DefaultRestriction("${r.getValue}")"""

    case r ⇒ println("[scala] unhandled restriction: " + r.getName); ""
  }

  /**
   * tell the compiler which code will be executed, to support optimization
   */
  private final def mapKnownReadType(t : Type) : String = t match {

    case t : UserType ⇒ s"    val t = this.t.asInstanceOf[${storagePool(t)}]"
    case _            ⇒ "" // it is always an option not to tell anything
  }

  private final def offsetCode(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      // TODO optimize calls to string and annotation types (requires prelude, check nesting!)
      case "annotation" | "string" ⇒ "result += t.offset(v)"

      case "v64" ⇒ """result += (if (0L == (v & 0xFFFFFFFFFFFFFF80L)) {
              1L
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
            })"""

      // offsets where we know the type
      case _ ⇒ s"result += ${exactFieldType(t : Type)}.offset(v)"
    }

    case t : UserType ⇒ "result += (if (null == v) 1 else V64.offset(v.getSkillID))"
    case t : InterfaceType if t.getSuperType.isInstanceOf[UserType] ⇒ "result += (if (null == v) 1 else V64.offset(v.getSkillID))"

    case t : ConstantLengthArrayType ⇒ s"v.foreach { v => ${offsetCode(t.getBaseType)} }"

    case t : SingleBaseTypeContainer ⇒ s"""result += (if(null == v) 1 else V64.offset(v.size))
      ${
      t.getBaseType.getSkillName match {
        case "string" | "annotation" ⇒ s"val t = this.t.asInstanceOf[SingleBaseTypeContainer[_,${
          mapType(t.getBaseType)
        }]].groundType.asInstanceOf[${
          exactFieldType(t.getBaseType)
        }]"
        case _ ⇒ "" // we will emit concrete code anyway
      }
    }
          if(null != v) v.foreach { v => ${offsetCode(t.getBaseType)} }"""

    // @note this might be optimizable, but i dont care for now
    case t : MapType       ⇒ "result += (if(null == v) 1 else t.offset(v))"
    case t : InterfaceType ⇒ "result += t.offset(v)"

    case _                 ⇒ "???"
  }

  private final def writeCode(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      // TODO optimize calls to string and annotation types (requires prelude, check nesting!)
      case "annotation" | "string" ⇒ "t.write(v, out)"

      case t                       ⇒ s"out.$t(v)"
    }

    // TODO optimize user types (requires prelude, check nesting!)
    case t : UserType ⇒ "if (null == v) out.i8(0) else out.v64(v.getSkillID)"
    case t : InterfaceType if t.getSuperType.isInstanceOf[UserType] ⇒ "if (null == v) out.i8(0) else out.v64(v.getSkillID)"

    case t : ConstantLengthArrayType ⇒ s"v.foreach { v => ${writeCode(t.getBaseType)} }"

    case t : SingleBaseTypeContainer ⇒ s"""if(null == v) out.i8(0) else { out.v64(v.size)
      ${
      t.getBaseType.getSkillName match {
        case "string" | "annotation" ⇒ s"val t = this.t.asInstanceOf[SingleBaseTypeContainer[_,${
          mapType(t.getBaseType)
        }]].groundType.asInstanceOf[${
          exactFieldType(t.getBaseType)
        }]"
        case _ ⇒ "" // we will emit concrete code anyway
      }
    }
            v.foreach { v => ${writeCode(t.getBaseType)} }}"""

    // @note this might be optimizable, but i dont care for now
    case t : MapType       ⇒ "t.write(v, out)"
    case t : InterfaceType ⇒ "t.write(v, out)"

    case _                 ⇒ "???"
  }
}
