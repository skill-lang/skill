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

trait StateMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/State.scala")

    out.write(s"""package ${packagePrefix}internal

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import de.ust.skill.common.jvm.streams.FileInputStream
import de.ust.skill.common.jvm.streams.FileOutputStream

import _root_.${packagePrefix}api._

/**
 * This class is used to handle objects in a serializable state.
 *
 * @author Timm Felden
 */
final class State private[internal] (
${
      (for (t ← IR) yield s"  val ${t.getName.capital} : ${t.getName.capital}Access,").mkString("\n")
    }
  val String : StringAccess,
  val pools : Array[StoragePool[_ <: SkillType, _ <: SkillType]],
  var path : Path,
  var mode : WriteMode)
    extends SkillFile {

  val poolByName = pools.map(_.name).zip(pools).toSeq.toMap

  finalizePools;

  def all = pools.iterator.asInstanceOf[Iterator[Access[_ <: SkillType]]]

  @inline private def finalizePools {
    def eliminatePreliminaryTypesIn[T](t : FieldType[T]) : FieldType[T] = t match {
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

      for (n ← p.knownFields if !fieldMap.contains(n)) {
        p.addKnownField(n, eliminatePreliminaryTypesIn)
      }
    }
  }

  // TODO type restrictions
  def check : Unit = for (p ← pools.par; f ← p.allFields) try { f.check } catch {
    case e : SkillException ⇒ throw new SkillException(s"check failed in $${p.name}.$${f.name}:\\n  $${e.getMessage}", e)
  }

  def flush : Unit = {
    check;
    mode match {
      case Write  ⇒ new StateWriter(this, FileOutputStream.write(path))
      case Append ⇒ new StateAppender(this, FileOutputStream.append(path))
    }
  }

  def close : Unit = {
    flush;
    // TODO invalidate state?
  }

  def changePath(newPath : Path) : Unit = mode match {
    case Write                       ⇒ path = newPath
    case Append if (path == newPath) ⇒ //nothing to do
    case Append ⇒
      Files.deleteIfExists(newPath);
      Files.copy(path, newPath);
      path = newPath
  }

  def changeMode(writeMode : Mode) : Unit = (mode, writeMode) match {
    case (Append, Write) ⇒ mode = Write
    case (Write, Append) ⇒
      throw new IllegalArgumentException(
        "Cannot change write mode from Write to Append, try to use open(<path>, Create, Append) instead."
      )
    case _ ⇒
  }
}

object State {
  /**
   * Opens a file and sets correct modes. This may involve reading data from the file.
   */
  def open(path : Path, modes : Seq[Mode]) : State = {
    // determine open mode
    // @note read is preferred over create, because empty files are legal and the file has been created by now if it did not exist yet
    // @note write is preferred over append, because usage is more inuitive
    val openMode = modes.collect {
      case m : OpenMode ⇒ m
    }.ensuring(_.size <= 1, throw new IOException("You can either create or read a file.")).headOption.getOrElse(Read)
    val writeMode = modes.collect {
      case m : WriteMode ⇒ m
    }.ensuring(_.size <= 1, throw new IOException("You can either write or append to a file.")).headOption.getOrElse(Write)

    // create the state 
    openMode match {
      case Create ⇒

        // initialization order of type information has to match file parser and can not be done in place
        val strings = new StringPool(null)
        val types = new ArrayBuffer[StoragePool[_ <: SkillType, _ <: SkillType]](2);
        val annotation = Annotation(types)
        val stringType = StringType(strings)

        // create type information
${
      var i = -1
      (for (t ← IR)
        yield s"""        val ${t.getName.capital} = new ${t.getName.capital}StoragePool(stringType, annotation, ${i += 1; i}${
        if (null == t.getSuperType) ""
        else { ", "+t.getSuperType.getName.capital }
      })
        types += ${t.getName.capital}"""
      ).mkString("\n")
    }
        new State(
${
      (for (t ← IR) yield s"""          ${t.getName.capital},""").mkString("\n")
    }
          strings,
          Array[StoragePool[_ <: SkillType, _ <: SkillType]](${IR.map(_.getName.capital).mkString(", ")}),
          path,
          writeMode
        )
      case Read ⇒
        FileParser.read(FileInputStream.open(path), writeMode)
    }
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
