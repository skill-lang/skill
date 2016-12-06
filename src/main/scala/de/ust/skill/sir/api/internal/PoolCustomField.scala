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

final class CustomFieldPool(poolIndex : Int,
superPool: FieldLikePool)
    extends SubPool[_root_.de.ust.skill.sir.CustomField, de.ust.skill.sir.FieldLike](
      poolIndex,
      "customfield",
superPool
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.CustomField] = classOf[_root_.de.ust.skill.sir.CustomField]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.CustomField] = {
    val f = (name match {
      case "language" ⇒ new KnownField_CustomField_language(ID, this, t.asInstanceOf[FieldType[java.lang.String]])
      case "options" ⇒ new KnownField_CustomField_options(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CustomFieldOption]]])
      case "typename" ⇒ new KnownField_CustomField_typename(ID, this, t.asInstanceOf[FieldType[java.lang.String]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.CustomField]]

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
    val Clslanguage = classOf[KnownField_CustomField_language]
    val Clsoptions = classOf[KnownField_CustomField_options]
    val Clstypename = classOf[KnownField_CustomField_typename]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.CustomField]]](Clslanguage,Clsoptions,Clstypename)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clslanguage))
        dataFields += new KnownField_CustomField_language(dataFields.size + 1, this, state.String)
    if(fields.contains(Clsoptions))
        dataFields += new KnownField_CustomField_options(dataFields.size + 1, this, VariableLengthArray(state.CustomFieldOption))
    if(fields.contains(Clstypename))
        dataFields += new KnownField_CustomField_typename(dataFields.size + 1, this, state.String)
    // no auto fields


    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new CustomFieldSubPool(poolIndex, name, this)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.CustomField = {
    val r = new _root_.de.ust.skill.sir.CustomField(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.CustomField(i + 1)
        i += 1
      }
    }
  }

  def make(language : java.lang.String = null, options : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CustomFieldOption] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CustomFieldOption](), typename : java.lang.String = null, comment : _root_.de.ust.skill.sir.Comment = null, name : _root_.de.ust.skill.sir.Identifier = null, `type` : _root_.de.ust.skill.sir.Type = null, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint](), restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]()) = {
    val r = new _root_.de.ust.skill.sir.CustomField(-1 - newObjects.size, language : java.lang.String, options : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CustomFieldOption], typename : java.lang.String, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, `type` : _root_.de.ust.skill.sir.Type, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction])
    newObjects.append(r)
    r
  }
}

final class CustomFieldSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.CustomField.UnknownSubType <: _root_.de.ust.skill.sir.CustomField, _root_.de.ust.skill.sir.FieldLike])
    extends SubPool[_root_.de.ust.skill.sir.CustomField.UnknownSubType, _root_.de.ust.skill.sir.FieldLike](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.CustomField.UnknownSubType] = classOf[_root_.de.ust.skill.sir.CustomField.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new CustomFieldSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.CustomField.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.CustomField.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.CustomField.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
