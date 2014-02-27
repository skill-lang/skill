/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal
import de.ust.skill.generator.scala.GeneralOutputMaker

trait FileParserMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/FileParser.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.Queue
import scala.collection.mutable.Stack

import ${packagePrefix}api._
import ${packagePrefix}internal.streams.FileInputStream
import ${packagePrefix}internal.streams.InStream

/**
 * The parser object contains LL-1 style parsing methods to deal with a binary SKilL file.
 *
 * @author Timm Felden
 */
object FileParser {

  def file(in: InStream): SkillState = {
    // ERROR REPORTING
    var blockCounter = 0;

    // STRING POOL
    val strings = ArrayBuffer[String](null);
    @inline def getString(index: Long) = try { strings(index.toInt) } catch { case e: Exception ⇒ throw InvalidPoolIndex(index, strings.size - 1, "string") }

    // STORAGE POOLS
    val types = ArrayBuffer[StoragePool[_ <: SkillType]]();
    val poolByName = HashMap[String, StoragePool[_ <: SkillType]]();
    @inline def newPool(name: String, superName: String, restrictions: Array[Nothing]): StoragePool[_ <: SkillType] = {
      val p = name match {
${
      (for (t ← IR) yield s"""        case "${t.getSkillName}" ⇒ new ${t.getCapitalName}StoragePool(types.size)""").mkString("\n")
    }
        case _ ⇒
          if (null == superName) new BasePool[SkillType](types.size, name, HashMap())
          else new SubPool[SkillType](types.size, name, HashMap(), poolByName(superName))
      }
      types += p
      poolByName.put(name, p)
      p
    }

    /**
     * Turns a field type into a preliminary type information. In case of user types, the declaration of the respective
     *  user type may follow after the field declaration.
     */
    @inline def fieldType: FieldType = in.v64 match {
      case 0            ⇒ ConstantI8(in.i8)
      case 1            ⇒ ConstantI16(in.i16)
      case 2            ⇒ ConstantI32(in.i32)
      case 3            ⇒ ConstantI64(in.i64)
      case 4            ⇒ ConstantV64(in.v64)
      case 5            ⇒ Annotation
      case 6            ⇒ BoolType
      case 7            ⇒ I8
      case 8            ⇒ I16
      case 9            ⇒ I32
      case 10           ⇒ I64
      case 11           ⇒ V64
      case 12           ⇒ F32
      case 13           ⇒ F64
      case 14           ⇒ StringType
      case 15           ⇒ ConstantLengthArray(in.v64, groundType)
      case 17           ⇒ VariableLengthArray(groundType)
      case 18           ⇒ ListType(groundType)
      case 19           ⇒ SetType(groundType)
      case 20           ⇒ MapType((0 until in.v64.toInt).map { n ⇒ groundType }.toSeq)
      case i if i >= 32 ⇒ TypeDefinitionIndex(i - 32)
      case id           ⇒ throw new ParseException(in, blockCounter, s"Invalid type ID: $$id")
    }

    /**
     * matches only types which are legal arguments to ADTs
     */
    @inline def groundType: FieldType = in.v64 match {
      case 5            ⇒ Annotation
      case 6            ⇒ BoolType
      case 7            ⇒ I8
      case 8            ⇒ I16
      case 9            ⇒ I32
      case 10           ⇒ I64
      case 11           ⇒ V64
      case 12           ⇒ F32
      case 13           ⇒ F64
      case 14           ⇒ StringType
      case i if i >= 32 ⇒ TypeDefinitionIndex(i - 32)
      case id           ⇒ throw new ParseException(in, blockCounter, s"Invalid base type ID: $$id")
    }

    @inline def stringBlock {
      try {
        val count = in.v64.toInt
        val offsets = new Array[Int](count);
        for (i ← 0 until count) {
          offsets(i) = in.i32;
        }
        // TODO !lazy
        strings.sizeHint(strings.size + count)
        var last = 0L
        for (i ← 0 until count) {
          strings += new String(in.bytes(offsets(i) - last), "UTF-8")
          last = offsets(i)
        }
      } catch {
        case e: Exception ⇒ throw ParseException(in, blockCounter, "corrupted string block", e)
      }
    }

    @inline def typeBlock {
      val resizeQueue = new Queue[StoragePool[_ <: SkillType]]
      val fieldDataQueue = new Queue[(StoragePool[_ <: SkillType], FieldDeclaration)]
      var offset = 0L

      @inline def typeDefinition {
        @inline def superDefinition: (String, Long) = {
          val superName = in.v64;
          if (0 == superName)
            (null, 1L) // bpsi is 1 if the first legal index is one
          else
            (getString(superName), in.v64)
        }
        @inline def restrictions = {
          in.v64.ensuring(_ == 0, "restriction parsing not implemented")
          Array[Nothing]()
        }

        // read type part
        val name = getString(in.v64)
        try {
          var definition: StoragePool[_ <: SkillType] = null
          var count = 0L
          var lbpsi = 1L
          if (poolByName.contains(name)) {
            definition = poolByName(name)
            if (definition.superPool.isDefined)
              lbpsi = in.v64
            count = in.v64
          } else {
            val superDef = superDefinition
            lbpsi = superDef._2
            count = in.v64
            val rest = restrictions

            definition = newPool(name, superDef._1, rest)
          }

          // adjust lbpsi
          // @note -1 is due to conversion between index<->array offset
          lbpsi += definition.basePool.data.length - 1

          // store block info and prepare resize
          definition.blockInfos += BlockInfo(lbpsi, count)
          resizeQueue += definition

          // read field part
          val fieldCount = in.v64.toInt
          val fields = definition.fields
          val knownFieldCount = fields.size
          if (0 != count) {
            for (fi ← 0 until knownFieldCount) {
              val end = in.v64
              fields(fi).dataChunks += new SimpleChunkInfo(offset, end, lbpsi, count)
              offset = end
              fieldDataQueue += ((definition, fields(fi)))
            }

            for (fi ← knownFieldCount until fieldCount) {
              val rest = restrictions
              val t = fieldType
              val name = getString(in.v64)
              val end = in.v64

              definition.addField(new FieldDeclaration(t, name, fi))
              fields(fi).dataChunks += new BulkChunkInfo(offset, end, count + definition.dynamicSize)
              offset = end
              fieldDataQueue += ((definition, fields(fi)))
            }
          } else {
            for (fi ← knownFieldCount until knownFieldCount + fieldCount) {
              val rest = restrictions
              val t = fieldType
              val name = getString(in.v64)
              val end = in.v64

              definition.addField(new FieldDeclaration(t, name, fi))
              fields(fi).dataChunks += new BulkChunkInfo(offset, end, count + definition.dynamicSize)
              offset = end
              fieldDataQueue += ((definition, fields(fi)))
            }
          }
        } catch {
          case e: Exception ⇒ throw new ParseException(in, blockCounter, e)
        }
      }
      @inline def resizePools {
        val resizeStack = new Stack[StoragePool[_]]
        // resize base pools and push entries to stack
        for (p ← resizeQueue) {
          p match {
            case p: BasePool[_] ⇒ p.resizeData(p.blockInfos.last.count.toInt)
            case _              ⇒
          }
          resizeStack.push(p)
        }

        // create instances from stack
        for (p ← resizeStack) {
          val bi = p.blockInfos.last
          var i = bi.bpsi
          val high = bi.bpsi + bi.count
          while (i < high && p.insertInstance(i + 1))
            i += 1;
        }
      }
      @inline def eliminatePreliminaryTypesIn(t: FieldType): FieldType = t match {
        case TypeDefinitionIndex(i)    ⇒ types(i.toInt)
        case TypeDefinitionName(n)     ⇒ poolByName(n)
        case ConstantLengthArray(l, t) ⇒ ConstantLengthArray(l, eliminatePreliminaryTypesIn(t))
        case VariableLengthArray(t)    ⇒ VariableLengthArray(eliminatePreliminaryTypesIn(t))
        case ListType(t)               ⇒ ListType(eliminatePreliminaryTypesIn(t))
        case SetType(t)                ⇒ SetType(eliminatePreliminaryTypesIn(t))
        case MapType(ts)               ⇒ MapType(for (t ← ts) yield eliminatePreliminaryTypesIn(t))
        case t                         ⇒ t
      }
      @inline def processFieldData {
        // we have to add the file offset to all begins and ends we encounter
        val fileOffset = in.position

        //process field data declarations in order of appearance and update offsets to absolute positions
        for ((t, f) ← fieldDataQueue) {
          f.t = eliminatePreliminaryTypesIn(f.t)

          val c = f.dataChunks.last
          c.begin += fileOffset
          c.end += fileOffset
          FieldParser.parseThisField(in, t, f, poolByName, strings)
        }
      }

      val count = in.v64.toInt
      for (i ← 0 until count)
        typeDefinition

      resizePools
      processFieldData
    }

    while (!in.eof) {
      stringBlock
      typeBlock

      blockCounter += 1
    }

    new SerializableState(
${
  (for (t ← IR) yield s"""      poolByName.get("${t.getSkillName}").getOrElse(newPool("${t.getSkillName}", null, null)).asInstanceOf[${t.getCapitalName}StoragePool],""").mkString("\n")
}
      types
    )
  }

  def read(path: Path) = file(new FileInputStream(Files.newByteChannel(path, StandardOpenOption.READ).asInstanceOf[FileChannel]))
}
""")

    //class prefix
    out.close()
  }
}
