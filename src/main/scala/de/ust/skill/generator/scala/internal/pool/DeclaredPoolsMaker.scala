package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.ContainerType
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
  protected def checkType(f: Field) = f.getType() match {
    case t: GroundType ⇒ t.getSkillName() match {
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
    // compound types use the string representation to check the type; note that this depends on IR.toString-methods
    case t: ContainerType ⇒ s"""f.t.toString.equals("$t")"""

    case t: Declaration   ⇒ s"""f.t.isInstanceOf[UserType] && f.t.asInstanceOf[UserType].name.equals("${t.getSkillName()}")"""

    // this should be unreachable; it might be reachable if IR changed
    case t                ⇒ throw new Error(s"not yet implemented: ${t.getName()}")
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
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import scala.collection.mutable.ArrayBuffer

import ${packagePrefix}api._
import ${packagePrefix}internal._
import ${packagePrefix}internal.parsers.FieldParser
import ${packagePrefix}internal.types._

final class ${name}StoragePool(userType: UserType, σ: SerializableState, blockCount: Int)
    extends ${
      d.getSuperType() match {
        case null ⇒ s"""BasePool[_root_.$packagePrefix$name](userType.ensuring(_.name.equals("$sName")), σ, blockCount)"""
        case s ⇒ {
          val base = s"_root_.$packagePrefix${d.getBaseType().getName()}"
          val superName = d.getSuperType().getSkillName()
          val superType = d.getSuperType().getName()
          s"""SubPool[_root_.$packagePrefix$name, $base](
      userType,
      σ.pools("$superName").asInstanceOf[KnownPool[_root_.$packagePrefix$superType, $base]],
      σ,
      blockCount
    )"""
        }
      }
    } {

  @inline override def newInstance = new _root_.${packagePrefix}internal.types.$name

  // set eager fields of data instances
  override def readFields(fieldParser: FieldParser) {
    subPools.collect { case p: KnownPool[_, _] ⇒ p }.foreach(_.readFields(fieldParser))
""")

    // parse known fields
    fields.foreach({ f ⇒
      val name = f.getName()
      if (f.isConstant) {
        // constant fields are not directly deserialized, but they need to be checked for the right value
        out.write(s"""
    // ${f.getType().getSkillName()} $name
    userType.fields.filter({ f ⇒ "${f.getSkillName()}".equals(f.name) }).foreach(_ match {
      // correct field type
      case f if ${checkType(f)} ⇒
        if(f.t.asInstanceOf[ConstantIntegerInfo[_]].value != ${f.constantValue})
          throw new ParseException("Constant value differed.")

      // incompatible field type
      case f ⇒ TypeMissmatchError(f.t, "${f.getType().getSkillName()}", "$name")
    })
""")

      } else if (f.isAuto) {
        // auto fields must not be part of the serialized data
        out.write(s"""
    // auto ${f.getType().getSkillName()} $name
    if(!userType.fields.filter({ f ⇒ "${f.getSkillName()}".equals(f.name) }).isEmpty)
      ParseException("Found field data for auto field ${d.getName()}.$name")
""")

      } else {
        // the ordinary field case
        val scalaType = mapType(f.getType())

        out.write(s"""
    // ${f.getType().getSkillName()} $name
    {
      var fieldData = new ArrayBuffer[$scalaType]
      userType.fields.filter({ f ⇒ "${f.getSkillName()}".equals(f.name) }).foreach(_ match {
        // correct field type
        case f if ${checkType(f)} ⇒
          fieldData ++= fieldParser.readField(userType.instanceCount, f.t, f.dataChunks).asInstanceOf[List[$scalaType]]

        // incompatible field type
        case f ⇒ TypeMissmatchError(f.t, "${f.getType().getSkillName()}", "$name")
      })

      // map field data to instances
      var off = 0
      σ.get${d.getName().capitalize}s.foreach { o ⇒ o.set${f.getName().capitalize}(fieldData(off)); off += 1 }
    }
""")
      }
    })

    // note: the add method will get more complex as soon as restrictions are added, e.g. in the context of @unique
    out.write(s"""  }

  private[internal] def add$name(obj: ${packagePrefix}internal.types.$name): $packagePrefix$name = {
    newObjects.append(obj);
    obj
  }

  override def prepareSerialization(σ: SerializableState) {

  }
""")

    // write field data
    out.write(s"""
  override def write(head: FileChannel, out: ByteArrayOutputStream, σ: SerializableState) {
        // TODO
  }
""")

    out.write("}\n")
    out.close()
  }

}
