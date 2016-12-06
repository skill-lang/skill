/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.Type
import de.ust.skill.ir.Typedef
import de.ust.skill.ir.UserType

trait SkillFileMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/SkillFile.scala")

    //package & imports
    out.write(s"""package ${packagePrefix}api


import java.io.File
import java.nio.file.Path

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import de.ust.skill.common.jvm.streams.FileInputStream
import de.ust.skill.common.jvm.streams.MappedInStream
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.Create
import de.ust.skill.common.scala.api.Read
import de.ust.skill.common.scala.api.ReadMode
import de.ust.skill.common.scala.api.ReadOnly
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Write
import de.ust.skill.common.scala.api.WriteMode
import de.ust.skill.common.scala.internal.SkillState
import de.ust.skill.common.scala.internal.StoragePool
import de.ust.skill.common.scala.internal.StringPool
import de.ust.skill.common.scala.internal.fieldTypes
import de.ust.skill.common.scala.internal.fieldTypes.AnnotationType
import de.ust.skill.common.scala.internal.InterfacePool
import de.ust.skill.common.scala.internal.UnrootedInterfacePool

/**
 * A skill file that corresponds to your specification. Have fun!
 *
 * @author Timm Felden
 */
final class SkillFile(
  _path : Path,
  _mode : WriteMode,
  _String : StringPool,
  _annotationType : fieldTypes.AnnotationType,
  _types : ArrayBuffer[StoragePool[_ <: SkillObject, _ <: SkillObject]],
  _typesByName : HashMap[String, StoragePool[_ <: SkillObject, _ <: SkillObject]])
    extends SkillState(_path, _mode, _String, _annotationType, _types, _typesByName) {

  private[api] def AnnotationType : AnnotationType = annotationType
${
      (for (t ← IR) yield s"""
  val ${name(t)} : internal.${storagePool(t)} = typesByName("${t.getSkillName}").asInstanceOf[internal.${storagePool(t)}]"""
      ).mkString
    }
${
      (for (t ← IRInterfaces if !t.getSuperType.isInstanceOf[UserType]) yield s"""
  val ${name(t)} : UnrootedInterfacePool[${mapType(t)}] =
    new UnrootedInterfacePool[${mapType(t)}]("${name(t)}", AnnotationType,
      Array[StoragePool[_ <: ${mapType(t)}, _ <: SkillObject]](${
        collectRealizationNames(t).map(name).mkString(", ")
      }));""").mkString
    }
${
      (for (t ← IRInterfaces if t.getSuperType.isInstanceOf[UserType]) yield s"""
  val ${name(t)} : InterfacePool[${mapType(t)}, ${mapType(t.getBaseType)}] =
    new InterfacePool[${mapType(t)}, ${mapType(t.getBaseType)}]("${name(t)}", ${name(t.getSuperType)},
      Array[StoragePool[_ <: ${mapType(t)}, _ <: ${mapType(t.getBaseType)}]](${
        collectRealizationNames(t).map(name).mkString(", ")
      }));""").mkString
    }
}

/**
 * @author Timm Felden
 */
object SkillFile {
  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def open(path : String, read : ReadMode = Read, write : WriteMode = Write) : SkillFile = {
    val f = new File(path)
    if (!f.exists())
      f.createNewFile()
    readFile(f.toPath, read, write)
  }
  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def open(file : File, read : ReadMode, write : WriteMode) : SkillFile = {
    if (!file.exists())
      file.createNewFile()
    readFile(file.toPath, read, write)
  }
  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def open(path : Path, read : ReadMode, write : WriteMode) : SkillFile = readFile(path, read, write)

  /**
   * same as open(create)
   */
  def create(path : Path, write : WriteMode = Write) : SkillFile = readFile(path, Create, write)

  /**
   * same as open(read)
   */
  def read(path : Path, write : WriteMode = Write) : SkillFile = readFile(path, Read, write)

  private def readFile(path : Path, read : ReadMode, write : WriteMode) : SkillFile = read match {
    case Read ⇒ internal.FileParser.read(FileInputStream.open(path, write == ReadOnly), write)

    case Create ⇒
      val String = new StringPool(null)
      val types = new ArrayBuffer[StoragePool[_ <: SkillObject, _ <: SkillObject]]()
      val typesByName = new HashMap[String, StoragePool[_ <: SkillObject, _ <: SkillObject]]()
      val Annotation = new AnnotationType(types, typesByName)
      val dataList = new ArrayBuffer[MappedInStream]()
      internal.FileParser.makeState(path, write, String, Annotation, types, typesByName, dataList)
  }
}
""")

    out.close()
  }

  private def collectRealizationNames(target : InterfaceType) : Seq[UserType] = {
    def reaches(t : Type) : Boolean = t match {
      case t : UserType      ⇒ t.getSuperInterfaces.contains(target) || t.getSuperInterfaces.exists(reaches)
      case t : InterfaceType ⇒ t.getSuperInterfaces.contains(target) || t.getSuperInterfaces.exists(reaches)
      case t : Typedef       ⇒ reaches(t.getTarget)
      case _                 ⇒ false
    }

    IR.filter(reaches).toSeq
  }
}
