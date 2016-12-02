/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 01.12.2016                               *
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

final class ClassTypePool(poolIndex : Int,
superPool: UserdefinedTypePool)
    extends SubPool[_root_.de.ust.skill.sir.ClassType, de.ust.skill.sir.Type](
      poolIndex,
      "classtype",
superPool
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.ClassType] = classOf[_root_.de.ust.skill.sir.ClassType]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.ClassType] = {
    val f = (name match {
      case "fields" ⇒ new KnownField_ClassType_fields(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike]]])
      case "interfaces" ⇒ new KnownField_ClassType_interfaces(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType]]])
      case "super" ⇒ new KnownField_ClassType_super(ID, this, t.asInstanceOf[FieldType[_root_.de.ust.skill.sir.ClassType]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.ClassType]]

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
    val Clsfields = classOf[KnownField_ClassType_fields]
    val Clsinterfaces = classOf[KnownField_ClassType_interfaces]
    val Clssuper = classOf[KnownField_ClassType_super]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.ClassType]]](Clsfields,Clsinterfaces,Clssuper)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clsfields))
        dataFields += new KnownField_ClassType_fields(dataFields.size + 1, this, VariableLengthArray(state.FieldLike))
    if(fields.contains(Clsinterfaces))
        dataFields += new KnownField_ClassType_interfaces(dataFields.size + 1, this, SetType(state.InterfaceType))
    if(fields.contains(Clssuper))
        dataFields += new KnownField_ClassType_super(dataFields.size + 1, this, state.ClassType)
    // no auto fields
  

    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new ClassTypeSubPool(poolIndex, name, this)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.ClassType = {
    val r = new _root_.de.ust.skill.sir.ClassType(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.ClassType(i + 1)
        i += 1
      }
    }
  }

  def make(fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike](), interfaces : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType] = scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType](), `super` : _root_.de.ust.skill.sir.ClassType = null, comment : _root_.de.ust.skill.sir.Comment = null, name : _root_.de.ust.skill.sir.Identifier = null, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint](), restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]()) = {
    val r = new _root_.de.ust.skill.sir.ClassType(-1 - newObjects.size, fields : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.FieldLike], interfaces : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.InterfaceType], `super` : _root_.de.ust.skill.sir.ClassType, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction])
    newObjects.append(r)
    r
  }
}

final class ClassTypeSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.ClassType.UnknownSubType <: _root_.de.ust.skill.sir.ClassType, _root_.de.ust.skill.sir.Type])
    extends SubPool[_root_.de.ust.skill.sir.ClassType.UnknownSubType, _root_.de.ust.skill.sir.Type](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.ClassType.UnknownSubType] = classOf[_root_.de.ust.skill.sir.ClassType.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new ClassTypeSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.ClassType.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.ClassType.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.ClassType.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
