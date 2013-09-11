package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.Declaration

/**
 * Creates storage pools for declared types.
 *
 * TODO parsing works only if there is just one field
 * @author Timm Felden
 */
trait DeclaredPoolsMaker extends GeneralOutputMaker {
  override def make {
    super.make
    IR.foreach({ d ⇒
      makePool(open("internal/pool/"+d.getName()+"StoragePool.scala"), d)
    })
  }

  private def makePool(out: PrintWriter, d: Declaration) {
    val name = d.getName()
    val sName = name.toLowerCase()
    val fields = d.getAllFields().toList

    // head
    out.write(s"""package ${packagePrefix}internal.pool

import java.io.ByteArrayOutputStream
import scala.collection.mutable.ArrayBuffer

import ${packagePrefix}internal.types._
import ${packagePrefix}internal._
import ${packagePrefix}internal.parsers.FieldParser
import ${packagePrefix}internal.SerializableState.v64

final class ${name}StoragePool(userType: UserType, storedSize: Long, σ: SerializableState)
    extends StoragePool("$sName", userType, None, storedSize, 0) {

  import SerializableState.v64
  override type T = $name

  // parse required fields and make object
  {
    val fieldParser = new FieldParser(σ)
""")

    // parse known fields
    fields.foreach({ f ⇒
      val name = f.getName()
      val fd = name+"FieldData"
      val scalaType = _T(f.getType())

      out.write(s"""
    // ${f.getType().getTypeName()} $name
    var $fd = new ArrayBuffer[$scalaType]
    userType.fields.filter({ f ⇒ "${f.getCanonicalName()}".equals(f.name) }).foreach(_ match {
      // correct field type
      case f if f.t${
        // TODO
        ".isInstanceOf[V64Info]"
      } ⇒
        $fd ++= fieldParser.readField(storedSize, f.t, f.dataChunks).asInstanceOf[List[$scalaType]]

      // incompatible field type
      case f ⇒ TypeMissmatchError(f.t, "${f.getType().getTypeName().toLowerCase()}", "$name")
    })
    if ($fd.isEmpty)
      $fd ++= new Array[$scalaType](storedSize.toInt)
""")
    })

    // make objects
    out.write(s"""
    // make objects
    for (i ← (0 until storedSize.toInt)) {
      data += new T(""")
    out.write(fields.map({ f ⇒ f.getName()+"FieldData(i)" }).mkString("", ", ", ")\n    }\n  }\n"))

    // add new instances
    out.write(s"""
  private[internal] def add${name.capitalize}(obj: ${packagePrefix}internal.types.$name): ${packagePrefix}$name = {
    dirty = true

    data += obj
    return obj
  }
""")

    // write field data
    out.write(s"""
  override def writeFieldData(out: ByteArrayOutputStream, σ: SerializableState) {

    userType.fields.foreach({ f ⇒
      f.name match {""")

    fields.foreach({ f ⇒
      val name = f.getName()
      //TODO write(v64 ... this has to be replaced by something more generic and faster which takes f.t and an accessor to f.data
      out.write(s"""
        case "${f.getCanonicalName()}" ⇒ {
          data.foreach { o ⇒ /** TODO write o.$name */ }
          endOffsets.put(f, out.size())
        }
""")
    })
    out.write(s"""
        case _ ⇒ writeFieldData(out, σ, f)
      }
    })
  }
""")

    out.write("}")
    out.close()
  }

}