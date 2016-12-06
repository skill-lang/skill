/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 06.12.2016                               *
 * |___/_|\_\_|_|____|    by: feldentm                                        *
\*                                                                            */
package de.ust.skill.sir.api.internal

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.WrappedArray
import scala.reflect.Manifest

import de.ust.skill.common.jvm.streams.InStream

import de.ust.skill.common.scala.SkillID
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.TypeMissmatchError
import de.ust.skill.common.scala.internal.BasePool
import de.ust.skill.common.scala.internal.FieldDeclaration
import de.ust.skill.common.scala.internal.SkillState
import de.ust.skill.common.scala.internal.SingletonStoragePool
import de.ust.skill.common.scala.internal.StoragePool
import de.ust.skill.common.scala.internal.SubPool
import de.ust.skill.common.scala.internal.fieldTypes._
import de.ust.skill.common.scala.internal.restrictions.FieldRestriction

import _root_.de.ust.skill.sir.api._

final class FilePathPool(poolIndex : Int)
    extends BasePool[_root_.de.ust.skill.sir.FilePath](
      poolIndex,
      "filepath"
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.FilePath] = classOf[_root_.de.ust.skill.sir.FilePath]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.FilePath] = {
    val f = (name match {
      case "isabsolut" ⇒ new KnownField_FilePath_isAbsolut(ID, this)
      case "parts" ⇒ new KnownField_FilePath_parts(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[java.lang.String]]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.FilePath]]

    //check type
    if (t != f.t)
      throw new TypeMissmatchError(t, f.t.toString, f.name, name)

    restrictions.foreach(f.addRestriction(_))
    dataFields += f
    return f
  }
  override def ensureKnownFields(st : SkillState) {
    val state = st.asInstanceOf[SkillFile]
    // data fields
    val ClsisAbsolut = classOf[KnownField_FilePath_isAbsolut]
    val Clsparts = classOf[KnownField_FilePath_parts]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.FilePath]]](ClsisAbsolut,Clsparts)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(ClsisAbsolut))
        dataFields += new KnownField_FilePath_isAbsolut(dataFields.size + 1, this, BoolType)
    if(fields.contains(Clsparts))
        dataFields += new KnownField_FilePath_parts(dataFields.size + 1, this, VariableLengthArray(state.String))
    // no auto fields


    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new FilePathSubPool(poolIndex, name, this)

  override def allocateData : Unit = data = new Array[_root_.de.ust.skill.sir.FilePath](cachedSize)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.FilePath = {
    val r = new _root_.de.ust.skill.sir.FilePath(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.FilePath(i + 1)
        i += 1
      }
    }
  }

  def make(isAbsolut : scala.Boolean = false, parts : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String]()) = {
    val r = new _root_.de.ust.skill.sir.FilePath(-1 - newObjects.size, isAbsolut : scala.Boolean, parts : scala.collection.mutable.ArrayBuffer[java.lang.String])
    newObjects.append(r)
    r
  }
}

final class FilePathSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.FilePath.UnknownSubType <: _root_.de.ust.skill.sir.FilePath, _root_.de.ust.skill.sir.FilePath])
    extends SubPool[_root_.de.ust.skill.sir.FilePath.UnknownSubType, _root_.de.ust.skill.sir.FilePath](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.FilePath.UnknownSubType] = classOf[_root_.de.ust.skill.sir.FilePath.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new FilePathSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.FilePath.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.FilePath.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.FilePath.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
