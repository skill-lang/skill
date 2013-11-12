/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.ir.ContainerType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.Type
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.ConstantLengthArrayType

/**
 * Creates storage pools for declared types.
 *
 * @author Timm Felden
 */
trait DeclaredPoolsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    IR.foreach({ d ⇒
      makePool(open("internal/pool/"+d.getName()+"StoragePool.scala"), d)
    })
  }

  /**
   * This method creates a type check for deserialization.
   */
  private def checkType(f: Field) = f.getType() match {
    case t: GroundType ⇒ t.getSkillName() match {
      case "annotation" ⇒ "f.t == AnnotationInfo"
      case "bool"       ⇒ "f.t == BoolInfo"
      case "i8"         ⇒ "f.t == I8Info"
      case "i16"        ⇒ "f.t == I16Info"
      case "i32"        ⇒ "f.t == I32Info"
      case "i64"        ⇒ "f.t == I64Info"
      case "v64"        ⇒ "f.t == V64Info"
      case "string"     ⇒ "f.t == StringInfo"
      case s            ⇒ throw new Error(s"not yet implemented: $s")
    }
    // compound types use the string representation to check the type; note that this depends on IR.toString-methods
    case t: ContainerType ⇒ s"""f.t.toString.equals("$t")"""

    case t: Declaration   ⇒ s"""f.t.isInstanceOf[UserType] && f.t.asInstanceOf[UserType].name.equals("${t.getSkillName()}")"""

    // this should be unreachable; it might be reachable if IR changed
    case t                ⇒ throw new Error(s"not yet implemented: ${t.getName()}")
  }

  private def makeReadFunctionCall(t: Type): String = t match {
    case t: GroundType ⇒ s"val it = fieldParser.read${t.getSkillName().capitalize}s(userType.instanceCount, f.dataChunks)"

    case t: Declaration ⇒ s"""val d = new Array[${t.getCapitalName()}](userType.instanceCount.toInt)
        fieldParser.readUserRefs("${t.getSkillName()}", d, f.dataChunks)
        val it = d.iterator"""

    case t: MapType ⇒ s"""val it = fieldParser.readMaps[${mapType(t)}](
          f.t.asInstanceOf[MapInfo],
          userType.instanceCount,
          f.dataChunks
        )"""

    case t: SingleBaseTypeContainer ⇒ s"""val it = fieldParser.read${t.getClass().getSimpleName().replace("Type", "")}s[${mapType(t.getBaseType())}](
          f.t.asInstanceOf[${t.getClass().getSimpleName().replace("Type", "Info")}],
          userType.instanceCount,
          f.dataChunks
        )"""
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

  override def newInstance = new _root_.${packagePrefix}internal.types.$name

  // set eager fields of data instances
  override def readFields(fieldParser: FieldParser) {
    subPools.collect { case p: KnownPool[_, _] ⇒ p }.foreach(_.readFields(fieldParser))
""")

    // parse known fields
    fields.foreach({ f ⇒
      if (!f.isIgnored()) {
        val name = f.getName()
        if (f.isConstant) {
          // constant fields are not directly deserialized, but they need to be checked for the right value
          out.write(s"""
    // ${f.getType().getSkillName()} $name
    userType.fields.get("${f.getSkillName()}").foreach(_ match {
      // correct field type
      case f if ${checkType(f)} ⇒
        if(f.t.asInstanceOf[ConstantIntegerInfo[_]].value != ${f.constantValue})
          throw new SkillException("Constant value differed.")

      // incompatible field type
      case f ⇒ TypeMissmatchError(f.t, "${f.getType().getSkillName()}", "$name", "${d.getName()}StoragePool")
    })
""")

        } else if (f.isAuto) {
          // auto fields must not be part of the serialized data
          out.write(s"""
    // auto ${f.getType().getSkillName()} $name
    if(!userType.fields.get("${f.getSkillName()}").isEmpty)
      throw new SkillException("Found field data for auto field ${d.getName()}.$name")
""")

        } else {
          val t = f.getType()
          // the ordinary field case
          out.write(s"""
    // ${t.getSkillName()} $name
    userType.fields.get("${f.getSkillName()}").foreach(_ match {
      // correct field type
      case f if ${checkType(f)} ⇒ {
        ${makeReadFunctionCall(t)}

        iterator.foreach(_.set${f.getName().capitalize}(it.next))
      }

      // incompatible field type
      case f ⇒ throw TypeMissmatchError(f.t, "${f.getType().getSkillName()}", "$name", "${d.getName()}StoragePool")
    })
""")
        }
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
  override def write(head: FileChannel, out: ByteArrayOutputStream, state: SerializableState, ws: WriteState) {
    val serializationFunction = state.serializationFunction
    import serializationFunction._
    import SerializationFunctions._

    @inline def put(b: Array[Byte]) = head.write(ByteBuffer.wrap(b));

    put(string("$sName"))
    put(v64(0)) // TODO
    put(v64(this.dynamicSize))
    put(v64(0)) // restrictions not implemented yet

    put(v64(userType.fields.size))
""")

    fields.foreach({ f ⇒
      out.write(s"""
    userType.fields.get("${f.getSkillName()}").foreach { f ⇒
      put(v64(0)) // field restrictions not implemented yet
      put(v64(f.t.typeId))
      put(string("${f.getSkillName()}"))

      this.foreach { instance ⇒ ${writeSingleField(f)} }
      put(v64(out.size))
    }
""")
    })

    out.write(s"""
  }
""")

    out.write("}\n")
    out.close()
  }

  def writeSingleField(f: Field): String = f.getType match {
    case t: GroundType if ("annotation" == t.getSkillName()) ⇒ s"""out.write(v64(ws.getByRef(instance.get${f.getName().capitalize}.getClass.getSimpleName.toLowerCase, instance.get${f.getName().capitalize})))"""
    case t: Declaration ⇒ s"""out.write(v64(ws.getByRef("${t.getSkillName}", instance.get${f.getName().capitalize})))"""
    // TODO implementation for container types
    case t: ContainerType ⇒ "???"
    case _ ⇒ s"out.write(${f.getType().getSkillName()}(instance.get${f.getName().capitalize}))"
  }

}
