/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 19.11.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import _root_.de.ust.skill.generator.genericBinding.api._
import _root_.de.ust.skill.generator.genericBinding.internal.streams.FileInputStream
import _root_.de.ust.skill.generator.genericBinding.internal.streams.FileOutputStream

/**
 * This class is used to handle objects in a serializable state.
 *
 * @author Timm Felden
 */
final class State private[internal] (

  val String : StringAccess,
  val pools : Array[StoragePool[_ <: SkillType, _ <: SkillType]],
  var path : Path,
  val mode : WriteMode)
    extends SkillFile {

  val poolByName = pools.map(_.name).zip(pools).toSeq.toMap

  finalizePools;

  def all = pools.iterator.asInstanceOf[Iterator[Access[_ <: SkillType]]]

  @inline private def finalizePools {
    @inline def eliminatePreliminaryTypesIn[T](t : FieldType[T]) : FieldType[T] = t match {
      case TypeDefinitionIndex(i) ⇒ try {
        pools(i.toInt).asInstanceOf[FieldType[T]]
      } catch {
        case e : Exception ⇒ throw new IllegalStateException(s"inexistent user type $i (user types: ${poolByName.mkString})", e)
      }
      case TypeDefinitionName(n) ⇒ try {
        poolByName(n).asInstanceOf[FieldType[T]]
      } catch {
        case e : Exception ⇒ throw new IllegalStateException(s"inexistent user type $n (user types: ${poolByName.mkString})", e)
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

      for ((n, (t, _)) ← p.knownFields if !fieldMap.contains(n)) {
        p.addField(p.fields.size, eliminatePreliminaryTypesIn(t), n, HashSet())
      }
    }
  }

  // TODO type restrictions
  def check : Unit = for (p ← pools.par; f ← p.allFields) try { f.check } catch {
    case e : SkillException ⇒ throw new SkillException(s"check failed in ${p.name}.${f.name}:\n  ${e.getMessage}", e)
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

        new State(

          strings,
          Array[StoragePool[_ <: SkillType, _ <: SkillType]](),
          path,
          writeMode
        )
      case Read ⇒
        FileParser.read(new FileInputStream(path), writeMode)
    }
  }
}
