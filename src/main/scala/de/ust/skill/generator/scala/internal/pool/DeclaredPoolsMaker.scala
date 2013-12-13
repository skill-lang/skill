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
   * Maps types to their "TypeInfo" correspondents.
   */
  private def mapTypeInfo(t: Type): String = t.getSkillName() match {
    case "annotation" ⇒ "AnnotationInfo"
    case "bool"       ⇒ "BoolInfo"
    case "i8"         ⇒ "I8Info"
    case "i16"        ⇒ "I16Info"
    case "i32"        ⇒ "I32Info"
    case "i64"        ⇒ "I64Info"
    case "v64"        ⇒ "V64Info"
    case "f32"        ⇒ "F32Info"
    case "f64"        ⇒ "F64Info"
    case "string"     ⇒ "StringInfo"
    // TODO not that simple in fact
    case s            ⇒ s"""NamedUserType("$s")"""
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
      case "f32"        ⇒ "f.t == F32Info"
      case "f64"        ⇒ "f.t == F64Info"
      case "string"     ⇒ "f.t == StringInfo"
      case s            ⇒ throw new Error(s"not yet implemented: $s")
    }
    // compound types use the string representation to check the type; note that this depends on IR.toString-methods
    case t: ContainerType ⇒ s"""f.t.toString.equals("$t")"""

    case t: Declaration ⇒
      s"""f.t.isInstanceOf[UserType] && f.t.asInstanceOf[UserType].name.equals("${t.getSkillName()}")"""

    // this should be unreachable; it might be reachable if IR changed
    case t ⇒ throw new Error(s"not yet implemented: ${t.getName()}")
  }

  private def makeReadCode(f: Field): String = f.getType match {
    case t: GroundType if t.isInteger() ⇒ s"""val fieldData = fieldParser.read${t.getSkillName().capitalize}s(dynamicSize, f.dataChunks)
          val fields = iterator
          for (i ← 0 until fieldData.size)
            fields.next.${escaped(f.getName)}_=(fieldData(i))"""
    case t ⇒ s"""${makeReadFunctionCall(t)}

          iterator.foreach(_.${escaped(f.getName)} = it.next)"""
  }

  private def makeReadFunctionCall(t: Type): String = t match {
    case t: GroundType ⇒
      s"val it = fieldParser.read${t.getSkillName().capitalize}s(dynamicSize, f.dataChunks)"

    case t: Declaration ⇒
      s"""val d = new Array[_root_.${packagePrefix}${t.getCapitalName()}](dynamicSize.toInt)
          fieldParser.readUserRefs("${t.getSkillName()}", d, f.dataChunks)
          val it = d.iterator"""

    case t: MapType ⇒ s"""val it = fieldParser.readMaps[${mapType(t)}](
            f.t.asInstanceOf[MapInfo],
            dynamicSize,
            f.dataChunks
          )"""

    case t: SingleBaseTypeContainer ⇒
      s"""val it = fieldParser.read${t.getClass().getSimpleName().replace("Type", "")}s[${mapType(t.getBaseType())}](
            f.t.asInstanceOf[${t.getClass().getSimpleName().replace("Type", "Info")}],
            dynamicSize,
            f.dataChunks
          )"""
  }

  /**
   * Make a pool for d.
   */
  private def makePool(out: PrintWriter, d: Declaration) {
    val name = d.getName
    val sName = name.toLowerCase()
    val fields = d.getFields().toList

    ////////////
    // HEADER //
    ////////////

    out.write(s"""package ${packagePrefix}internal.pool

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

import scala.annotation.switch
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import ${packagePrefix}api._
import ${packagePrefix}internal._
import ${packagePrefix}internal.parsers.FieldParser
import ${packagePrefix}internal.types._

final class ${name}StoragePool(state: SerializableState) extends ${
      d.getSuperType() match {
        case null ⇒
          s"""BasePool[_root_.$packagePrefix$name](
  "${d.getSkillName}",
  HashMap[String, FieldDeclaration](
    ${
            (
              for (f ← d.getFields())
                yield s""""${f.getSkillName()}" -> new FieldDeclaration(${mapTypeInfo(f.getType)}, "${f.getSkillName()}", -1)"""
            ).mkString("", ",\n    ", "")
          }
  ),
  Array[_root_.$packagePrefix$name]()
)"""

        case s ⇒ {
          val base = s"_root_.$packagePrefix${d.getBaseType().getName()}"
          s"""SubPool[_root_.$packagePrefix$name, $base](
  "${d.getSkillName}",
  HashMap[String, FieldDeclaration](
  ),
  state.$s
)"""
        }
      }
    } with SkillState.${name}Access {

  @inline override def newInstance = new _root_.${packagePrefix}internal.types.$name
""")

    ///////////////
    // ITERATORS //
    ///////////////

    val applyCallArguments = d.getAllFields().filter { f ⇒ !f.isConstant && !f.isIgnored }.map({
      f ⇒ s"${f.getName().capitalize}: ${mapType(f.getType())}"
    }).mkString(", ")

    out.write(s"""
  override def all = iterator
  override def allInTypeOrder = typeOrderIterator
  override def apply($applyCallArguments) = addDate(new _root_.${packagePrefix}internal.types.$name($applyCallArguments))

  override def iterator = ${
      if (null == d.getSuperType) s"""data.iterator ++ newDynamicInstances"""
      else "blockInfos.foldRight(newDynamicInstances) { (block, iter) ⇒ basePool.data.view(block.bpsi.toInt, (block.bpsi + block.count).toInt).iterator ++ iter }"
    }

  override def typeOrderIterator = subPools.collect {
    // @note: you can ignore the type erasure warning, because the generators invariants guarantee type safety
    case p: KnownPool[_, $name] @unchecked ⇒ p
  }.foldLeft(staticInstances)(_ ++ _.staticInstances)

  override def staticInstances = staticData.iterator ++ newObjects.iterator
  override def newDynamicInstances = subPools.collect {
    // @note: you can ignore the type erasure warning, because the generators invariants guarantee type safety
    case p: KnownPool[_, Date] @unchecked ⇒ p
  }.foldLeft(newObjects.iterator)(_ ++ _.newObjects.iterator)

  /**
   * the number of static instances loaded from the file
   */
  private var staticData = Array[_root_.${packagePrefix}internal.types.$name]();
  /**
   * the static size is thus the number of static instances plus the number of new objects
   */
  override def staticSize: Long = staticData.size + newObjects.length

  /**
   * construct instances of the pool in post-order, i.e. bottom-up
   */
  final override def constructPool() {
    // construct data in a bottom up order
    subPools.collect { case p: KnownPool[_, _] ⇒ p }.foreach(_.constructPool)
    val staticDataConstructor = new ArrayBuffer[_root_.${packagePrefix}internal.types.$name]
    for (b ← blockInfos) {
      val from: Int = b.bpsi.toInt - 1
      val until: Int = b.bpsi.toInt + b.count.toInt - 1
      for (i ← from until until)
        if (null == data(i)) {
          val next = new _root_.${packagePrefix}internal.types.$name
          next.setSkillID(i + 1)
          staticDataConstructor += next
          data(i) = next
        }
    }
    staticData = staticDataConstructor.toArray
  }

  // set eager fields of data instances
  override def readFields(fieldParser: FieldParser) {
    subPools.collect { case p: KnownPool[_, _] ⇒ p }.foreach(_.readFields(fieldParser))

    for ((name, f) ← fields) {
      (name: @switch) match {""")

    // parse known fields
    fields.foreach({ f ⇒
      if (!f.isIgnored()) {
        val name = f.getName()
        if (f.isConstant) {
          // constant fields are not directly deserialized, but they need to be checked for the right value
          out.write(s"""
        // const ${f.getType().getSkillName()} $name
        case "${f.getSkillName()}" ⇒
         if(f.t.asInstanceOf[ConstantIntegerInfo[_]].value != ${f.constantValue})
            throw new SkillException("Constant value differed.")
    })
""")

        } else if (f.isAuto) {
          // auto fields must not be part of the serialized data
          out.write(s"""
        // auto ${f.getType.getSkillName} $name
        case "${f.getSkillName()}" ⇒ if(!f.dateChunks.isEmpty)
          throw new SkillException("Found field data for auto field ${d.getName()}.$name")
""")

        } else {
          // the ordinary field case
          out.write(s"""
        // ${f.getType.getSkillName} $name
        case "${f.getSkillName()}" ⇒ locally {
          ${makeReadCode(f)}
        }
""")
        }
      }
    })

    // note: the add method will get more complex as soon as restrictions are added, e.g. in the context of @unique
    out.write(s"""
        // TODO delegate type error detection to the field parser
        case _ ⇒ // TODO generic fields
      }
    }
  }

  private[internal] def add$name(obj: ${packagePrefix}internal.types.$name): $packagePrefix$name = {
    newObjects.append(obj);
    obj
  }

  override def prepareSerialization(σ: SerializableState) {

  }
""")

    ///////////
    // write //
    ///////////

    def writeField(f: Field): String = f.getType match {
      case t: GroundType ⇒ t.getSkillName match {
        case "annotation" ⇒
          s"""this.foreach { instance ⇒ annotation(instance.${escaped(f.getName)}[SkillType]).foreach(out.write _) }"""

        case "v64" ⇒
          s"""val target = new Array[Byte](9 * outData.size)
          var offset = 0

          val it = outData.iterator
          while (it.hasNext)
            offset += v64(it.next.${escaped(f.getName)}, target, offset)

          out.write(target, 0, offset)"""

        case "i64" ⇒
          s"""val target = ByteBuffer.allocate(8 * size)
          val it = outData.iterator
          while (it.hasNext)
            target.putLong(it.next.${escaped(f.getName)})

          out.write(target.array)"""

        case _ ⇒ s"this.foreach { instance ⇒ out.write(${f.getType().getSkillName()}(instance.${escaped(f.getName)})) }"
      }
      case t: Declaration ⇒
        s"""@inline def putField(i:$packagePrefix${d.getName}) { out.write(v64(i.${escaped(f.getName)}.getSkillID)) }
          ws.foreachOf("${t.getSkillName}", putField)"""

      // TODO implementation for container types
      case t: ContainerType ⇒ "???"

      case _                ⇒ s"this.foreach { instance ⇒ out.write(${f.getType().getSkillName()}(instance.${escaped(f.getName)})) }"
    }

    out.write(s"""
  override def write(head: FileChannel, out: ByteArrayOutputStream, ws: WriteState) {
    import ws._
    import SerializationFunctions._

    @inline def put(b: Array[Byte]) = head.write(ByteBuffer.wrap(b));

    val outData = this

    put(string("$sName"))
    ${
      if (null == d.getSuperType)
        "put(Array[Byte](0))"
      else
        s"""put(string("${d.getSuperType.getSkillName}"))
    put(v64(ws.lbpsiMap("$sName")))"""
    }
    put(v64(outData.size))
    put(v64(0)) // restrictions not implemented yet

    put(v64(fields.size))

    for ((name, f) ← fields) {
      (name: @switch) match {""")

    for (f ← fields) {
      out.write(s"""
        case "${f.getSkillName()}" ⇒ locally {
          put(v64(0)) // field restrictions not implemented yet
          put(v64(${
        f.getType match {
          case t: Declaration ⇒ s"""ws.typeID("${t.getSkillName}")"""
          case _              ⇒ "f.t.typeId"
        }
      }))
          put(string("${f.getSkillName()}"))

          ${writeField(f)}
          put(v64(out.size))
        }""")
    }

    out.write(s"""
      }
    }
  }
""")

    ////////////
    // append //
    ////////////

    out.write(s"""
  override def append(head: FileChannel, out: ByteArrayOutputStream, as: AppendState) {
    import as._
    import SerializationFunctions._

    @inline def put(b: Array[Byte]) = head.write(ByteBuffer.wrap(b));

    val outData = as.d("$sName").asInstanceOf[Iterable[$name]]

    // check the kind of header we have to write
    if (staticSize > newObjects.size) {
      // the type is known, thus we only have to write {name, ?lbpsi, count, fieldCount}
      put(string("$sName"))
      put(v64(outData.size)) // the append state known how many instances we will write

      put(v64(fields.size))

    } else {
      // the type is yet unknown, thus we will write all information
      put(string("$sName"))
      put(Array[Byte](0))
      put(v64(outData.size))
      put(v64(0)) // restrictions not implemented yet

      put(v64(fields.size))
    }

    for ((name, f) ← fields) {
      (name: @switch) match {""")
    fields.foreach({ f ⇒
      val sName = f.getSkillName
      out.write(s"""
        case "$sName" ⇒ locally {
          // TODO append new fields

          // put(v64(0)) // field restrictions not implemented yet
          // put(v64(${
        f.getType match {
          case t: Declaration ⇒ s"""ws.typeID("${t.getSkillName}")"""
          case _              ⇒ "f.t.typeId"
        }
      }))
          // put(string("${f.getSkillName()}"))

          ${writeField(f)}
          put(v64(out.size))
        }""")
    })

    out.write("""
      }
    }
  }
}
""")
    out.close()
  }
}
