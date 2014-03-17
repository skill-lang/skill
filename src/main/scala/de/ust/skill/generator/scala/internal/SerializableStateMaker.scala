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

    out.write(s"""package ${packagePrefix}internal

import java.nio.file.Files
import java.nio.file.Path

import ${packagePrefix}api._
import ${packagePrefix}internal.streams.FileOutputStream

/**
 * This class is used to handle objects in a serializable state.
 *
 * @author Timm Felden
 */
final class SerializableState(
${
      (for (t ← IR) yield s"  val ${t.getCapitalName} : ${t.getCapitalName}Access,").mkString("\n")
    }
  val String : StringAccess,
  val pools : Array[StoragePool[_ <: SkillType, _ <: SkillType]],
  var fromPath : Option[Path])
    extends SkillState {

  val poolByName = pools.map(_.name).zip(pools).toSeq.toMap

  finalizePools;

  def all = pools.iterator.asInstanceOf[Iterator[Access[_ <: SkillType]]]

  def write(target : Path) : Unit = {
    new StateWriter(this, FileOutputStream.write(target))
    if (fromPath.isEmpty)
      fromPath = Some(target)
  }
  // @note: this is more tricky then append, because the state has to be prepared before the file is deleted
  def write() : Unit = ???

  def append() : Unit = new StateAppender(this, FileOutputStream.append(fromPath.getOrElse(throw new IllegalStateException("The state was not created using a read operation, thus append is not possible!"))))
  def append(target : Path) : Unit = {
    if (fromPath.isEmpty) {
      // append and write is the same operation, if we did not read a file
      write(target)
    } else if (target.equals(fromPath.get)) {
      append
    } else {
      // copy the read file to the target location
      Files.deleteIfExists(target)
      Files.copy(fromPath.get, target)
      // append to the target file
      new StateAppender(this, FileOutputStream.append(target))
    }
  }

  @inline private def finalizePools {
    @inline def eliminatePreliminaryTypesIn(t : FieldType) : FieldType = t match {
      case TypeDefinitionIndex(i) ⇒ try {
        pools(i.toInt)
      } catch {
        case e : Exception ⇒ throw new IllegalStateException(s"inexistent user type $$i (user types: $${poolByName.mkString})", e)
      }
      case TypeDefinitionName(n) ⇒ try {
        poolByName(n)
      } catch {
        case e : Exception ⇒ throw new IllegalStateException(s"inexistent user type $$n (user types: $${poolByName.mkString})", e)
      }
      case ConstantLengthArray(l, t) ⇒ ConstantLengthArray(l, eliminatePreliminaryTypesIn(t))
      case VariableLengthArray(t)    ⇒ VariableLengthArray(eliminatePreliminaryTypesIn(t))
      case ListType(t)               ⇒ ListType(eliminatePreliminaryTypesIn(t))
      case SetType(t)                ⇒ SetType(eliminatePreliminaryTypesIn(t))
      case MapType(ts)               ⇒ MapType(for (t ← ts) yield eliminatePreliminaryTypesIn(t))
      case t                         ⇒ t
    }
    for (p ← pools) {
      val fieldMap = p.fields.map { _.name }.zip(p.fields).toMap

      for ((n, t) ← p.knownFields if !fieldMap.contains(n)) {
        p.addField(new FieldDeclaration(eliminatePreliminaryTypesIn(t), n, p.fields.size))
      }
    }
  }
}
//final class SerializableState extends SkillState {
//  import SerializableState._
//
//  /**
//   * path of the file, the serializable state has been created from; this is required for lazy evaluation and appending
//   *
//   * null iff the state has not been created from a file
//   */
//  private[internal] var fromPath: Path = null
//  /**
//   * reader for the input file, which is used for lazy evaluation and error reporting
//   */
//  private[internal] var fromReader: ByteReader = null
//
//  private[internal] var pools = new HashMap[String, AbstractPool]
//  @inline def knownPools = pools.values.collect({ case p: KnownPool[_, _] ⇒ p }).toArray
//
//  override val String = new StringPool(this)
//  ${
      //      (
      //        for (d ← IR; name = d.getName)
      //          yield s"""override val $name = new ${name}StoragePool(this)
      //  pools.put("${d.getSkillName}", $name)"""
      //      ).mkString("", "\n  ", "")
    }
//
//  /**
//   * Creates a new SKilL file at target.
//   */
//  override def write(target: Path) {
//    import SerializationFunctions._
//
//    Files.deleteIfExists(target)
//
//    val file = Files.newByteChannel(target,
//      StandardOpenOption.CREATE,
//      StandardOpenOption.WRITE,
//      StandardOpenOption.TRUNCATE_EXISTING).asInstanceOf[FileChannel]
//
//    if (null == fromPath) try {
//      fromPath = target
//      fromReader = new ByteReader(Files.newByteChannel(fromPath).asInstanceOf[FileChannel])
//    } catch {
//      case e: IOException ⇒ // we don't care, because this means we do not have right permissions Oo
//    }
//
//    val ws = new WriteState(this)
//
//    String.prepareAndWrite(file, ws)
//    knownPools.foreach(_.prepareSerialization(this))
//
//    ws.writeTypeBlock(file)
//
//    file.close()
//
//    // reset skill IDs
//    knownPools.foreach {
//      case p: BasePool[_] ⇒ p.restoreIndices
//      case _              ⇒
//    }
//  }
//
//  /**
//   * Appends to the SKilL file we read.
//   */
//  override def append = append(fromPath.ensuring(_ != null))
//  /**
//   * Appends to an existing SKilL file.
//   */
//  override def append(target: Path) {
//    require(canAppend)
//    import SerializationFunctions._
//
//    val file = Files.newByteChannel(target,
//      StandardOpenOption.APPEND,
//      StandardOpenOption.WRITE).asInstanceOf[FileChannel]
//
//    assert(target == fromPath, "we still have to implement copy in this case!!!")
//
//    val as = new AppendState(this)
//
//    String.prepareAndAppend(file, as)
//    knownPools.foreach(_.prepareSerialization(this))
//
//    as.writeTypeBlock(file)
//    file.close()
//  }
//
//  // TODO check if state can be appended
//  def canAppend: Boolean = null != fromPath
//
//    // read required fields
//    val fieldParser = new FieldParser(this)
//${
      //      (for (d ← IR) yield s"""    ${d.getName}.readFields(fieldParser)""").mkString("\n")
    }
//  }
//
//  /**
//   * prints some debug information onto stdout
//   */
//  def dumpDebugInfo = {
//    println("DEBUG INFO START")
//    println(s"StringPool ($${String.size}):")
//    for (i ← 1 to String.stringPositions.size) {
//      if (!String.idMap.contains(i))
//        print("lazy ⇒ ")
//      println(String.get(i))
//    }
//    String.newStrings.foreach(println(_))
//    println("")
//    println("ReflectionPool:")
//    for (s ← pools.values) {
//      println(s.name + s.blockInfos.mkString("[", ", ", "] "))
//      println(s.fields.values.map { f ⇒ f.toString ++ f.dataChunks.mkString(" = {", ", ", "}") }.mkString("{\\n", ";\\n", "\\n}\\n"))
//    }
//    println("")
//    println("StoragePools:")
//    knownPools.foreach({ s ⇒
//      println(s.name+": "+s.dynamicSize);
//      println(s.iterator.map(_.asInstanceOf[KnownType].prettyString).mkString("  ", "\\n  ", "\\n"))
//    })
//    println("")
//
//    println("DEBUG INFO END")
//  }
//}

object SerializableState {
  /**
   * Creates a new and empty serializable state.
   */
  def create() : SerializableState = {
${
      var i = -1
      (for (t ← IR) yield s"""    val ${t.getCapitalName} = new ${t.getCapitalName}StoragePool(${i += 1; i}${if (null == t.getSuperType) "" else {", " + t.getSuperType.getCapitalName}})""").mkString("\n")
    }
    new SerializableState(
${
      (for (t ← IR) yield s"""      ${t.getCapitalName},""").mkString("\n")
    }
      new StringPool(null),
      Array[StoragePool[_ <: SkillType, _ <: SkillType]](${IR.map(_.getCapitalName).mkString(",")}),
      None
    )
  }
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
