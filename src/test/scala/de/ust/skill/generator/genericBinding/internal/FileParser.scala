/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 29.10.2014                               *
 * |___/_|\_\_|_|____|    by: Timm Felden                                     *
\*                                                                            */
package de.ust.skill.generator.genericBinding.internal

import java.nio.file.Path

import scala.Array.canBuildFrom
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.Queue
import scala.collection.mutable.Stack

import de.ust.skill.generator.genericBinding.api.SkillState
import de.ust.skill.generator.genericBinding.internal.streams.FileInputStream
import de.ust.skill.generator.genericBinding.internal.streams.InStream

/**
 * The parser implementation is based on the denotational semantics given in TR14§6.
 *
 * @author Timm Felden
 */
object FileParser {

  def file(in : FileInputStream) : SkillState = {
    // ERROR REPORTING
    var blockCounter = 0;
    var seenTypes = HashSet[String]();

    // STRING POOL
    val String = new StringPool(in)

    // STORAGE POOLS
    val types = ArrayBuffer[StoragePool[_ <: SkillType, _ <: SkillType]]();
    val poolByName = HashMap[String, StoragePool[_ <: SkillType, _ <: SkillType]]();
    @inline def newPool[T <: B, B <: SkillType](name : String, superPool : StoragePool[_ >: T <: B, B], restrictions : HashSet[Restriction]) : StoragePool[T, B] = {
      val p = (name match {

        case _ ⇒
          if (null == superPool) new BasePool[SkillType.SubType](types.size, name, HashMap())
          else superPool.makeSubPool(types.size, name)
      }).asInstanceOf[StoragePool[T, B]]
      types += p
      poolByName.put(name, p)
      p
    }

    /**
     * Turns a field type into a preliminary type information. In case of user types, the declaration of the respective
     *  user type may follow after the field declaration.
     */
    @inline def fieldType : FieldType[_] = in.v64 match {
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
      case 15           ⇒ ConstantLengthArray(in.v64, fieldType)
      case 17           ⇒ VariableLengthArray(fieldType)
      case 18           ⇒ ListType(fieldType)
      case 19           ⇒ SetType(fieldType)
      case 20           ⇒ MapType(fieldType, fieldType) // <- TR14
      // TR13: MapType((0 until in.v64.toInt).map { n ⇒ groundType }.toSeq)
      case i if i >= 32 ⇒ TypeDefinitionIndex(i - 32)
      case id           ⇒ throw ParseException(in, blockCounter, s"Invalid type ID: $id", null)
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
      // deferred field declaration appends: pool, ID, type, name, block
      val fieldInsertionQueue = new Queue[(StoragePool[_ <: SkillType, _ <: SkillType], Int, FieldType[_], String, BulkChunkInfo)]
      // field data updates: pool x fieldID
      val fieldDataQueue = new Queue[(StoragePool[_ <: SkillType, _ <: SkillType], Int)]
      var offset = 0L

      @inline def typeDefinition[T <: B, B <: SkillType] {
        // read type part
        val name = String.get(in.v64)

        @inline def superDefinition : StoragePool[_ >: T <: B, B] = {
          val superID = in.v64;
          if (0 == superID)
            null
          else if (superID > types.size)
            throw new ParseException(in, blockCounter, s"Type $name refers to an ill-formed super type.", null)
          else
            types(superID.toInt - 1).asInstanceOf[StoragePool[_ >: T <: B, B]]
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
            case i ⇒ throw new ParseException(in, blockCounter, "Found unknown type restriction $i. Please regenerate your binding, if possible.", null)
          }
          ).toSet[Restriction].to
        }
        @inline def fieldRestrictions(t : FieldType[_]) : HashSet[Restriction] = {
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
              case t    ⇒ throw new ParseException(in, blockCounter, "Type $t can not be range restricted!", null)
            }
            case 2 ⇒ throw new ParseException(in, blockCounter, "Fields can not be unique restricted!", null)
            case 3 ⇒ ConstantLengthPointer
            case 4 ⇒ throw new ParseException(in, blockCounter, "Fields can not be singleton restricted!", null)
            case 5 ⇒ Coding(String.get(in.v64))
            case 6 ⇒ throw new ParseException(in, blockCounter, "Fields can not be monotone restricted!", null)
            case i ⇒ throw new ParseException(in, blockCounter, "Found unknown field restriction $i. Please regenerate your binding, if possible.", null)
          }
          ).toSet[Restriction].to
        }

        // type duplication error detection
        if (seenTypes.contains(name))
          throw ParseException(in, blockCounter, s"Duplicate definition of type $name", null)
        seenTypes += name

        // try to parse the type definition
        try {
          var count = in.v64

          var definition : StoragePool[T, B] = null
          var lbpsi = 1L // bpsi is 1 if the first legal index is one
          if (poolByName.contains(name)) {
            definition = poolByName(name).asInstanceOf[StoragePool[T, B]]

          } else {
            val rest = typeRestrictions
            val superDef = superDefinition
            definition = newPool[T, B](name, superDef, rest)
          }
          if (0L != count && definition.superPool.isDefined)
            lbpsi = in.v64

          // adjust lbpsi
          // @note -1 is due to conversion between index<->array offset
          lbpsi += definition.basePool.data.length - 1

          // store block info and prepare resize
          definition.blockInfos += BlockInfo(lbpsi, count)
          resizeQueue += definition

          // read field part
          val fields = definition.fields
          var totalFieldCount = fields.size
          for (fieldIndex ← 0 until in.v64.toInt) {
            val ID = in.v64.toInt
            if (ID > totalFieldCount || ID < 0)
              throw new ParseException(in, blockCounter, s"Found an illegal field ID: $ID", null)

            if (ID == totalFieldCount) {
              // new field
              val name = String.get(in.v64)
              val t = fieldType
              val rest = fieldRestrictions(t)
              val end = in.v64

              fieldInsertionQueue += ((definition, ID, t, name, new BulkChunkInfo(offset, end, count + definition.dynamicSize)))

              offset = end
              totalFieldCount += 1
            } else {
              // known field
              val end = in.v64
              fields(ID).addChunk(new SimpleChunkInfo(offset, end, lbpsi, count))

              offset = end
            }
            fieldDataQueue += ((definition, ID))
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
        for ((p, id, t, name, block) ← fieldInsertionQueue) {
          p.addField(id, eliminatePreliminaryTypesIn(t), name).addChunk(block)
        }
      }
      @inline def eliminatePreliminaryTypesIn[T](t : FieldType[T]) : FieldType[T] = t match {
        case TypeDefinitionIndex(i) ⇒ try {
          types(i.toInt).asInstanceOf[FieldType[T]]
        } catch {
          case e : Exception ⇒ throw ParseException(in, blockCounter, s"inexistent user type $i (user types: ${types.zipWithIndex.map(_.swap).toMap.mkString})", e)
        }
        case TypeDefinitionName(n) ⇒ try {
          poolByName(n).asInstanceOf[FieldType[T]]
        } catch {
          case e : Exception ⇒ throw ParseException(in, blockCounter, s"inexistent user type $n (user types: ${poolByName.mkString})", e)
        }
        case ConstantLengthArray(l, t) ⇒ ConstantLengthArray(l, eliminatePreliminaryTypesIn(t))
        case VariableLengthArray(t)    ⇒ VariableLengthArray(eliminatePreliminaryTypesIn(t))
        case ListType(t)               ⇒ ListType(eliminatePreliminaryTypesIn(t))
        case SetType(t)                ⇒ SetType(eliminatePreliminaryTypesIn(t))
        case MapType(k, v)             ⇒ MapType(eliminatePreliminaryTypesIn(k), eliminatePreliminaryTypesIn(v))
        case t                         ⇒ t
      }
      @inline def processFieldData {
        // we have to add the file offset to all begins and ends we encounter
        val fileOffset = in.position

        //process field data declarations in order of appearance and update offsets to absolute positions
        @inline def processField[T](p : StoragePool[_ <: SkillType, _ <: SkillType], index : Int) {
          val f = p.fields(index).asInstanceOf[FieldDeclaration[T]]
          f.t = eliminatePreliminaryTypesIn[T](f.t.asInstanceOf[FieldType[T]])

          f.addOffsetToLastChunk(fileOffset)

          // TODO move to KnownField
          if (f.isInstanceOf[KnownField[T]])
            FieldParser.parseThisField(in, p, f.asInstanceOf[KnownField[T]], poolByName, String)
        }
        for ((p, fID) ← fieldDataQueue) {
          processField(p, fID)
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
      try {
        stringBlock
        typeBlock
      } catch {
        case e : SkillException ⇒ throw e
        case e : Exception      ⇒ throw new ParseException(in, blockCounter, "unexpected foreign exception", e)
      }

      blockCounter += 1
      seenTypes = HashSet()
    }

    new SerializableState(

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
