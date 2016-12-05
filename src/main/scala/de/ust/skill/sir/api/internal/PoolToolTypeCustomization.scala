/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 05.12.2016                               *
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

final class ToolTypeCustomizationPool(poolIndex : Int)
    extends BasePool[_root_.de.ust.skill.sir.ToolTypeCustomization](
      poolIndex,
      "tooltypecustomization"
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.ToolTypeCustomization] = classOf[_root_.de.ust.skill.sir.ToolTypeCustomization]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.ToolTypeCustomization] = {
    val f = (name match {
      case "hints" ⇒ new KnownField_ToolTypeCustomization_hints(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint]]])
      case "restrictions" ⇒ new KnownField_ToolTypeCustomization_restrictions(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.ToolTypeCustomization]]

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
    val Clshints = classOf[KnownField_ToolTypeCustomization_hints]
    val Clsrestrictions = classOf[KnownField_ToolTypeCustomization_restrictions]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.ToolTypeCustomization]]](Clshints,Clsrestrictions)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clshints))
        dataFields += new KnownField_ToolTypeCustomization_hints(dataFields.size + 1, this, VariableLengthArray(state.Hint))
    if(fields.contains(Clsrestrictions))
        dataFields += new KnownField_ToolTypeCustomization_restrictions(dataFields.size + 1, this, VariableLengthArray(state.Restriction))
    // no auto fields
  

    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new ToolTypeCustomizationSubPool(poolIndex, name, this)

  override def allocateData : Unit = data = new Array[_root_.de.ust.skill.sir.ToolTypeCustomization](cachedSize)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.ToolTypeCustomization = {
    val r = new _root_.de.ust.skill.sir.ToolTypeCustomization(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.ToolTypeCustomization(i + 1)
        i += 1
      }
    }
  }

  def make(hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint](), restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]()) = {
    val r = new _root_.de.ust.skill.sir.ToolTypeCustomization(-1 - newObjects.size, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction])
    newObjects.append(r)
    r
  }
}

final class ToolTypeCustomizationSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.ToolTypeCustomization.UnknownSubType <: _root_.de.ust.skill.sir.ToolTypeCustomization, _root_.de.ust.skill.sir.ToolTypeCustomization])
    extends SubPool[_root_.de.ust.skill.sir.ToolTypeCustomization.UnknownSubType, _root_.de.ust.skill.sir.ToolTypeCustomization](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.ToolTypeCustomization.UnknownSubType] = classOf[_root_.de.ust.skill.sir.ToolTypeCustomization.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new ToolTypeCustomizationSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.ToolTypeCustomization.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.ToolTypeCustomization.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.ToolTypeCustomization.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
