/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal

import scala.collection.JavaConversions._
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.Type
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.ListType

trait SerializableStateMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/SerializableState.scala")

    //package & imports
    out.write(s"""package ${packagePrefix}internal

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import ${packagePrefix}api._
import ${packagePrefix}internal.parsers._
import ${packagePrefix}internal.pool._
import ${packagePrefix}internal.types._
""")

    // first part: exceptions, internal structure, file writing
    copyFromTemplate(out, "SerializableState.scala.part1.template")

    //access to declared types
    IR.foreach({ t ⇒
      val name = t.getName()
      val Name = t.getCapitalName()
      val sName = t.getSkillName()
      val tName = packagePrefix + name

      val addArgs = t.getAllFields().filter(!_.isConstant).map({
        f ⇒ s"${f.getName().capitalize}: ${mapType(f.getType())}"
      }).mkString(", ")

      out.write(s"""
  /**
   * returns a $name iterator
   */
  def get${Name}s(): Iterator[$tName] = new PoolIterator[$tName](pools("$sName").asInstanceOf[${Name}StoragePool])

  /**
   * adds a new $name to the $name pool
   */
  def add$Name($addArgs) = pools("$sName").asInstanceOf[${Name}StoragePool].add$Name(new _root_.${packagePrefix}internal.types.$name($addArgs))
""")
    })

    // state initialization
    out.write(s"""
  /**
   * ensures that all declared types have pools
   */
  def finishInitialization {
    // create a user type map containing all known user types
    var index = pools.size
    def nextIndex: Int = { val rval = index; index += 1; rval }
    def mkUserType(name: String) = new UserType(nextIndex, name, name match {
${
      IR.map { d ⇒
        s"""      case "${d.getSkillName}" ⇒ ${
          if (null == d.getSuperType()) "None" else s"""Some("${d.getSuperType.getSkillName}")"""
        }"""
      }.mkString("\n")
    }
    })
    val userTypes = Map(
${
      IR.map { d ⇒
        val name = d.getSkillName
        s"""      "$name" -> (if (pools.contains("$name")) pools("$name").userType else mkUserType("$name"))"""
      }.mkString(",\n")
    }
    )

    // create fields
    val requiredFields = Map(
${
      IR.map { d ⇒
        val name = d.getSkillName
        s"""      "$name" -> HashMap(${
          d.getFields().map { f ⇒
            val fName = f.getSkillName
            s"""        "$fName" -> new FieldDeclaration(${fieldType(f.getType)}, "$fName", 0)"""
          }.mkString("\n", ",\n", "\n      ")
        })"""
      }.mkString(",\n")
    }
    )

    // add required fields
    userTypes.values.foreach { t ⇒
      requiredFields(t.name).foreach {
        case (n, f) ⇒
          if (t.fields.contains(n)) {
            if (t.fields(n) != f)
              TypeMissmatchError(t, f.toString, n, t.name)
          } else {
            t.fields.put(n, f)
          }
      }
    }

    // create pool in pre-order
${
      IR.map { d ⇒
        val name = d.getSkillName
        s"""    if (!pools.contains("$name")) pools.put("$name", new ${d.getCapitalName}StoragePool(userTypes("$name"), this, 0))"""
      }.mkString("\n")
    }

    userTypes.keys.foreach { t ⇒
      requiredFields(t).keys.foreach(addString(_))
      addString(t)
    }
  }

""")

    // second part: debug stuff; reading of files
    copyFromTemplate(out, "SerializableState.scala.part2.template")

    out.close()
  }

  private def fieldType(t: Type): String = t match {
    case t: Declaration             ⇒ s"""userTypes("${t.getSkillName}")"""

    case t: GroundType              ⇒ t.getSkillName.capitalize+"Info"

    case t: ConstantLengthArrayType ⇒ s"new ConstantLengthArrayInfo(${t.getLength}, ${fieldType(t.getBaseType)})"
    case t: VariableLengthArrayType ⇒ s"new VariableLengthArrayInfo(${fieldType(t.getBaseType)})"
    case t: ListType                ⇒ s"new ListInfo(${fieldType(t.getBaseType)})"
    case t: SetType                 ⇒ s"new SetInfo(${fieldType(t.getBaseType)})"
    case t: MapType                 ⇒ s"new MapInfo(${t.getBaseTypes.map(fieldType(_)).mkString("List(", ",", ")")})"
  }
}
