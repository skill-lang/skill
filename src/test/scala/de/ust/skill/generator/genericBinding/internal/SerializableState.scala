/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 29.10.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import java.nio.file.Files
import java.nio.file.Path

import de.ust.skill.generator.genericBinding.api._
import de.ust.skill.generator.genericBinding.internal.streams.FileOutputStream

/**
 * This class is used to handle objects in a serializable state.
 *
 * @author Timm Felden
 */
final class SerializableState(

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

    new SerializableState(

      new StringPool(null),
      Array[StoragePool[_ <: SkillType, _ <: SkillType]](),
      None
    )
  }
}
