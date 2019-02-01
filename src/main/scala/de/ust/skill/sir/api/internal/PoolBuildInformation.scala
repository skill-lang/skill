/*  ___ _  ___ _ _                                                                                                    *\
** / __| |/ (_) | |     Your SKilL scala Binding                                                                      **
** \__ \ ' <| | | |__   generated: 01.02.2019                                                                         **
** |___/_|\_\_|_|____|  by: feldentm                                                                                  **
\*                                                                                                                    */
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

final class BuildInformationPool(poolIndex : Int)
    extends BasePool[_root_.de.ust.skill.sir.BuildInformation](
      poolIndex,
      "buildinformation"
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.BuildInformation] = classOf[_root_.de.ust.skill.sir.BuildInformation]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.BuildInformation] = {
    val f = (name match {
      case "language" ⇒ new F_BuildInformation_language(ID, this, t.asInstanceOf[FieldType[java.lang.String]])
      case "options" ⇒ new F_BuildInformation_options(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[java.lang.String]]])
      case "output" ⇒ new F_BuildInformation_output(ID, this, t.asInstanceOf[FieldType[_root_.de.ust.skill.sir.FilePath]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.BuildInformation]]

    //check type
    if (t != f.t)
      throw new TypeMissmatchError(t, f.t.toString, f.name, name)

    val rs = restrictions.iterator
    while(rs.hasNext)
      f.addRestriction(rs.next())

    dataFields += f
    return f
  }
  override def ensureKnownFields(st : SkillState) {
    val state = st.asInstanceOf[SkillFile]
    // data fields
    val Clslanguage = classOf[F_BuildInformation_language]
    val Clsoptions = classOf[F_BuildInformation_options]
    val Clsoutput = classOf[F_BuildInformation_output]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.BuildInformation]]](Clslanguage,Clsoptions,Clsoutput)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clslanguage))
        dataFields += new F_BuildInformation_language(dataFields.size + 1, this, state.String)
    if(fields.contains(Clsoptions))
        dataFields += new F_BuildInformation_options(dataFields.size + 1, this, VariableLengthArray(state.String))
    if(fields.contains(Clsoutput))
        dataFields += new F_BuildInformation_output(dataFields.size + 1, this, state.FilePath)
    // no auto fields


    val fs = (dataFields ++ autoFields).iterator
    while (fs.hasNext)
      fs.next().createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new BuildInformationSubPool(poolIndex, name, this)

  override def allocateData : Unit = data = new Array[_root_.de.ust.skill.sir.BuildInformation](cachedSize)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.BuildInformation = {
    val r = new _root_.de.ust.skill.sir.BuildInformation(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.BuildInformation(i + 1)
        i += 1
      }
    }
  }

  def make(language : java.lang.String = null, options : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String](), output : _root_.de.ust.skill.sir.FilePath = null) = {
    val r = new _root_.de.ust.skill.sir.BuildInformation(-1 - newObjects.size, language : java.lang.String, options : scala.collection.mutable.ArrayBuffer[java.lang.String], output : _root_.de.ust.skill.sir.FilePath)
    newObjects.append(r)
    r
  }
}

final class BuildInformationSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.BuildInformation.UnknownSubType <: _root_.de.ust.skill.sir.BuildInformation, _root_.de.ust.skill.sir.BuildInformation])
    extends SubPool[_root_.de.ust.skill.sir.BuildInformation.UnknownSubType, _root_.de.ust.skill.sir.BuildInformation](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.BuildInformation.UnknownSubType] = classOf[_root_.de.ust.skill.sir.BuildInformation.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new BuildInformationSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.BuildInformation.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.BuildInformation.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.BuildInformation.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
