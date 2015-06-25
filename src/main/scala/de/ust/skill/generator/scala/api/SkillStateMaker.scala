/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api

import scala.collection.JavaConversions._
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.restriction.MonotoneRestriction
import de.ust.skill.ir.restriction.SingletonRestriction

trait SkillStateMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/SkillState.scala")

    //package & imports
    out.write(s"""package ${packagePrefix}api

import java.nio.file.Path
import java.io.File

import scala.collection.mutable.ArrayBuffer

import ${packagePrefix}internal.FileParser
import ${packagePrefix}internal.SerializableState
import ${packagePrefix}internal.SkillType
import ${packagePrefix}internal.{RefArray, AnnotationArray}
import ${packagePrefix}internal.{RefArrayBuffer, AnnotationArrayBuffer}
import ${packagePrefix}internal.{RefListBuffer, AnnotationListBuffer}
import ${packagePrefix}internal.{RefHashSet, AnnotationHashSet}
import ${packagePrefix}internal.MapView
import ${packagePrefix}internal.{BasicMapView, BasicRefMapView, BasicAnnotationMapView}
import ${packagePrefix}internal.{RefBasicMapView, RefMapView, RefAnnotationMapView}
import ${packagePrefix}internal.{AnnotationBasicMapView, AnnotationRefMapView, AnnotationMapView}
import ${packagePrefix}internal.{BasicMapMapView, RefMapMapView, AnnotationMapMapView}

/**
 * The public interface to the SKilL state, which keeps track of serializable objects and provides (de)serialization
 *  capabilities.
 *
 * @author Timm Felden
 */
trait SkillState {
${
      (for (t ← IR) yield s"  val ${t.getCapitalName}: ${t.getCapitalName}Access").mkString("\n")
    }

  val String: StringAccess

  def all: Iterator[Access[_ <: SkillType]]

  /**
   * Creates a new SKilL file at target. The recommended file extension is ".sf".
   *
   * @note Updates fromPath iff fromPath==null, i.e. if the state has been created out of thin air.
   */
  def write(target: Path): Unit

  /**
   * Write new content to the read SKilL file.
   */
  def write():Unit

  /**
   * Appends new content to the read SKilL file.
   *
   * @pre canAppend
   */
  def append(): Unit

  /**
   * Appends new content to the read SKilL file and stores the result in target.
   *
   * @pre canAppend
   */
  def append(target: Path): Unit

  /**
   * Checks restrictions in types. Restrictions are checked before write/append, where an error is raised if they do not
   * hold.
   */
  def checkRestrictions : Boolean

  /**
   * Creates a new constant length array of references
   */
  def makeRefArray[T <: SkillType](length : Int, access : Access[T]) : RefArray[T]

  /**
   * Creates a new constant length array of annotations
   */
  def makeAnnotationArray(length : Int) : AnnotationArray

  /**
   * Creates a new variable length array of references
   */
  def makeRefVarArray[T <: SkillType](access : Access[T]) : RefArrayBuffer[T]

  /**
   * Creates a new variable length array of annotations
   */
  def makeAnnotationVarArray() : AnnotationArrayBuffer

  /**
   * Creates a new list of references
   */
  def makeRefList[T <: SkillType](access : Access[T]) : RefListBuffer[T]

  /**
   * Creates a new list of annotations
   */
  def makeAnnotationList() : AnnotationListBuffer

  /**
   * Creates a new set of references
   */
  def makeRefSet[T <: SkillType](access : Access[T]) : RefHashSet[T]

  /**
   * Creates a new set of annotations
   */
  def makeAnnotationSet() : AnnotationHashSet

  /**
   * Creates a new map with primitive key and value types
   */
  def makeBasicMap[A, B]() : BasicMapView[A, B]

  /**
   * Creates a new map with primitive key type and reference value type
   */
  def makeBasicRefMap[A, B <: SkillType](access : Access[B]) : BasicRefMapView[A, B]

  /**
   * Creates a new map with primitve key type and annotation value type
   */
  def makeBasicAnnotationMap[A]() : BasicAnnotationMapView[A]

  /**
   * Creates a new map with reference key type and primitive value type
   */
  def makeRefBasicMap[A <: SkillType, B](access : Access[A]) : RefBasicMapView[A, B]

  /**
   * Creates a new map with reference key and value types
   */
  def makeRefMap[A <: SkillType, B <: SkillType](accessA : Access[A], accessB : Access[B]) : RefMapView[A, B]

  /**
   * Creates a new map with reference key type and annotation value type
   */
  def makeRefAnnotationMap[A <: SkillType](access : Access[A]) : RefAnnotationMapView[A]

  /**
   * Creates a new map with annotation key type and primitive value type
   */
  def makeAnnotationBasicMap[B]() : AnnotationBasicMapView[B]

  /**
   * Creates a new map with annotation key type and reference value type
   */
  def makeAnnotationRefMap[B <: SkillType](access : Access[B]) : AnnotationRefMapView[B]

  /**
   * Creates a new map with annotation key and value type
   */
  def makeAnnotationMap() : AnnotationMapView

  /**
   * Creates a new map with primitive key type and map value type
   */
  def makeBasicMapMap[A, B <: MapView[_, _]](map : B) : BasicMapMapView[A, B]

  /**
   * Creates a new map with reference key type and map value type
   */
  def makeRefMapMap[A <: SkillType, B <: MapView[_, _]](access : Access[A], map : B) : RefMapMapView[A, B]

  /**
   * Creates a new map with annotation key type and map value type
   */
  def makeAnnotationMapMap[B <: MapView[_, _]](map : B) : AnnotationMapMapView[B]
}

object SkillState {
  /**
   * Creates a new and empty SKilL state.
   */
  def create: SkillState = SerializableState.create

  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def read(path: Path): SkillState = FileParser.read(path)
  def read(file: File): SkillState = read(file.toPath)
  def read(path: String): SkillState = read(new File(path))
}""")

    out.close()
  }
}
