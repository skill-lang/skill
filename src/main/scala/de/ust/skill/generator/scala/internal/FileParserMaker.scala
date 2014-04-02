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

import java.nio.file.Path

import scala.Array.canBuildFrom
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Queue
import scala.collection.mutable.Stack

import ${packagePrefix}api.SkillState
import ${packagePrefix}internal.streams.FileInputStream
import ${packagePrefix}internal.streams.InStream

/**
 * The parser object contains LL-1 style parsing methods to deal with a binary SKilL file.
 *
 * @author Timm Felden
 */
object FileParser {

  def file(in : InStream) : SkillState = {
    // ERROR REPORTING
    var blockCounter = 0;
    var seenTypes = HashSet[String]();

    // STRING POOL
    val String = new StringPool(in)

    // STORAGE POOLS
    val types = ArrayBuffer[StoragePool[_ <: SkillType, _ <: SkillType]]();
    val poolByName = HashMap[String, StoragePool[_ <: SkillType, _ <: SkillType]]();
    @inline def newPool[T <: B, B <: SkillType](name : String, superName : String, restrictions : HashSet[Restriction]) : StoragePool[T, B] = {
      val p = (name match {
${
      (for (t ← IR) yield s"""        case "${t.getSkillName}" ⇒ new ${t.getCapitalName}StoragePool(types.size${
        if (null == t.getSuperType) ""
        else s""", poolByName("${t.getSuperType.getSkillName}").asInstanceOf[${t.getSuperType.getCapitalName}StoragePool]"""
      })""").mkString("\n")
    }
        case _ ⇒
          if (null == superName) new BasePool[SkillType](types.size, name, HashMap())
          else poolByName(superName).makeSubPool(types.size, name)
      }).asInstanceOf[StoragePool[T, B]]
      types += p
      poolByName.put(name, p)
      p
    }

    /**
     * Turns a field type into a preliminary type information. In case of user types, the declaration of the respective
     *  user type may follow after the field declaration.
     */
    @inline def fieldType : FieldType = in.v64 match {
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
      case id           ⇒ throw ParseException(in, blockCounter, s"Invalid type ID: $$id", null)
    }

    /**
     * matches only types which are legal arguments to ADTs
     */
    @inline def groundType : FieldType = in.v64 match {
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
      case id           ⇒ throw ParseException(in, blockCounter, s"Invalid base type ID: $$id", null)
    }

    @inline def stringBlock {
      try {
        val count = in.v64.toInt

        if (0L != count) {
          val offsets = new Array[Int](count);
          for (i ← 0 until count) {
            offsets(i) = in.i32;
          }
          String.stringPositions.sizeHint(String.stringPositions.size + count)
          var last = 0
          for (i ← 0 until count) {
            String.stringPositions.append((in.position + last, offsets(i) - last))
            String.idMap += null
            last = offsets(i)
          }
          in.jump(in.position + last);
        }
      } catch {
        case e : Exception ⇒ throw ParseException(in, blockCounter, "corrupted string block", e)
      }
    }

    @inline def typeBlock {
      // deferred pool resize requests
      val resizeQueue = new Queue[StoragePool[_ <: SkillType, _ <: SkillType]]
      // deferred field declaration appends
      val fieldInsertionQueue = new Queue[(StoragePool[_ <: SkillType, _ <: SkillType], FieldDeclaration)]
      // field data updates
      val fieldDataQueue = new Queue[(StoragePool[_ <: SkillType, _ <: SkillType], FieldDeclaration)]
      var offset = 0L

      @inline def typeDefinition[T <: B, B <: SkillType] {
        @inline def superDefinition : (String, Long) = {
          val superName = in.v64;
          if (0 == superName)
            (null, 1L) // bpsi is 1 if the first legal index is one
          else
            (String.get(superName), in.v64)
        }
        @inline def typeRestrictions : HashSet[Restriction] = {
          val count = in.v64.toInt
          (for (i ← 0 until count; if i < 7 || 1 == (i % 2))
            yield in.v64 match {
            case 0 ⇒ throw new ParseException(in, blockCounter, "Types can not be nullable restricted!", null)
            case 1 ⇒ throw new ParseException(in, blockCounter, "Types can not be range restricted!", null)
            case 2 ⇒ Unique
            case 3 ⇒ throw new ParseException(in, blockCounter, "Types can not be constant length pointer restricted!", null)
            case 4 ⇒ Singleton
            case 5 ⇒ throw new ParseException(in, blockCounter, "Types can not be coding restricted!", null)
            case 6 ⇒ Monotone
            case i ⇒ throw new ParseException(in, blockCounter, "Found unknown type restriction $$i. Please regenerate your binding, if possible.", null)
          }
          ).toSet[Restriction].to
        }
        @inline def fieldRestrictions(t : FieldType) : HashSet[Restriction] = {
          val count = in.v64.toInt
          (for (i ← 0 until count; if i < 7 || 1 == (i % 2))
            yield in.v64 match {
            case 0 ⇒ Nullable
            case 1 ⇒ t match {
              case null ⇒ throw new Error("Range Restrictions can not be serialized in TR13!")
              case I8   ⇒ Range(in.i8, in.i8)
              case I16  ⇒ Range(in.i16, in.i16)
              case I32  ⇒ Range(in.i32, in.i32)
              case I64  ⇒ Range(in.i64, in.i64)
              case V64  ⇒ Range(in.v64, in.v64)
              case F32  ⇒ Range(in.f32, in.f32)
              case F64  ⇒ Range(in.f64, in.f64)
              case t    ⇒ throw new ParseException(in, blockCounter, "Type $$t can not be range restricted!", null)
            }
            case 2 ⇒ throw new ParseException(in, blockCounter, "Fields can not be unique restricted!", null)
            case 3 ⇒ ConstantLengthPointer
            case 4 ⇒ throw new ParseException(in, blockCounter, "Fields can not be singleton restricted!", null)
            case 5 ⇒ Coding(String.get(in.v64))
            case 6 ⇒ throw new ParseException(in, blockCounter, "Fields can not be monotone restricted!", null)
            case i ⇒ throw new ParseException(in, blockCounter, "Found unknown field restriction $$i. Please regenerate your binding, if possible.", null)
          }
          ).toSet[Restriction].to
        }

        // read type part
        val name = String.get(in.v64)

        // type duplication error detection
        if (seenTypes.contains(name))
          throw ParseException(in, blockCounter, s"Duplicate definition of type $$name", null)
        seenTypes += name

        // try to parse the type definition
        try {
          var definition : StoragePool[T, B] = null
          var count = 0L
          var lbpsi = 1L
          if (poolByName.contains(name)) {
            definition = poolByName(name).asInstanceOf[StoragePool[T, B]]
            if (definition.superPool.isDefined)
              lbpsi = in.v64
            count = in.v64
          } else {
            val superDef = superDefinition
            if (null!=superDef._1 && !poolByName.contains(superDef._1))
              throw new ParseException(in, blockCounter, s"Type $$name refers to unknown super type $${superDef._1}. Known types are: $${types.mkString(", ")}", null)

            lbpsi = superDef._2
            count = in.v64
            val rest = typeRestrictions

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
            if (knownFieldCount > fieldCount)
              throw ParseException(in, blockCounter, s"Type $$name has $$fieldCount fields (requires $$knownFieldCount)", null)

            for (fi ← 0 until knownFieldCount) {
              val end = in.v64
              fields(fi).dataChunks += new SimpleChunkInfo(offset, end, lbpsi, count)
              offset = end
              fieldDataQueue += ((definition, fields(fi)))
            }

            for (fi ← knownFieldCount until fieldCount) {
              val rest = fieldRestrictions(null)
              val t = fieldType
              val name = String.get(in.v64)
              val end = in.v64

              val f = new FieldDeclaration(t, name, fi)
              fieldInsertionQueue += ((definition, f))
              f.dataChunks += new BulkChunkInfo(offset, end, count + definition.dynamicSize)
              offset = end
              fieldDataQueue += ((definition, f))
            }
          } else {
            for (fi ← knownFieldCount until knownFieldCount + fieldCount) {
              val rest = fieldRestrictions(null)
              val t = fieldType
              val name = String.get(in.v64)
              val end = in.v64

              val f = new FieldDeclaration(t, name, fi)
              fieldInsertionQueue += ((definition, f))
              f.dataChunks += new BulkChunkInfo(offset, end, count + definition.dynamicSize)
              offset = end
              fieldDataQueue += ((definition, f))
            }
          }
        } catch {
          case e : java.nio.BufferUnderflowException            ⇒ throw ParseException(in, blockCounter, "unexpected end of file", e)
          case e : Exception if !e.isInstanceOf[ParseException] ⇒ throw ParseException(in, blockCounter, e.getMessage, e)
        }
      }
      @inline def resizePools {
        val resizeStack = new Stack[StoragePool[_ <: SkillType, _ <: SkillType]]
        // resize base pools and push entries to stack
        for (p ← resizeQueue) {
          p match {
            case p : BasePool[_] ⇒ p.resizeData(p.blockInfos.last.count.toInt)
            case _               ⇒
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
      @inline def insertFields {
        for ((d, f) ← fieldInsertionQueue) {
          f.t = eliminatePreliminaryTypesIn(f.t)
          d.addField(f)
        }
      }
      @inline def eliminatePreliminaryTypesIn(t : FieldType) : FieldType = t match {
        case TypeDefinitionIndex(i) ⇒ try {
          types(i.toInt)
        } catch {
          case e : Exception ⇒ throw ParseException(in, blockCounter, s"inexistent user type $$i (user types: $${types.zipWithIndex.map(_.swap).toMap.mkString})", e)
        }
        case TypeDefinitionName(n) ⇒ try {
          poolByName(n)
        } catch {
          case e : Exception ⇒ throw ParseException(in, blockCounter, s"inexistent user type $$n (user types: $${poolByName.mkString})", e)
        }
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
          FieldParser.parseThisField(in, t, f, poolByName, String)
        }
      }

      val count = in.v64.toInt
      for (i ← 0 until count)
        typeDefinition

      resizePools
      insertFields
      processFieldData
    }

    while (!in.eof) {
      stringBlock
      typeBlock

      blockCounter += 1
      seenTypes = HashSet()
    }

    new SerializableState(
${
      (for (t ← IR) yield s"""      poolByName.get("${t.getSkillName}").getOrElse(newPool("${t.getSkillName}", null, null)).asInstanceOf[${t.getCapitalName}StoragePool],""").mkString("\n")
    }
      String,
      types.to,
      in match {
        case in : streams.FileInputStream ⇒ Some(in.path)
        case _                            ⇒ None
      }
    )
  }

  def read(path : Path) = file(new FileInputStream(path))
}
""")

    //class prefix
    out.close()
  }
}
