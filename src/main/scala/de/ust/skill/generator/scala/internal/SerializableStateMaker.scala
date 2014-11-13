/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import scala.collection.JavaConversions._
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.Type
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.restriction.MonotoneRestriction
import de.ust.skill.ir.restriction.SingletonRestriction

trait SerializableStateMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/SerializableState.scala")

    out.write(s"""package ${packagePrefix}internal

import java.nio.file.Files
import java.nio.file.Path

import scala.collection.mutable.ArrayBuffer

import ${packagePrefix}api._
import ${packagePrefix}internal.streams.FileOutputStream

/**
 * This class is used to handle objects in a serializable state.
 *
 * @author Timm Felden
 */
final class SerializableState(
${
      (for (t ← IR) yield s"  val ${t.getName.capital} : ${t.getName.capital}Access,").mkString("\n")
    }
  val String : StringAccess,
  val pools : Array[StoragePool[_ <: SkillType, _ <: SkillType]],
  var fromPath : Option[Path])
    extends SkillState {

  val poolByName = pools.map(_.name).zip(pools).toSeq.toMap

  finalizePools;

  def all = pools.iterator.asInstanceOf[Iterator[Access[_ <: SkillType]]]

  def write(target : Path) : Unit = {
    new StateWriter(this, FileOutputStream.write(target))
    if (fromPath.isEmpty)
      fromPath = Some(target)
  }
  // @note: this is more tricky then append, because the state has to be prepared before the file is deleted
  def write() : Unit = ???

  def append() : Unit = new StateAppender(this, FileOutputStream.append(fromPath.getOrElse(throw new IllegalStateException("The state was not created using a read operation, thus append is not possible!"))))
  def append(target : Path) : Unit = {
    if (fromPath.isEmpty) {
      // append and write is the same operation, if we did not read a file
      write(target)
    } else if (target.equals(fromPath.get)) {
      append
    } else {
      // copy the read file to the target location
      Files.deleteIfExists(target)
      Files.copy(fromPath.get, target)
      // append to the target file
      new StateAppender(this, FileOutputStream.append(target))
    }
  }

  def checkRestrictions() : Boolean = {
    ???
  }

  @inline private def finalizePools {
    @inline def eliminatePreliminaryTypesIn[T](t : FieldType[T]) : FieldType[T] = t match {
      case TypeDefinitionIndex(i) ⇒ try {
        pools(i.toInt).asInstanceOf[FieldType[T]]
      } catch {
        case e : Exception ⇒ throw new IllegalStateException(s"inexistent user type $$i (user types: $${poolByName.mkString})", e)
      }
      case TypeDefinitionName(n) ⇒ try {
        poolByName(n).asInstanceOf[FieldType[T]]
      } catch {
        case e : Exception ⇒ throw new IllegalStateException(s"inexistent user type $$n (user types: $${poolByName.mkString})", e)
      }
      case ConstantLengthArray(l, t) ⇒ ConstantLengthArray(l, eliminatePreliminaryTypesIn(t))
      case VariableLengthArray(t)    ⇒ VariableLengthArray(eliminatePreliminaryTypesIn(t))
      case ListType(t)               ⇒ ListType(eliminatePreliminaryTypesIn(t))
      case SetType(t)                ⇒ SetType(eliminatePreliminaryTypesIn(t))
      case MapType(k, v)             ⇒ MapType(eliminatePreliminaryTypesIn(k), eliminatePreliminaryTypesIn(v))
      case t                         ⇒ t
    }
    for (p ← pools) {
      val fieldMap = p.fields.map { _.name }.zip(p.fields).toMap

      for ((n, t) ← p.knownFields if !fieldMap.contains(n)) {
        p.addField(p.fields.size, eliminatePreliminaryTypesIn(t), n)
      }
    }
  }
}

object SerializableState {
  /**
   * Creates a new and empty serializable state.
   */
  def create() : SerializableState = {
    // initialization order of type information has to match file parser and can not be done in place
    val strings = new StringPool(null)
    val types = ArrayBuffer[StoragePool[_ <: SkillType, _ <: SkillType]](${IR.size});
    val annotation = Annotation(types)
    val stringType = StringType(String)

    // create type information
${
      var i = -1
      (for (t ← IR)
        yield s"""    val ${t.getName.capital} = new ${t.getName.capital}StoragePool(stringType, annotation, ${i += 1; i}${
        if (null == t.getSuperType) ""
        else { ", "+t.getSuperType.getName.capital }
      })
    types += ${t.getName.capital}"""
      ).mkString("\n")
    }
    new SerializableState(
${
      (for (t ← IR) yield s"""      ${t.getName.capital},""").mkString("\n")
    }
      strings,
      Array[StoragePool[_ <: SkillType, _ <: SkillType]](${IR.map(_.getName.capital).mkString(", ")}),
      None
    )
  }
}
""")

    out.close()
  }

  private def fieldType(t : Type) : String = t match {
    case t : Declaration             ⇒ s"""userTypes("${t.getSkillName}")"""

    case t : GroundType              ⇒ t.getSkillName.capitalize+"Info"

    case t : ConstantLengthArrayType ⇒ s"new ConstantLengthArrayInfo(${t.getLength}, ${fieldType(t.getBaseType)})"
    case t : VariableLengthArrayType ⇒ s"new VariableLengthArrayInfo(${fieldType(t.getBaseType)})"
    case t : ListType                ⇒ s"new ListInfo(${fieldType(t.getBaseType)})"
    case t : SetType                 ⇒ s"new SetInfo(${fieldType(t.getBaseType)})"
    case t : MapType                 ⇒ s"new MapInfo(${t.getBaseTypes.map(fieldType(_)).mkString("List(", ",", ")")})"
  }
}
