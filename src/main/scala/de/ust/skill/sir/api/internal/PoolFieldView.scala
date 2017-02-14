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

final class FieldViewPool(poolIndex : Int,
superPool: FieldLikePool)
    extends SubPool[_root_.de.ust.skill.sir.FieldView, de.ust.skill.sir.FieldLike](
      poolIndex,
      "fieldview",
superPool
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.FieldView] = classOf[_root_.de.ust.skill.sir.FieldView]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.FieldView] = {
    val f = (name match {
      case "target" ⇒ new KnownField_FieldView_target(ID, this, t.asInstanceOf[FieldType[_root_.de.ust.skill.sir.FieldLike]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.FieldView]]

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
    val Clstarget = classOf[KnownField_FieldView_target]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.FieldView]]](Clstarget)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clstarget))
        dataFields += new KnownField_FieldView_target(dataFields.size + 1, this, state.FieldLike)
    // no auto fields


    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new FieldViewSubPool(poolIndex, name, this)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.FieldView = {
    val r = new _root_.de.ust.skill.sir.FieldView(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.FieldView(i + 1)
        i += 1
      }
    }
  }

  def make(target : _root_.de.ust.skill.sir.FieldLike = null, comment : _root_.de.ust.skill.sir.Comment = null, name : _root_.de.ust.skill.sir.Identifier = null, `type` : _root_.de.ust.skill.sir.Type = null, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint](), restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]()) = {
    val r = new _root_.de.ust.skill.sir.FieldView(-1 - newObjects.size, target : _root_.de.ust.skill.sir.FieldLike, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, `type` : _root_.de.ust.skill.sir.Type, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction])
    newObjects.append(r)
    r
  }
}

final class FieldViewSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.FieldView.UnknownSubType <: _root_.de.ust.skill.sir.FieldView, _root_.de.ust.skill.sir.FieldLike])
    extends SubPool[_root_.de.ust.skill.sir.FieldView.UnknownSubType, _root_.de.ust.skill.sir.FieldLike](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.FieldView.UnknownSubType] = classOf[_root_.de.ust.skill.sir.FieldView.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new FieldViewSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.FieldView.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.FieldView.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.FieldView.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
