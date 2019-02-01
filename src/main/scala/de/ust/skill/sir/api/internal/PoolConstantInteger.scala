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

final class ConstantIntegerPool(poolIndex : Int,
superPool: SimpleTypePool)
    extends SubPool[_root_.de.ust.skill.sir.ConstantInteger, de.ust.skill.sir.Type](
      poolIndex,
      "constantinteger",
superPool
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.ConstantInteger] = classOf[_root_.de.ust.skill.sir.ConstantInteger]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.ConstantInteger] = {
    val f = (name match {
      case "value" ⇒ new F_ConstantInteger_value(ID, this)
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.ConstantInteger]]

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
    val Clsvalue = classOf[F_ConstantInteger_value]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.ConstantInteger]]](Clsvalue)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clsvalue))
        dataFields += new F_ConstantInteger_value(dataFields.size + 1, this, V64)
    // no auto fields


    val fs = (dataFields ++ autoFields).iterator
    while (fs.hasNext)
      fs.next().createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new ConstantIntegerSubPool(poolIndex, name, this)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.ConstantInteger = {
    val r = new _root_.de.ust.skill.sir.ConstantInteger(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.ConstantInteger(i + 1)
        i += 1
      }
    }
  }

  def make(value : Long = 0, name : _root_.de.ust.skill.sir.Identifier = null) = {
    val r = new _root_.de.ust.skill.sir.ConstantInteger(-1 - newObjects.size, value : Long, name : _root_.de.ust.skill.sir.Identifier)
    newObjects.append(r)
    r
  }
}

final class ConstantIntegerSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.ConstantInteger.UnknownSubType <: _root_.de.ust.skill.sir.ConstantInteger, _root_.de.ust.skill.sir.Type])
    extends SubPool[_root_.de.ust.skill.sir.ConstantInteger.UnknownSubType, _root_.de.ust.skill.sir.Type](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.ConstantInteger.UnknownSubType] = classOf[_root_.de.ust.skill.sir.ConstantInteger.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new ConstantIntegerSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.ConstantInteger.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.ConstantInteger.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.ConstantInteger.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
