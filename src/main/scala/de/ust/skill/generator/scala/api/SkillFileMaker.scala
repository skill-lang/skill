/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api

import scala.collection.JavaConversions._
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.restriction.MonotoneRestriction
import de.ust.skill.ir.restriction.SingletonRestriction

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
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.Create
import de.ust.skill.common.scala.api.Read
import de.ust.skill.common.scala.api.ReadMode
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Write
import de.ust.skill.common.scala.api.WriteMode
import de.ust.skill.common.scala.internal.SkillState
import de.ust.skill.common.scala.internal.StoragePool
import de.ust.skill.common.scala.internal.StringPool
import de.ust.skill.common.scala.internal.fieldTypes

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
${
      (for (t ← IR) yield s"""
  val ${name(t)} : internal.${storagePool(t)} = typesByName("${t.getSkillName}").asInstanceOf[internal.${storagePool(t)}]""").mkString
    }
}


/**
 * @author Timm Felden
 */
object SkillFile {
  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def open(path : String, read : ReadMode = Read, write : WriteMode = Write) : SkillFile = readFile(new File(path).ensuring(exists(_)).toPath, read, write)
  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def open(file : File, read : ReadMode, write : WriteMode) : SkillFile = readFile(file.ensuring(exists(_)).toPath, read, write)
  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def open(path : Path, read : ReadMode, write : WriteMode) : SkillFile = readFile(path, read, write)

  private def readFile(path : Path, read : ReadMode, write : WriteMode) : SkillFile = read match {
    case Read ⇒ internal.FileParser.read(FileInputStream.open(path), write)

    case Create ⇒
      /**
       *  initialization order of type information has to match file parser
       *  and can not be done in place
       */
      ???
    //               Strings : Skill.String_Pools.Pool :=
    //                 Skill.String_Pools.Create (Skill.Streams.Input (null));
    //               Types : Skill.Types.Pools.Type_Vector :=
    //                 Skill.Types.Pools.P_Type_Vector.Empty_Vector;
    //               String_Type : Skill.Field_Types.Builtin.String_Type_P
    //                 .Field_Type :=
    //                 Skill.Field_Types.Builtin.String_Type_P.Make (Strings);
    //               Annotation_Type : Skill.Field_Types.Builtin.Annotation_Type_P
    //                 .Field_Type :=
    //                 Skill.Field_Types.Builtin.Annotation (Types);
    //            begin
    //               return Make_State
    //                   (Path            => new String'(Path),
    //                    Mode            => Write_M,
    //                    Strings         => Strings,
    //                    String_Type     => String_Type,
    //                    Annotation_Type => Annotation_Type,
    //                    Types           => Types,
    //                    Types_By_Name   => Skill.Types.Pools.P_Type_Map.Empty_Map);
  }

  // ensures existence :)
  private def exists(f : File) = {
    if (!f.exists())
      f.createNewFile()
    true
  }
}
""")

    out.close()
  }
}
