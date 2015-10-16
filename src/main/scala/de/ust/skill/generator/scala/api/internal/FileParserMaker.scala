/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api.internal
import de.ust.skill.generator.scala.GeneralOutputMaker

trait FileParserMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/internal/FileParser.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}api.internal

import java.nio.file.Path

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import de.ust.skill.common.jvm.streams.MappedInStream
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.TypeSystemError
import de.ust.skill.common.scala.api.WriteMode
import de.ust.skill.common.scala.internal.SkillFileParser
import de.ust.skill.common.scala.internal.StoragePool
import de.ust.skill.common.scala.internal.StringPool
import de.ust.skill.common.scala.internal.UnknownBasePool
import de.ust.skill.common.scala.internal.fieldTypes.AnnotationType
import de.ust.skill.common.scala.internal.restrictions.TypeRestriction

import _root_.${packagePrefix}api.SkillFile

/**
 * Parametrization of the common skill parser.
 *
 * @author Timm Felden
 */
object FileParser extends SkillFileParser[SkillFile] {

  // TODO we can make this faster using a hash map (for large type systems)
  def newPool(
    typeId : Int,
    name : String,
    superPool : StoragePool[_ <: SkillObject, _ <: SkillObject],
    rest : HashSet[TypeRestriction]) : StoragePool[_ <: SkillObject, _ <: SkillObject] = {
    name match {${
      (for (t ← IR)
        yield s"""
      case "${t.getSkillName}" ⇒${
        if (null == t.getSuperType) s"""
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type ${name(t)} with super type S{superPool.name}, but none was expected")
        else
          new ${storagePool(t)}(typeId)"""
        else s"""
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type ${name(t)} with no super type, but ${name(t.getSuperType)} was expected")
        else
          new ${storagePool(t)}(typeId, superPool.asInstanceOf[${storagePool(t.getSuperType)}])"""
      }"""
      ).mkString("\n")
    }
      case _ ⇒
        if (null == superPool)
          new UnknownBasePool(name, typeId)
        else
          superPool.makeSubPool(name, typeId)
    }
  }

  def makeState(path : Path,
                mode : WriteMode,
                String : StringPool,
                Annotation : AnnotationType,
                types : ArrayBuffer[StoragePool[_ <: SkillObject, _ <: SkillObject]],
                typesByName : HashMap[String, StoragePool[_ <: SkillObject, _ <: SkillObject]],
                dataList : ArrayBuffer[MappedInStream]) : SkillFile = {

    // ensure that pools exist at all${
      (for (t ← IR)
        yield s"""
    typesByName.get("${t.getSkillName}").getOrElse {
      val p = newPool(types.size + 32, "${t.getSkillName}", ${
        if (null == t.getSuperType) "null"
        else s"""typesByName("${t.getSuperType.getSkillName}")"""
      }, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("${t.getSkillName}", p)
    }"""
      ).mkString
    }

    // trigger allocation and instance creation
    types.foreach(_.allocateData)
    types.par.foreach(_.allocateInstances)

    triggerFieldDeserialization(types, dataList)

    val r = new SkillFile(path, mode, String, Annotation, types, typesByName)
    types.par.foreach(_.ensureKnownFields(r))
    r
  }
}
""")

    //class prefix
    out.close()
  }
}
