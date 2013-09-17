package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType

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

  /**
   * This method creates a type check for deserialization.
   */
  def checkType(f: Field) = f.getType() match {
    case t: GroundType ⇒ t.getTypeName() match {
      case "annotation" ⇒ "f.t.isInstanceOf[AnnotationInfo]"
      case "bool"       ⇒ "f.t.isInstanceOf[BoolInfo]"
      case "i8"         ⇒ "f.t.isInstanceOf[I8Info]"
      case "i16"        ⇒ "f.t.isInstanceOf[I16Info]"
      case "i32"        ⇒ "f.t.isInstanceOf[I32Info]"
      case "i64"        ⇒ "f.t.isInstanceOf[I64Info]"
      case "v64"        ⇒ "f.t.isInstanceOf[V64Info]"
      case "string"     ⇒ "f.t.isInstanceOf[StringInfo]"
      case s            ⇒ throw new Error(s"not yet implemented: $s")
    }
    // TODO maps & co

    case t: Declaration ⇒ s"""f.t.isInstanceOf[UserType] && f.t.asInstanceOf[UserType].name.equals("${t.getTypeName().toLowerCase()}")"""
    case t              ⇒ throw new Error(s"not yet implemented: ${t.getTypeName()}")
  }

  /**
   * Make a pool for d.
   */
  private def makePool(out: PrintWriter, d: Declaration) {
    val name = d.getName()
    val sName = name.toLowerCase()
    val fields = d.getFields().toList

    // head
    out.write(s"""package ${packagePrefix}internal.pool

import java.io.ByteArrayOutputStream

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import ${packagePrefix}internal._
import ${packagePrefix}internal.parsers.FieldParser
import ${packagePrefix}internal.SerializableState.v64
import ${packagePrefix}internal.types._

final class ${name}StoragePool(userType: UserType, σ: SerializableState, blockCount: Int)
    extends StoragePool(userType.ensuring(_.name.equals("$sName")), ${
      d.getSuperType() match {
        case null ⇒ "None";
        case s    ⇒ s"""σ.pools.get("${s.getName().toLowerCase()}")"""
      }
    }, blockCount)
    with KnownPool {

  import SerializableState.v64
  override type T = _root_.$packagePrefix$name

  /**
   * the base type data store
   */
  var data = ${
      d.getSuperType() match {
        case null ⇒ "new Array[T](userType.instanceCount.toInt)"
        case _    ⇒ s"basePool.asInstanceOf[${d.getBaseType().getName()}StoragePool].data"
      }
    }

  override def constructPool() {
    // construct data in a bottom up order
    subPools.filter(_.isInstanceOf[KnownPool]).foreach(_.asInstanceOf[KnownPool].constructPool)
    userType.blockInfos.values.foreach({ b ⇒
      for (i ← b.bpsi - 1 until b.bpsi + b.count - 1)
        if (null == data(i.toInt))
          data(i.toInt) = new _root_.${packagePrefix}internal.types.$name
    })
${
      if (null == d.getSuperType()) """
    // parse fields; note that this will set fields of lower types first
    readFields(new FieldParser(σ))
"""
      else ""
    }  }

  // set eager fields of data instances
  override def readFields(fieldParser: FieldParser) {
    subPools.filter(_.isInstanceOf[KnownPool]).foreach(_.asInstanceOf[KnownPool].readFields(fieldParser))
""")

    // parse known fields
    fields.foreach({ f ⇒
      val name = f.getName()
      val scalaType = _T(f.getType())

      out.write(s"""
    // ${f.getType().getTypeName()} $name
    {
      var fieldData = new ArrayBuffer[$scalaType]
      userType.fields.filter({ f ⇒ "${f.getCanonicalName()}".equals(f.name) }).foreach(_ match {
        // correct field type
        case f if ${checkType(f)} ⇒
          fieldData ++= fieldParser.readField(userType.instanceCount, f.t, f.dataChunks).asInstanceOf[List[$scalaType]]

        // incompatible field type
        case f ⇒ TypeMissmatchError(f.t, "${f.getType().getTypeName().toLowerCase()}", "$name")
      })

      // map field data to instances
      var off = 0
      σ.get${d.getName().capitalize}s.foreach { o ⇒ o.asInstanceOf[T].set${f.getName().capitalize}(fieldData(off)); off += 1 }
    }
""")
    })

    // we are done reading fields; now we can get fields
    out.write(s"""  }

  override def getByID(index: Long): T = try { data(index.toInt - 1).asInstanceOf[T] } catch {
    case e: ClassCastException ⇒ SkillException("tried to access a \\"$name\\" at index "+index+", but it was actually a "+data(index.toInt - 1).getClass().getName(), e)
  }
""")

    // create code to add new instances to the pool
    out.write(s"""
  private[internal] def add$name(obj: ${packagePrefix}internal.types.$name): $packagePrefix$name = {
    // TODO requires a "newObjects" ArrayBuffer
    //    dirty = true
    //
    //    data += obj
    //    return obj
    null
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
        case _ ⇒ // writeFieldData(out, σ, f)
      }
    })
  }
""")

    out.write("}")
    out.close()
  }

}