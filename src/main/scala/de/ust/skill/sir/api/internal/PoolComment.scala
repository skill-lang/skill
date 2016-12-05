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

final class CommentPool(poolIndex : Int)
    extends BasePool[_root_.de.ust.skill.sir.Comment](
      poolIndex,
      "comment"
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.Comment] = classOf[_root_.de.ust.skill.sir.Comment]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.Comment] = {
    val f = (name match {
      case "tags" ⇒ new KnownField_Comment_tags(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CommentTag]]])
      case "text" ⇒ new KnownField_Comment_text(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[java.lang.String]]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.Comment]]

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
    val Clstags = classOf[KnownField_Comment_tags]
    val Clstext = classOf[KnownField_Comment_text]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.Comment]]](Clstags,Clstext)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(Clstags))
        dataFields += new KnownField_Comment_tags(dataFields.size + 1, this, VariableLengthArray(state.CommentTag))
    if(fields.contains(Clstext))
        dataFields += new KnownField_Comment_text(dataFields.size + 1, this, VariableLengthArray(state.String))
    // no auto fields
  

    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new CommentSubPool(poolIndex, name, this)

  override def allocateData : Unit = data = new Array[_root_.de.ust.skill.sir.Comment](cachedSize)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.Comment = {
    val r = new _root_.de.ust.skill.sir.Comment(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.Comment(i + 1)
        i += 1
      }
    }
  }

  def make(tags : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CommentTag] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CommentTag](), text : scala.collection.mutable.ArrayBuffer[java.lang.String] = scala.collection.mutable.ArrayBuffer[java.lang.String]()) = {
    val r = new _root_.de.ust.skill.sir.Comment(-1 - newObjects.size, tags : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CommentTag], text : scala.collection.mutable.ArrayBuffer[java.lang.String])
    newObjects.append(r)
    r
  }
}

final class CommentSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.Comment.UnknownSubType <: _root_.de.ust.skill.sir.Comment, _root_.de.ust.skill.sir.Comment])
    extends SubPool[_root_.de.ust.skill.sir.Comment.UnknownSubType, _root_.de.ust.skill.sir.Comment](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.Comment.UnknownSubType] = classOf[_root_.de.ust.skill.sir.Comment.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new CommentSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.Comment.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.Comment.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.Comment.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
