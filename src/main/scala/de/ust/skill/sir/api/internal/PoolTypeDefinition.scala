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

final class TypeDefinitionPool(poolIndex : Int,
superPool: UserdefinedTypePool)
    extends SubPool[_root_.de.ust.skill.sir.TypeDefinition, de.ust.skill.sir.Type](
      poolIndex,
      "typedefinition",
superPool
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.TypeDefinition] = classOf[_root_.de.ust.skill.sir.TypeDefinition]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.TypeDefinition] = {
    val f = (name match {
      case "target" ⇒ new KnownField_TypeDefinition_target(ID, this, t.asInstanceOf[FieldType[_root_.de.ust.skill.sir.Type]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.TypeDefinition]]

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
    val Clstarget = classOf[KnownField_TypeDefinition_target]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.TypeDefinition]]](Clstarget)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clstarget))
        dataFields += new KnownField_TypeDefinition_target(dataFields.size + 1, this, state.Type)
    // no auto fields


    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new TypeDefinitionSubPool(poolIndex, name, this)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.TypeDefinition = {
    val r = new _root_.de.ust.skill.sir.TypeDefinition(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.TypeDefinition(i + 1)
        i += 1
      }
    }
  }

  def make(target : _root_.de.ust.skill.sir.Type = null, comment : _root_.de.ust.skill.sir.Comment = null, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint](), restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction](), name : _root_.de.ust.skill.sir.Identifier = null) = {
    val r = new _root_.de.ust.skill.sir.TypeDefinition(-1 - newObjects.size, target : _root_.de.ust.skill.sir.Type, comment : _root_.de.ust.skill.sir.Comment, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction], name : _root_.de.ust.skill.sir.Identifier)
    newObjects.append(r)
    r
  }
}

final class TypeDefinitionSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.TypeDefinition.UnknownSubType <: _root_.de.ust.skill.sir.TypeDefinition, _root_.de.ust.skill.sir.Type])
    extends SubPool[_root_.de.ust.skill.sir.TypeDefinition.UnknownSubType, _root_.de.ust.skill.sir.Type](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.TypeDefinition.UnknownSubType] = classOf[_root_.de.ust.skill.sir.TypeDefinition.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new TypeDefinitionSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.TypeDefinition.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.TypeDefinition.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.TypeDefinition.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
