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

final class IdentifierPool(poolIndex : Int)
    extends BasePool[_root_.de.ust.skill.sir.Identifier](
      poolIndex,
      "identifier"
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.Identifier] = classOf[_root_.de.ust.skill.sir.Identifier]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.Identifier] = {
    val f = (name match {
      case "parts" ⇒ new F_Identifier_parts(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[java.lang.String]]])
      case "skillname" ⇒ new F_Identifier_skillname(ID, this, t.asInstanceOf[FieldType[java.lang.String]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.Identifier]]

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
    val Clsparts = classOf[F_Identifier_parts]
    val Clsskillname = classOf[F_Identifier_skillname]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.Identifier]]](Clsparts,Clsskillname)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clsparts))
        dataFields += new F_Identifier_parts(dataFields.size + 1, this, VariableLengthArray(state.String))
    if(fields.contains(Clsskillname))
        dataFields += new F_Identifier_skillname(dataFields.size + 1, this, state.String)
    // no auto fields


    val fs = (dataFields ++ autoFields).iterator
    while (fs.hasNext)
      fs.next().createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new IdentifierSubPool(poolIndex, name, this)

  override def allocateData : Unit = data = new Array[_root_.de.ust.skill.sir.Identifier](cachedSize)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.Identifier = {
    val r = new _root_.de.ust.skill.sir.Identifier(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.Identifier(i + 1)
        i += 1
      }
    }
  }

  def make(parts : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String](), skillname : java.lang.String = null) = {
    val r = new _root_.de.ust.skill.sir.Identifier(-1 - newObjects.size, parts : scala.collection.mutable.ArrayBuffer[java.lang.String], skillname : java.lang.String)
    newObjects.append(r)
    r
  }
}

final class IdentifierSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.Identifier.UnknownSubType <: _root_.de.ust.skill.sir.Identifier, _root_.de.ust.skill.sir.Identifier])
    extends SubPool[_root_.de.ust.skill.sir.Identifier.UnknownSubType, _root_.de.ust.skill.sir.Identifier](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.Identifier.UnknownSubType] = classOf[_root_.de.ust.skill.sir.Identifier.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new IdentifierSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.Identifier.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.Identifier.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.Identifier.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
