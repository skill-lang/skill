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
import de.ust.skill.ir.restriction.MonotoneRestriction
import de.ust.skill.ir.restriction.SingletonRestriction

trait SerializableStateMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/SerializableState.scala")

    //package & imports
    out.write(s"""package ${packagePrefix}internal

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import ${packagePrefix}api._
import ${packagePrefix}internal.SerializationFunctions.v64
import ${packagePrefix}internal.parsers._
import ${packagePrefix}internal.pool._

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
  private[internal] var fromPath: Path = null
  /**
   * reader for the input file, which is used for lazy evaluation and error reporting
   */
  private[internal] var fromReader: ByteReader = null

  private[internal] var pools = new HashMap[String, AbstractPool]
  @inline def knownPools = pools.values.collect({ case p: KnownPool[_, _] ⇒ p }).toArray

  override val String = new StringPool(this)
  ${
      (
        for (d ← IR; name = d.getName)
          yield s"""override val $name = new ${name}StoragePool(this)
  pools.put("${d.getSkillName}", $name)"""
      ).mkString("", "\n  ", "")
    }

  /**
   * Creates a new SKilL file at target.
   */
  override def write(target: Path) {
    import SerializationFunctions._

    // create the output channel
    val file = Files.newByteChannel(target,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING).asInstanceOf[FileChannel]

    // update from path, if it did not exist before allowing for future append operations
    if (null == fromPath) try {
      fromPath = target
      fromReader = new ByteReader(Files.newByteChannel(fromPath).asInstanceOf[FileChannel])
    } catch {
      case e: IOException ⇒ // we don't care, because this means we do not have right permissions Oo
    }

    val ws = new WriteState(this)
    // write string pool
    String.prepareAndWrite(file, ws)

    // collect all known objects
    // prepare pools, i.e. ensure that all objects get IDs which can be turned into logic pointers
    knownPools.foreach(_.prepareSerialization(this))

    // write count of the type block
    file.write(ByteBuffer.wrap(v64(pools.size)))

    // write fields back to their buffers
    val out = new ByteArrayOutputStream

    // write header
    def writeSubPools(p: KnownPool[_, _]) {
      p.write(file, out, ws)
      for (p ← p.getSubPools)
        if (p.isInstanceOf[KnownPool[_, _]])
          writeSubPools(p.asInstanceOf[KnownPool[_, _]])
    }
    for (p ← knownPools)
      if (p.isInstanceOf[BasePool[_]])
        writeSubPools(p)

    // write data
    file.write(ByteBuffer.wrap(out.toByteArray()))

    // done:)
    file.close()

    // reset skill IDs
    knownPools.foreach {
      case p: BasePool[_] ⇒ p.restoreIndices
      case _              ⇒
    }
  }

  /**
   * Appends to the SKilL file we read.
   */
  override def append = append(fromPath.ensuring(_ != null))
  /**
   * Appends to an existing SKilL file.
   */
  override def append(target: Path) {
    require(canAppend)
    import SerializationFunctions._

    val file = Files.newByteChannel(target,
      StandardOpenOption.APPEND,
      StandardOpenOption.WRITE).asInstanceOf[FileChannel]

    assert(target == fromPath, "we still have to implement copy in this case!!!")

    val as = new AppendState(this)

    // collect all known objects
    String.prepareAndAppend(file, as)

    // prepare pools, i.e. ensure that all objects get IDs which can be turned into logic pointers
    knownPools.foreach(_.prepareSerialization(this))

    // write count of the type block
    file.write(ByteBuffer.wrap(v64(pools.size)))

    // write fields back to their buffers
    val out = new ByteArrayOutputStream

    // write header
    def writeSubPools(p: KnownPool[_, _]) {
      p.append(file, out, as)
      for (p ← p.getSubPools)
        if (p.isInstanceOf[KnownPool[_, _]])
          writeSubPools(p.asInstanceOf[KnownPool[_, _]])
    }
    for (p ← knownPools)
      if (p.isInstanceOf[BasePool[_]])
        writeSubPools(p)

    // write data
    file.write(ByteBuffer.wrap(out.toByteArray()))

    // done:)
    file.close()
  }

  // TODO check if state can be appended
  def canAppend: Boolean = null != fromPath
""")

    // state initialization
    out.write(s"""
  /**
   * replace user types with pools and read required field data
   */
  def finishInitialization {

    // remove proxy types and add names to the string pool
    for (p ← pools.values) {
      String.add(p.name)
      for (f ← p.fields.values) {
        String.add(f.name)
        f.t match {
          case t: NamedUserType ⇒ f.t = pools(t.name)
          case t: UserType      ⇒ f.t = pools(t.name)
          case _                ⇒
        }
      }
    }

    // read required fields
    val fieldParser = new FieldParser(this)
${(for (d ← IR) yield s"""    ${d.getName}.readFields(fieldParser)""").mkString("\n")}
  }

  /**
   * prints some debug information onto stdout
   */
  def dumpDebugInfo = {
    println("DEBUG INFO START")
    println(s"StringPool ($${String.size}):")
    for (i ← 1 to String.stringPositions.size) {
      if (!String.idMap.contains(i))
        print("lazy ⇒ ")
      println(String.get(i))
    }
    String.newStrings.foreach(println(_))
    println("")
    println("ReflectionPool:")
    for (s ← pools.values) {
      println(s.name + s.blockInfos.mkString("[", ", ", "] "))
      println(s.fields.values.map { f ⇒ f.toString ++ f.dataChunks.mkString(" = {", ", ", "}") }.mkString("{\\n", ";\\n", "\\n}\\n"))
    }
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
