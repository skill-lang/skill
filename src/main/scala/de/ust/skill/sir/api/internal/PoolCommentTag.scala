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

final class CommentTagPool(poolIndex : Int)
    extends BasePool[_root_.de.ust.skill.sir.CommentTag](
      poolIndex,
      "commenttag"
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.CommentTag] = classOf[_root_.de.ust.skill.sir.CommentTag]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.CommentTag] = {
    val f = (name match {
      case "name" ⇒ new KnownField_CommentTag_name(ID, this, t.asInstanceOf[FieldType[java.lang.String]])
      case "text" ⇒ new KnownField_CommentTag_text(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[java.lang.String]]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.CommentTag]]

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
    val Clsname = classOf[KnownField_CommentTag_name]
    val Clstext = classOf[KnownField_CommentTag_text]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.CommentTag]]](Clsname,Clstext)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clsname))
        dataFields += new KnownField_CommentTag_name(dataFields.size + 1, this, state.String)
    if(fields.contains(Clstext))
        dataFields += new KnownField_CommentTag_text(dataFields.size + 1, this, VariableLengthArray(state.String))
    // no auto fields


    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new CommentTagSubPool(poolIndex, name, this)

  override def allocateData : Unit = data = new Array[_root_.de.ust.skill.sir.CommentTag](cachedSize)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.CommentTag = {
    val r = new _root_.de.ust.skill.sir.CommentTag(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.CommentTag(i + 1)
        i += 1
      }
    }
  }

  def make(name : java.lang.String = null, text : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String]()) = {
    val r = new _root_.de.ust.skill.sir.CommentTag(-1 - newObjects.size, name : java.lang.String, text : scala.collection.mutable.ArrayBuffer[java.lang.String])
    newObjects.append(r)
    r
  }
}

final class CommentTagSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.CommentTag.UnknownSubType <: _root_.de.ust.skill.sir.CommentTag, _root_.de.ust.skill.sir.CommentTag])
    extends SubPool[_root_.de.ust.skill.sir.CommentTag.UnknownSubType, _root_.de.ust.skill.sir.CommentTag](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.CommentTag.UnknownSubType] = classOf[_root_.de.ust.skill.sir.CommentTag.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new CommentTagSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.CommentTag.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.CommentTag.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.CommentTag.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
