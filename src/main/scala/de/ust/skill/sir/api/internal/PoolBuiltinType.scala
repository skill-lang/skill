/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 09.11.2016                               *
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

final class BuiltinTypePool(poolIndex : Int,
superPool: TypePool)
    extends SubPool[_root_.de.ust.skill.sir.BuiltinType, de.ust.skill.sir.Type](
      poolIndex,
      "builtintype",
superPool
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.BuiltinType] = classOf[_root_.de.ust.skill.sir.BuiltinType]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.BuiltinType] = {
    val f = (name match {
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.BuiltinType]]

    //check type
    if (t != f.t)
      throw new TypeMissmatchError(t, f.t.toString, f.name, name)

    restrictions.foreach(f.addRestriction(_))
    dataFields += f
    return f
  }
  override def ensureKnownFields(st : SkillState) {
    // no data fields
    // no auto fields
  

    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new BuiltinTypeSubPool(poolIndex, name, this)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.BuiltinType = {
    val r = new _root_.de.ust.skill.sir.BuiltinType(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.BuiltinType(i + 1)
        i += 1
      }
    }
  }

  def make() = {
    val r = new _root_.de.ust.skill.sir.BuiltinType(-1 - newObjects.size)
    newObjects.append(r)
    r
  }
}

final class BuiltinTypeSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.BuiltinType.UnknownSubType <: _root_.de.ust.skill.sir.BuiltinType, _root_.de.ust.skill.sir.Type])
    extends SubPool[_root_.de.ust.skill.sir.BuiltinType.UnknownSubType, _root_.de.ust.skill.sir.Type](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.BuiltinType.UnknownSubType] = classOf[_root_.de.ust.skill.sir.BuiltinType.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new BuiltinTypeSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.BuiltinType.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.BuiltinType.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.BuiltinType.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
