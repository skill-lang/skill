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

final class ToolPool(poolIndex : Int)
    extends BasePool[_root_.de.ust.skill.sir.Tool](
      poolIndex,
      "tool"
    ) {
  override def getInstanceClass: Class[_root_.de.ust.skill.sir.Tool] = classOf[_root_.de.ust.skill.sir.Tool]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, _root_.de.ust.skill.sir.Tool] = {
    val f = (name match {
      case "buildtargets" ⇒ new KnownField_Tool_buildTargets(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.BuildInformation]]])
      case "customfieldannotations" ⇒ new KnownField_Tool_customFieldAnnotations(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.ToolTypeCustomization]]])
      case "customtypeannotations" ⇒ new KnownField_Tool_customTypeAnnotations(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, _root_.de.ust.skill.sir.ToolTypeCustomization]]])
      case "description" ⇒ new KnownField_Tool_description(ID, this, t.asInstanceOf[FieldType[java.lang.String]])
      case "name" ⇒ new KnownField_Tool_name(ID, this, t.asInstanceOf[FieldType[java.lang.String]])
      case "selectedfields" ⇒ new KnownField_Tool_selectedFields(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, scala.collection.mutable.HashMap[java.lang.String, _root_.de.ust.skill.sir.FieldLike]]]])
      case "selectedusertypes" ⇒ new KnownField_Tool_selectedUserTypes(ID, this, t.asInstanceOf[FieldType[scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.UserdefinedType]]])
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, _root_.de.ust.skill.sir.Tool]]

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
    val ClsbuildTargets = classOf[KnownField_Tool_buildTargets]
    val ClscustomFieldAnnotations = classOf[KnownField_Tool_customFieldAnnotations]
    val ClscustomTypeAnnotations = classOf[KnownField_Tool_customTypeAnnotations]
    val Clsdescription = classOf[KnownField_Tool_description]
    val Clsname = classOf[KnownField_Tool_name]
    val ClsselectedFields = classOf[KnownField_Tool_selectedFields]
    val ClsselectedUserTypes = classOf[KnownField_Tool_selectedUserTypes]

    val fields = HashSet[Class[_ <: FieldDeclaration[_, _root_.de.ust.skill.sir.Tool]]](ClsbuildTargets,ClscustomFieldAnnotations,ClscustomTypeAnnotations,Clsdescription,Clsname,ClsselectedFields,ClsselectedUserTypes)
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
    if(fields.contains(ClsbuildTargets))
        dataFields += new KnownField_Tool_buildTargets(dataFields.size + 1, this, VariableLengthArray(state.BuildInformation))
    if(fields.contains(ClscustomFieldAnnotations))
        dataFields += new KnownField_Tool_customFieldAnnotations(dataFields.size + 1, this, MapType(state.FieldLike, state.ToolTypeCustomization))
    if(fields.contains(ClscustomTypeAnnotations))
        dataFields += new KnownField_Tool_customTypeAnnotations(dataFields.size + 1, this, MapType(state.UserdefinedType, state.ToolTypeCustomization))
    if(fields.contains(Clsdescription))
        dataFields += new KnownField_Tool_description(dataFields.size + 1, this, state.String)
    if(fields.contains(Clsname))
        dataFields += new KnownField_Tool_name(dataFields.size + 1, this, state.String)
    if(fields.contains(ClsselectedFields))
        dataFields += new KnownField_Tool_selectedFields(dataFields.size + 1, this, MapType(state.UserdefinedType, MapType(state.String, state.FieldLike)))
    if(fields.contains(ClsselectedUserTypes))
        dataFields += new KnownField_Tool_selectedUserTypes(dataFields.size + 1, this, SetType(state.UserdefinedType))
    // no auto fields


    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }

  override def makeSubPool(name : String, poolIndex : Int) = new ToolSubPool(poolIndex, name, this)

  override def allocateData : Unit = data = new Array[_root_.de.ust.skill.sir.Tool](cachedSize)
  override def reflectiveAllocateInstance: _root_.de.ust.skill.sir.Tool = {
    val r = new _root_.de.ust.skill.sir.Tool(-1)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new _root_.de.ust.skill.sir.Tool(i + 1)
        i += 1
      }
    }
  }

  def make(buildTargets : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.BuildInformation] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.BuildInformation](), customFieldAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.ToolTypeCustomization] = scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.ToolTypeCustomization](), customTypeAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, _root_.de.ust.skill.sir.ToolTypeCustomization] = scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, _root_.de.ust.skill.sir.ToolTypeCustomization](), description : java.lang.String = null, name : java.lang.String = null, selectedFields : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, scala.collection.mutable.HashMap[java.lang.String, _root_.de.ust.skill.sir.FieldLike]] = scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, scala.collection.mutable.HashMap[java.lang.String, _root_.de.ust.skill.sir.FieldLike]](), selectedUserTypes : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.UserdefinedType] = scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.UserdefinedType]()) = {
    val r = new _root_.de.ust.skill.sir.Tool(-1 - newObjects.size, buildTargets : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.BuildInformation], customFieldAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.ToolTypeCustomization], customTypeAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, _root_.de.ust.skill.sir.ToolTypeCustomization], description : java.lang.String, name : java.lang.String, selectedFields : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, scala.collection.mutable.HashMap[java.lang.String, _root_.de.ust.skill.sir.FieldLike]], selectedUserTypes : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.UserdefinedType])
    newObjects.append(r)
    r
  }
}

final class ToolSubPool(poolIndex : Int, name : String, superPool : StoragePool[_ >: _root_.de.ust.skill.sir.Tool.UnknownSubType <: _root_.de.ust.skill.sir.Tool, _root_.de.ust.skill.sir.Tool])
    extends SubPool[_root_.de.ust.skill.sir.Tool.UnknownSubType, _root_.de.ust.skill.sir.Tool](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[_root_.de.ust.skill.sir.Tool.UnknownSubType] = classOf[_root_.de.ust.skill.sir.Tool.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new ToolSubPool(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new _root_.de.ust.skill.sir.Tool.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : _root_.de.ust.skill.sir.Tool.UnknownSubType = {
      val r = new _root_.de.ust.skill.sir.Tool.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
