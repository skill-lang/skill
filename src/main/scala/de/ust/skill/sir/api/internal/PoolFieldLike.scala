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

final class FieldLikePool(poolIndex : Int)
    extends BasePool[_root_.de.ust.skill.sir.FieldLike](
      poolIndex,
      "fieldlike"
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.FieldLike] = classOf[_root_.de.ust.skill.sir.FieldLike]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.FieldLike] = {
    val f = (name match {
      case "comment" ⇒ new KnownField_FieldLike_comment(ID, this, t.asInstanceOf[FieldType[_root_.de.ust.skill.sir.Comment]])
      case "name" ⇒ new KnownField_FieldLike_name(ID, this, t.asInstanceOf[FieldType[_root_.de.ust.skill.sir.Identifier]])
      case "type" ⇒ new KnownField_FieldLike_type(ID, this, t.asInstanceOf[FieldType[_root_.de.ust.skill.sir.Type]])
      case "hints" ⇒ new KnownField_FieldLike_hints(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint]]])
      case "restrictions" ⇒ new KnownField_FieldLike_restrictions(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.FieldLike]]

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
    val Clscomment = classOf[KnownField_FieldLike_comment]
    val Clsname = classOf[KnownField_FieldLike_name]
    val Clstype = classOf[KnownField_FieldLike_type]
    val Clshints = classOf[KnownField_FieldLike_hints]
    val Clsrestrictions = classOf[KnownField_FieldLike_restrictions]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.FieldLike]]](Clscomment,Clsname,Clstype,Clshints,Clsrestrictions)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clscomment))
        dataFields += new KnownField_FieldLike_comment(dataFields.size + 1, this, state.Comment)
    if(fields.contains(Clsname))
        dataFields += new KnownField_FieldLike_name(dataFields.size + 1, this, state.Identifier)
    if(fields.contains(Clstype))
        dataFields += new KnownField_FieldLike_type(dataFields.size + 1, this, state.Type)
    if(fields.contains(Clshints))
        dataFields += new KnownField_FieldLike_hints(dataFields.size + 1, this, VariableLengthArray(state.Hint))
    if(fields.contains(Clsrestrictions))
        dataFields += new KnownField_FieldLike_restrictions(dataFields.size + 1, this, VariableLengthArray(state.Restriction))
    // no auto fields
  

    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new FieldLikeSubPool(poolIndex, name, this)

  override def allocateData : Unit = data = new Array[_root_.de.ust.skill.sir.FieldLike](cachedSize)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.FieldLike = {
    val r = new _root_.de.ust.skill.sir.FieldLike(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.FieldLike(i + 1)
        i += 1
      }
    }
  }

  def make(comment : _root_.de.ust.skill.sir.Comment = null, name : _root_.de.ust.skill.sir.Identifier = null, `type` : _root_.de.ust.skill.sir.Type = null, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint](), restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]()) = {
    val r = new _root_.de.ust.skill.sir.FieldLike(-1 - newObjects.size, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, `type` : _root_.de.ust.skill.sir.Type, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction])
    newObjects.append(r)
    r
  }
}

final class FieldLikeSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.FieldLike.UnknownSubType <: _root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.FieldLike])
    extends SubPool[_root_.de.ust.skill.sir.FieldLike.UnknownSubType, _root_.de.ust.skill.sir.FieldLike](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.FieldLike.UnknownSubType] = classOf[_root_.de.ust.skill.sir.FieldLike.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new FieldLikeSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.FieldLike.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.FieldLike.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.FieldLike.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
