/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api.internal

import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.View
import de.ust.skill.ir.restriction.SingletonRestriction
import de.ust.skill.ir.ReferenceType
import de.ust.skill.ir.ContainerType
import de.ust.skill.ir.Type
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.UserType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.Field
import de.ust.skill.ir.InterfaceType

trait PoolsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    // reflection has to know projected definitions
    val flatIR = this.types.removeSpecialDeclarations.getUsertypes

    for (t ← IR) {
      val typeName = "_root_." + packagePrefix + name(t)
      val isSingleton = !t.getRestrictions.collect { case r : SingletonRestriction ⇒ r }.isEmpty

      // reference to state if required
      val stateRef = if (t.hasDistributedField()) s", basePool.owner.asInstanceOf[${packagePrefix}api.SkillFile]"
      else ""

      // find all fields that belong to the projected version, but use the unprojected variant
      val flatIRFieldNames = flatIR.find(_.getName == t.getName).get.getFields.map(_.getSkillName).toSet
      val fields = t.getAllFields.filter(f ⇒ flatIRFieldNames.contains(f.getSkillName))
      val projectedField = flatIR.find(_.getName == t.getName).get.getFields.map {
        case f ⇒ fields.find(_.getSkillName.equals(f.getSkillName)).get -> f
      }.toMap

      val out = open(s"api/internal/Pool${t.getName.capital}.scala")
      //package
      out.write(s"""package ${packagePrefix}api.internal

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

import _root_.${packagePrefix}api._

final class ${storagePool(t)}(poolIndex : Int${
        if (t.getSuperType == null) ""
        else s",\nsuperPool: ${storagePool(t.getSuperType)}"
      })
    extends ${
        if (t.getSuperType == null) s"BasePool[$typeName]"
        else s"SubPool[$typeName, ${packagePrefix}${t.getBaseType.getName.capital}]"
      }(
      poolIndex,
      "${t.getSkillName}"${
        if (t.getSuperType == null) ""
        else ",\nsuperPool"
      }
    )${
        if (isSingleton) s" with SingletonStoragePool[$typeName, ${packagePrefix}${t.getBaseType.getName.capital}]"
        else ""
      } {
  override def getInstanceClass: Class[$typeName] = classOf[$typeName]

  override def addField[T : Manifest](ID : Int, t : FieldType[T], name : String,
                           restrictions : HashSet[FieldRestriction]) : FieldDeclaration[T, $typeName] = {
    val f = (name match {${
        (for (f ← fields)
          yield s"""
      case "${f.getSkillName}" ⇒ new ${knownField(projectedField(f))}(${
          if (f.isAuto()) ""
          else "ID, "
        }this${
          if (f.getType.isInstanceOf[ReferenceType] || f.getType.isInstanceOf[ContainerType])
            s""", t.asInstanceOf[FieldType[${mapType(f.getType)}]]"""
          else ""
        })"""
        ).mkString("")
      }
      case _      ⇒ return super.addField(ID, t, name, restrictions)
    }).asInstanceOf[FieldDeclaration[T, $typeName]]

    //check type
    if (t != f.t)
      throw new TypeMissmatchError(t, f.t.toString, f.name, name)

    restrictions.foreach(f.addRestriction(_))
    dataFields += f
    return f
  }
  override def ensureKnownFields(st : SkillState) {${
        if (fields.isEmpty) ""
        else """
    val state = st.asInstanceOf[SkillFile]"""
      }
    ${
        val (afs, dfs) = fields.partition(_.isAuto)
        (if (dfs.isEmpty) "// no data fields\n"
        else s"""// data fields
    ${
          (for (f ← dfs) yield s"val ${clsName(f)} = classOf[${knownField(projectedField(f))}]").mkString("", "\n    ", "\n")
        }
    val fields = HashSet[Class[_ <: FieldDeclaration[_, ${mapType(t)}]]](${
          (for (f ← dfs) yield s"${clsName(f)}").mkString(",")
        })
    var dfi = dataFields.size
    while (dfi != 0) {
      dfi -= 1
      fields.remove(dataFields(dfi).getClass)
    }
${
          (for (f ← dfs)
            yield s"""    if(fields.contains(${clsName(f)}))
        dataFields += new ${knownField(projectedField(f))}(dataFields.size + 1, this, ${mapFieldDefinition(f.getType)})"""
          ).mkString("\n")
        }
""") + (
          if (afs.isEmpty) "    // no auto fields\n  "
          else s"""    // auto fields
    autoFields.sizeHint(${afs.size})${
            afs.map { f ⇒
              s"""
    autoFields += new ${knownField(projectedField(f))}(this, ${mapFieldDefinition(f.getType)})${
                if (f.isDistributed) s".ensuring { f ⇒ ${knownField(f)} = f; true }"
                else ""
              }"""
            }.mkString
          }
  """)
      }

    for(f <- dataFields ++ autoFields)
      f.createKnownRestrictions
  }
${
        // access to distributed fields
        (for (f ← t.getFields if f.isDistributed())
          yield s"\n  var ${knownField(f)} : ${knownField(f)} = _\n").mkString
      }
  override def makeSubPool(name : String, poolIndex : Int) = ${
        if (isSingleton) s"""throw new NoSuchMethodError("${t.getName.capital} is a Singleton and can therefore not have any subtypes.")"""
        else s"new ${subPool(t)}(poolIndex, name, this)"
      }${
        if (null == t.getSuperType) s"""

  override def allocateData : Unit = data = new Array[$typeName](cachedSize)"""
        else""
      }${
        if (isSingleton) s"""

  override def reflectiveAllocateInstance : $typeName = {
    if (null != this.data && 0 != this.staticDataInstances) {
      val r = staticInstances.next
      if (null != r) r
      else new $typeName(-1$stateRef)
    } else if (!newObjects.isEmpty) {
      newObjects.head
    } else {
      val r = new $typeName(-1$stateRef)
      this.newObjects.append(r)
      r
    }
  }
"""
        else s"""
  override def reflectiveAllocateInstance: $typeName = {
    val r = new $typeName(-1$stateRef)
    this.newObjects.append(r)
    r
  }

  override def allocateInstances {
    for (b ← blocks.par) {
      var i : SkillID = b.bpo
      val last = i + b.staticCount
      while (i < last) {
        data(i) = new $typeName(i + 1$stateRef)
        i += 1
      }
    }
  }

  def make(${makeConstructorArguments(t)}) = {
    val r = new $typeName(-1 - newObjects.size$stateRef${appendConstructorArguments(t)})
    newObjects.append(r)
    r
  }"""
      }
}
""")

      if (!isSingleton) {
        // create a sub pool
        out.write(s"""
final class ${subPool(t)}(poolIndex : Int, name : String, superPool : StoragePool[_ >: $typeName.UnknownSubType <: $typeName, _root_.${packagePrefix}${t.getBaseType.getName.capital}])
    extends SubPool[$typeName.UnknownSubType, _root_.${packagePrefix}${name(t.getBaseType)}](
      poolIndex,
      name,
      superPool
    ) {
  override def getInstanceClass : Class[$typeName.UnknownSubType] = classOf[$typeName.UnknownSubType]

  override def makeSubPool(name : String, poolIndex : Int) = new ${subPool(t)}(poolIndex, name, this)

  override def ensureKnownFields(st : SkillState) {}

  override def allocateInstances {
      for (b ← blocks.par) {
        var i : SkillID = b.bpo
        val last = i + b.staticCount
        while (i < last) {
          data(i) = new $typeName.UnknownSubType(i + 1, this)
          i += 1
        }
      }
    }

    def reflectiveAllocateInstance : $typeName.UnknownSubType = {
      val r = new $typeName.UnknownSubType(-1, this)
      this.newObjects.append(r)
      r
    }
}
""")
      }
      out.close()
    }
  }

  /**
   * escaped name for field classes
   */
  private final def clsName(f : Field) : String = escaped("Cls" + f.getName.camel)

  protected def mapFieldDefinition(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "string"     ⇒ "state.String"
      case "annotation" ⇒ "state.AnnotationType"
      case "bool"       ⇒ "BoolType"
      case n            ⇒ n.capitalize
    }
    case t : UserType                ⇒ s"state.${name(t)}"
    case t : InterfaceType           ⇒ s"state.${name(t)}"

    case t : ConstantLengthArrayType ⇒ s"ConstantLengthArray(${t.getLength}, ${mapFieldDefinition(t.getBaseType)})"
    case t : VariableLengthArrayType ⇒ s"VariableLengthArray(${mapFieldDefinition(t.getBaseType)})"
    case t : ListType                ⇒ s"ListType(${mapFieldDefinition(t.getBaseType)})"
    case t : SetType                 ⇒ s"SetType(${mapFieldDefinition(t.getBaseType)})"
    case t : MapType ⇒ t.getBaseTypes.init.foldRight(mapFieldDefinition(t.getBaseTypes.last)) {
      case (t, str) ⇒ s"MapType(${mapFieldDefinition(t)}, $str)"
    }
    case _ ⇒ "???"
  }
}
