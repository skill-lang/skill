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
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try
import scala.util.Failure

import de.ust.skill.common.jvm.streams.FileInputStream

import _root_.${packagePrefix}api.WriteMode
import _root_.${packagePrefix}internal

/**
 * The parser implementation is based on the denotational semantics given in TR14§6.
 *
 * @author Timm Felden
 */
object FileParser {

  def read(in : FileInputStream, mode : WriteMode) : State = {
    // ERROR REPORTING
    var blockCounter = 0;
    var seenTypes = HashSet[String]();

    // STRING POOL
    val String = new StringPool(in)

    // STORAGE POOLS
    val types = ArrayBuffer[StoragePool[_ <: SkillType, _ <: SkillType]]();
    val Annotation = internal.Annotation(types)
    val StringType = internal.StringType(String)
    val poolByName = HashMap[String, StoragePool[_ <: SkillType, _ <: SkillType]]();
    @inline def newPool[T <: B, B <: SkillType](name : String, superPool : StoragePool[_ >: T <: B, B],
                                                rs : HashSet[restrictions.TypeRestriction]) : StoragePool[T, B] = {
      val p = (name match {
${
      (for (t ← IR)
        yield s"""        case "${t.getSkillName}" ⇒ new ${t.getName.capital}StoragePool(StringType, Annotation, types.size${
        if (null == t.getSuperType) ""
        else s""", poolByName("${t.getSuperType.getSkillName}").asInstanceOf[${t.getSuperType.getName.capital}StoragePool]"""
      })""").mkString("\n")
    }
        case _ ⇒
          if (null == superPool) new BasePool[SkillType.SubType](types.size, name, Set())
          else superPool.makeSubPool(types.size, name)
      }).asInstanceOf[StoragePool[T, B]]

      // check super type expectations
      if (p.superPool.getOrElse(null) != superPool)
        throw ParseException(in, blockCounter,
          s"${""}""The super type of $$name stored in the file does not match the specification!
  expected $${p.superPool.map(_.name).getOrElse("<none>")}, but was $${
            if (null == superPool) "<none>" else superPool
          }"${""}"", null)

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
      case 20           ⇒ MapType(fieldType, fieldType)
      case i if i >= 32 ⇒ TypeDefinitionIndex(i - 32)
      case id           ⇒ throw ParseException(in, blockCounter, s"Invalid type ID: $$id", null)
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
      val resizeQueue = new Queue[StoragePool[_ <: SkillType, _ <: SkillType]];
      // deferred field declaration appends: pool, ID, type, name, block
      val fieldInsertionQueue = new Queue[(StoragePool[_ <: SkillType, _ <: SkillType], Int, FieldType[_],
                                           HashSet[restrictions.FieldRestriction[_]], String, BulkChunkInfo)];

      // field data updates: pool x fieldID
      val fieldDataQueue = new Queue[(StoragePool[_ <: SkillType, _ <: SkillType], Int)];
      var offset = 0L;

      @inline def typeDefinition[T <: B, B <: SkillType] {
        // read type part
        val name = String.get(in.v64)
        if (null == name)
          throw new ParseException(in, blockCounter, "corrupted file: nullptr in typename", null)

        @inline def superDefinition : StoragePool[_ >: T <: B, B] = {
          val superID = in.v64;
          if (0 == superID)
            null
          else if (superID > types.size)
            throw new ParseException(in, blockCounter, s""${""}"Type $$name refers to an ill-formed super type.
  found: $$superID  current number of other types $${types.size}""${""}", null)
          else
            types(superID.toInt - 1).asInstanceOf[StoragePool[_ >: T <: B, B]]
        }
        @inline def typeRestrictions : HashSet[restrictions.TypeRestriction] = {
          val count = in.v64.toInt
          (for (i ← 0 until count; if i < 7 || 1 == (i % 2))
            yield in.v64 match {
            //            case 0 ⇒ Unique
            //            case 1 ⇒ Singleton
            //            case 2 ⇒ Monotone
            case i ⇒ throw new ParseException(in, blockCounter,
              s"Found unknown type restriction $$i. Please regenerate your binding, if possible.", null)
          }
          ).toSet[restrictions.TypeRestriction].to
        }
        @inline def fieldRestrictions(t : FieldType[_]) : HashSet[restrictions.FieldRestriction[_]] = {
          val count = in.v64.toInt
          (for (i ← 0 until count; if i < 7 || 1 == (i % 2))
            yield in.v64 match {
            case 0 ⇒ restrictions.NonNull
            case 3 ⇒ t match {
              case I8  ⇒ restrictions.Range(in.i8, in.i8)
              case I16 ⇒ restrictions.Range(in.i16, in.i16)
              case I32 ⇒ restrictions.Range(in.i32, in.i32)
              case I64 ⇒ restrictions.Range(in.i64, in.i64)
              case V64 ⇒ restrictions.Range(in.v64, in.v64)
              case F32 ⇒ restrictions.Range(in.f32, in.f32)
              case F64 ⇒ restrictions.Range(in.f64, in.f64)
              case t   ⇒ throw new ParseException(in, blockCounter, s"Type $$t can not be range restricted!", null)
            }
            //            case 5 ⇒ Coding(String.get(in.v64))
            //            case 7 ⇒ ConstantLengthPointer
            case i ⇒ throw new ParseException(in, blockCounter,
              s"Found unknown field restriction $$i. Please regenerate your binding, if possible.", null)
          }
          ).toSet[restrictions.FieldRestriction[_]].to
        }

        // type duplication error detection
        if (seenTypes.contains(name))
          throw ParseException(in, blockCounter, s"Duplicate definition of type $$name", null)
        seenTypes += name

        // try to parse the type definition
        try {
          var count = in.v64

          var definition : StoragePool[T, B] = null
          if (poolByName.contains(name)) {
            definition = poolByName(name).asInstanceOf[StoragePool[T, B]]

          } else {
            val rest = typeRestrictions
            val superDef = superDefinition
            definition = newPool[T, B](name, superDef, rest)
          }

          val bpo = definition.basePool.data.length + (
            if (0L != count && definition.superPool.isDefined) in.v64
            else 0L
          )

          // store block info and prepare resize
          definition.blockInfos += BlockInfo(bpo, count)
          resizeQueue += definition

          // read field part
          val fields = definition.fields
          var totalFieldCount = fields.size
          for (fieldIndex ← 0 until in.v64.toInt) {
            val ID = in.v64.toInt
            if (ID > totalFieldCount || ID < 0)
              throw new ParseException(in, blockCounter, s"Found an illegal field ID: $$ID", null)

            if (ID == totalFieldCount) {
              // new field
              val name = String.get(in.v64)
              if (null == name)
                throw new ParseException(in, blockCounter, "corrupted file: nullptr in fieldname", null)

              val t = fieldType
              val rest = fieldRestrictions(t)
              val end = in.v64

              fieldInsertionQueue += ((definition, ID, t, rest, name,
                new BulkChunkInfo(offset, end, count + definition.dynamicSize)))

              offset = end
              totalFieldCount += 1
            } else {
              // seen field
              val end = in.v64
              fields(ID).addChunk(new SimpleChunkInfo(offset, end, bpo, count))

              offset = end
            }
            fieldDataQueue += ((definition, ID))
          }
        } catch {
          case e : java.nio.BufferUnderflowException ⇒
            throw ParseException(in, blockCounter, "unexpected end of file", e)
          case e : Exception if !e.isInstanceOf[ParseException] ⇒
            throw ParseException(in, blockCounter, e.getMessage, e)
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
          var i = bi.bpo
          val high = bi.bpo + bi.count
          while (i < high && p.insertInstance(i + 1))
            i += 1;
        }
      }
      @inline def insertFields {
        for ((p, id, t, rs, name, block) ← fieldInsertionQueue) {
          p.addField(id, eliminatePreliminaryTypesIn(t), name, rs).addChunk(block)
        }
      }
      @inline def eliminatePreliminaryTypesIn[T](t : FieldType[T]) : FieldType[T] = t match {
        case TypeDefinitionIndex(i) ⇒ try {
          types(i.toInt).asInstanceOf[FieldType[T]]
        } catch {
          case e : Exception ⇒ throw ParseException(in, blockCounter,
            s"inexistent user type $$i (user types: $${
              types.zipWithIndex.map(_.swap).toMap.mkString
            })", e)
        }
        case TypeDefinitionName(n) ⇒ try {
          poolByName(n).asInstanceOf[FieldType[T]]
        } catch {
          case e : Exception ⇒ throw ParseException(in, blockCounter,
            s"inexistent user type $$n (user types: $${poolByName.mkString})", e)
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
        var dataEnd = fileOffset

        // awaiting async read operations
        val asyncReads = ArrayBuffer[Future[Try[Unit]]]();

        //process field data declarations in order of appearance and update offsets to absolute positions
        @inline def processField[T](p : StoragePool[_ <: SkillType, _ <: SkillType], index : Int) {
          val f = p.fields(index).asInstanceOf[FieldDeclaration[T]]
          f.t = eliminatePreliminaryTypesIn[T](f.t.asInstanceOf[FieldType[T]])

          // make begin/end absolute
          f.addOffsetToLastChunk(fileOffset)
          val last = f.lastChunk

          val map = in.map(0L, last.begin, last.end)
          asyncReads.append(Future(Try(f.read(map))))
          dataEnd = Math.max(dataEnd, last.end)
        }
        for ((p, fID) ← fieldDataQueue) {
          processField(p, fID)
        }
        in.jump(dataEnd)

        // await async reads
        for (f ← asyncReads)
          Await.ready(f, Duration.Inf).foreach {
            case Failure(e) ⇒ throw ParseException(in, blockCounter, "reading field data failed, see propagated exception", e)
            case _          ⇒
          }
      }

      val count = in.v64.toInt
      for (i ← 0 until count)
        typeDefinition

      resizePools
      insertFields
      processFieldData
    }

    // process stream
    while (!in.eof) {
      try {
        stringBlock
        typeBlock
      } catch {
        case e : SkillException ⇒ throw e
        case e : Exception      ⇒ throw ParseException(in, blockCounter, "unexpected foreign exception", e)
      }

      blockCounter += 1
      seenTypes = HashSet()
    }

    // finish state
    val r = new State(
${
      (for (t ← IR) yield s"""      poolByName.get("${t.getSkillName}").getOrElse(
        newPool[${mapType(t)}, ${mapType(t.getBaseType)}]("${t.getSkillName}", ${
        if (null == t.getSuperType) "null"
        else s"""poolByName("${t.getSuperType.getSkillName}").asInstanceOf[${t.getSuperType.getName.capital}StoragePool]"""
      }, null)
      ).asInstanceOf[${t.getName.capital}StoragePool],""").mkString("\n")
    }
      String,
      types.to,
      in.path,
      mode
    )
    try {
      r.check
    } catch {
      case e : SkillException ⇒ throw ParseException(in, blockCounter, "Post serialization check failed!", e)
    }
    r
  }
}
""")

    //class prefix
    out.close()
  }
}
