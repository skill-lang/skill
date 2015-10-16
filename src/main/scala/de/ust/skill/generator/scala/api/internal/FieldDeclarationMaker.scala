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

trait FieldDeclarationMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/internal/FieldDeclaration.scala")
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
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.internal.AutoField
import de.ust.skill.common.scala.internal.BulkChunk
import de.ust.skill.common.scala.internal.Chunk
import de.ust.skill.common.scala.internal.IgnoredField
import de.ust.skill.common.scala.internal.KnownField
import de.ust.skill.common.scala.internal.SimpleChunk
import de.ust.skill.common.scala.internal.fieldTypes._
import de.ust.skill.common.scala.internal.restrictions._
""");

    for (t ← IR; f ← t.getFields; if !f.isInstanceOf[View])
      out.write(s"""
/**
 * ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
 */
final class ${knownField(f)}(${
        if (f.isAuto()) ""
        else """
  _index : Int,"""
      }
  _owner : ${storagePool(t)},
  _type : FieldType[${mapType(f.getType)}]${
        if (f.getType.isInstanceOf[ReferenceType] || f.getType.isInstanceOf[ContainerType]) ""
        else s" = ${mapToFieldType(f.getType)}"
      })
    extends ${
        if (f.isAuto()) "Auto"
        else "Known"
      }Field[${mapType(f.getType)},${mapType(t)}](_type,
      "${f.getSkillName}",${
        if (f.isAuto()) """
      0,"""
        else """
      _index,"""
      }
      _owner)${
        // mark ignored fields as ignored
        if (f.isIgnored()) s"""
    with IgnoredField[${mapType(f.getType)},${mapType(t)}]"""
        else ""
      }${
        // has no serialization
        if (f.isAuto()) " {"
        // generate a read function
        else s""" {

  override def read(part : MappedInStream, target : Chunk) {${
          if (f.isConstant()) """
    // reading constants is O(0)"""
          else s"""
    val is = target match {
      case c : SimpleChunk ⇒ owner.data.view(c.bpo.toInt, (c.bpo + c.count).toInt).iterator
      case bci : BulkChunk ⇒ owner.all
    }
    val in = part.view(target.begin.toInt, target.end.toInt)
    for (i ← is)
      i.${escaped(f.getName.camel)} = t.read(in)"""
        }
  }

  def offset: Unit = {
    val range = owner.blocks.last
    val data = owner.data
    var result = 0L

    var i = range.bpo
    val high = i + range.dynamicCount

    while (i != high) {
      val v = data(i).${name(f)}
      ${offsetCode(f.getType)}
      i += 1
    }
    cachedOffset = result
  }

  def write(out: MappedOutStream): Unit = {
    val range = owner.blocks.last
    val data = owner.data

    var i = range.bpo
    val high = i + range.dynamicCount

    while (i != high) {
      val v = data(i).${name(f)}
      ${writeCode(f.getType)}
      i += 1
    }
  }

"""
      }${
        if (f.getRestrictions.isEmpty()) ""
        else s"""  restrictions ++= HashSet(${mkFieldRestrictions(f)})
"""
      }

  //override def get(i : ${mapType(t)}) = i.${escaped(f.getName.camel)}
  //override def set(i : ${mapType(t)}, v : ${mapType(f.getType)}) = ${
        if (f.isConstant()) s"""throw new IllegalAccessError("${f.getName.camel} is a constant!")"""
        else s"i.${escaped(f.getName.camel)} = v"
      }

  override def getR(i : SkillObject) = i.asInstanceOf[${mapType(t)}].${escaped(f.getName.camel)}
  override def setR(i : SkillObject, v : ${mapType(f.getType)}) : Unit = ${
        if (f.isConstant()) s"""throw new IllegalAccessError("${f.getName.camel} is a constant!")"""
        else s"i.asInstanceOf[${mapType(t)}].${escaped(f.getName.camel)} = v"
      }
}
""")

    //class prefix
    out.close()
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

  private def mkFieldRestrictions(f : Field) : String = {
    f.getRestrictions.map(_ match {
      case r : NonNullRestriction ⇒ s"NonNull[${mapType(f.getType)}]"
      case r : IntRangeRestriction ⇒
        s"Range(${
          r.getLow
        }L.to${mapType(f.getType)}, ${r.getHigh}L.to${mapType(f.getType)})"

      case r : FloatRangeRestriction ⇒ f.getType.getSkillName match {
        case "f32" ⇒ s"Range(${r.getLowFloat}f, ${r.getHighFloat}f)"
        case "f64" ⇒ s"Range(${r.getLowDouble}, ${r.getHighDouble})"
        case t     ⇒ throw new IllegalStateException(s"parser should have rejected a float restriction on field $f")
      }
      case r : ConstantLengthPointerRestriction ⇒
        s"ConstantLengthPointer"
    }).mkString(", ")
  }

  private final def offsetCode(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
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

      // TODO optimize calls to string and annotation types (requires prelude, check nesting!)
      // constant offsets are not important
      case _ ⇒ "result += t.offset(v)"
    }

    case t : UserType ⇒ "???"
    case _            ⇒ "???"
  }

  private final def writeCode(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      // TODO optimize calls to string and annotation types (requires prelude, check nesting!)
      case "annotation" | "string" ⇒ "t.write(v, out)"

      case t                       ⇒ s"out.$t(v)"
    }

    // TODO optimize user types (requires prelude, check nesting!)
    case t : UserType ⇒ "???"
    case _            ⇒ "???"
  }
}
