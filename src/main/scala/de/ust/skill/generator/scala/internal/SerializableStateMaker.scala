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

/**
 * This class is used to handle objects in a serializable state.
 *
 * @author Timm Felden
 */
final class SerializableState extends SkillState {
  import SerializableState._

  /**
   * path of the file, the serializable state has been created from; this is required for lazy evaluation and appending
   *
   * null iff the state has not been created from a file
   */
  private[internal] var fromReader: ByteReader = null

  private[internal] var pools = new HashMap[String, AbstractPool]
  @inline def knownPools = pools.values.collect({ case p: KnownPool[_, _] ⇒ p });

  private[internal] val strings = new StringPool

  /**
   * Creates a new SKilL file at target.
   */
  def write(target: Path) {
    import SerializationFunctions._

    val file = Files.newByteChannel(target,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING).asInstanceOf[FileChannel]

    // collect all known objects
    strings.prepareSerialization(this)
    val ws = new WriteState(this)

    // prepare pools, i.e. ensure that all objects get IDs which can be turned into logic pointers
    knownPools.foreach(_.prepareSerialization(this))

    // write string pool
    strings.write(file, this)

    // write count of the type block
    file.write(ByteBuffer.wrap(v64(pools.size)))

    // write fields back to their buffers
    val out = new ByteArrayOutputStream

    // write header
    knownPools.foreach(_.write(file, out, ws))

    // write data
    file.write(ByteBuffer.wrap(out.toByteArray()))

    // done:)
    file.close()
  }

  /**
   * retrieves a string from the known strings; this can cause disk access, due to the lazy nature of the implementation
   *
   * @throws ArrayOutOfBoundsException if index is not valid
   */
  def getString(index: Long): String = {
    if (0 == index)
      return null

    strings.idMap.get(index) match {
      case Some(s) ⇒ s
      case None ⇒ {
        if (index > strings.stringPositions.size)
          InvalidPoolIndex(index, strings.stringPositions.size, "string")

        val off = strings.stringPositions(index)
        fromReader.push(off._1)
        var chars = fromReader.bytes(off._2)
        fromReader.pop

        val result = new String(chars, "UTF-8")
        strings.idMap.put(index, result)
        result
      }
    }
  }

  /**
   * adds a string to the state
   */
  def addString(string: String) {
    strings.newStrings += string
  }
""")

    //access to declared types
    IR.foreach({ t ⇒
      val name = t.getName()
      val Name = t.getCapitalName()
      val sName = t.getSkillName()
      val tName = packagePrefix + name

      val addArgs = t.getAllFields().filter { f ⇒ !f.isConstant && !f.isIgnored }.map({
        f ⇒ s"${f.getName().capitalize}: ${mapType(f.getType())}"
      }).mkString(", ")

      out.write(s"""
  /**
   * returns a $name iterator
   */
  def get${Name}s(): Iterator[$tName] = pools("$sName").asInstanceOf[${Name}StoragePool].iterator

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
              throw TypeMissmatchError(t, f.toString, n, t.name)
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

  /**
   * prints some debug information onto stdout
   */
  def dumpDebugInfo = {
    println("DEBUG INFO START")
    println(s"StringPool ($${strings.size}):")
    for (i ← 1 to strings.stringPositions.size) {
      if (!strings.idMap.contains(i))
        print("lazy ⇒ ")
      println(getString(i))
    }
    strings.newStrings.foreach(println(_))
    println("")
    println("ReflectionPool:")
    pools.values.foreach(s ⇒ println(s.userType.getDeclaration))
    println("")
    println("StoragePools:")
    knownPools.foreach({ s ⇒
      println(s.name+": "+s.dynamicSize);
      println(s.iterator.map(_.asInstanceOf[KnownType].prettyString).mkString("  ", "\\n  ", "\\n"))
    })
    println("")

    println("DEBUG INFO END")
  }
}

object SerializableState {

  /**
   * Creates a new and empty serializable state.
   */
  def create(): SerializableState = {
    val result = new SerializableState;
    result.finishInitialization
    result
  }

  /**
   * Reads a skill file and turns it into a serializable state.
   */
  def read(target: Path): SerializableState = FileParser.read(target)
}
""")

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
