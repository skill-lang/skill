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

final class SimpleTypePool(poolIndex : Int,
superPool: BuiltinTypePool)
    extends SubPool[_root_.de.ust.skill.sir.SimpleType, de.ust.skill.sir.Type](
      poolIndex,
      "simpletype",
superPool
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.SimpleType] = classOf[_root_.de.ust.skill.sir.SimpleType]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.SimpleType] = {
    val f = (name match {
      case "name" ⇒ new F_SimpleType_name(ID, this, t.asInstanceOf[FieldType[_root_.de.ust.skill.sir.Identifier]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.SimpleType]]

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
    val Clsname = classOf[F_SimpleType_name]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.SimpleType]]](Clsname)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clsname))
        dataFields += new F_SimpleType_name(dataFields.size + 1, this, state.Identifier)
    // no auto fields


    val fs = (dataFields ++ autoFields).iterator
    while (fs.hasNext)
      fs.next().createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new SimpleTypeSubPool(poolIndex, name, this)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.SimpleType = {
    val r = new _root_.de.ust.skill.sir.SimpleType(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.SimpleType(i + 1)
        i += 1
      }
    }
  }

  def make(name : _root_.de.ust.skill.sir.Identifier = null) = {
    val r = new _root_.de.ust.skill.sir.SimpleType(-1 - newObjects.size, name : _root_.de.ust.skill.sir.Identifier)
    newObjects.append(r)
    r
  }
}

final class SimpleTypeSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.SimpleType.UnknownSubType <: _root_.de.ust.skill.sir.SimpleType, _root_.de.ust.skill.sir.Type])
    extends SubPool[_root_.de.ust.skill.sir.SimpleType.UnknownSubType, _root_.de.ust.skill.sir.Type](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.SimpleType.UnknownSubType] = classOf[_root_.de.ust.skill.sir.SimpleType.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new SimpleTypeSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.SimpleType.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.SimpleType.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.SimpleType.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
