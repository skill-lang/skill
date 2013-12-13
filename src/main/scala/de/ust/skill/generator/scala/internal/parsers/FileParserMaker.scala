/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.parsers

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait FileParserMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/parsers/FileParser.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.parsers

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.language.implicitConversions

import ${packagePrefix}internal._
import ${packagePrefix}internal.pool._

/**
 * The file parser reads the whole type information of a file.
 * It creates a new SerializableState which contains types for these type declaration.
 * The state will handle deserialization of field data.
 *
 * @author Timm Felden
 * @see SKilL TR13 §6.2.
 */
final private class FileParser extends ByteStreamParsers {
  /**
   * creates storage pools in type order
   */
  private def makeState() {
    def makePool(t: UserType): AbstractPool = {
      val result = t.name match {""")

    // make pool (depends on IR) @note: the created code is correct, because it will be executed in type-order :)
    for (d ← IR) {
      val name = d.getName
      if (null == d.getSuperType)
        out.write(s"""
        case "${d.getSkillName}" ⇒
          σ.$name.data = new Array[_root_.${packagePrefix}${name.capitalize}](t.instanceCount.toInt)
          σ.$name
""")
      else
        out.write(s"""
        case "${d.getSkillName}" ⇒
          σ.$name.data = σ.$name.basePool.data
          σ.$name
""")
    }

    out.write("""
        case name ⇒ new GenericPool(name, t.superName.map(σ.pools(_)), t.fields)
      }
      result.blockInfos.appendAll(t.blockInfos)
      for ((n, f) ← t.fields) {
        if (result.fields.contains(n)) {
          // Type-check
          if (result.fields(n).t != f.t)
            throw new TypeMissmatchError(t, result.fields(n).t.toString, f.name, result.name)

          // copy data-chunks
          result.fields(n).dataChunks.appendAll(f.dataChunks)
        } else {
          // add unknownfield
          result.fields.put(n, f)
        }
      }
      σ.pools.put(t.name, result)
      t.subTypes.foreach(makePool(_))
      result
    }

    // make base pools; the makePool function makes sub pools
    for (t ← userTypeIndexMap.values if t.superName.isEmpty)
      makePool(t) match {
        case p: KnownPool[_, _] ⇒ p.constructPool
        case _                  ⇒
      }

    // read eager fields
    val fp = new FieldParser(σ)
    σ.knownPools.foreach(_.readFields(fp))

    // ensure presence of the specified types
    σ.finishInitialization
  }

  /**
   * A type declaration, as it occurs during parsing of a type blocks header.
   *
   * @author Timm Felden
   */
  private class TypeDeclaration(
      val name: String,
      val superName: Option[String],
      val lbpsi: Long,
      val count: Long /*, restrictions*/ ,
      val fieldCount: Long,
      // null iff the type occurred first in this block
      var userType: UserType) {

    // ensure presence of lbpsi type start indices
    if (superName.isEmpty && !localBlockBaseTypeStartIndices.contains(name))
      localBlockBaseTypeStartIndices.put(name, 0L)

    /**
     * Field declarations obtained from the header.
     */
    var fieldDeclarations: List[FieldDeclaration] = null

    /**
     * creates a user type, if non exists
     */
    def mkUserType() {
      // create a new user type
      userType = new UserType(userTypeIndexMap.size, name, superName)
      fieldDeclarations.foreach(userType.addField(_))

      userTypeIndexMap.put(userType.index, userType)
      userTypeNameMap.put(userType.name, userType)
    }

    /**
     * Reads field declarations matching this type declaration from a stream, based on the state σ
     *
     * TODO treat restrictions
     */
    def parseFieldDeclarations: Parser[TypeDeclaration] = {
      // if we have new instances and the type existed and there are new fields, we get into special cases
      if (null != userType && count > 0) {
        val knownFields = userType.fields.size
        if (0 > fieldCount - knownFields)
          throw ParseException(σ.fromReader, blockCounter, s"Type $name has $fieldCount fields (requires $knownFields)")

        var fieldIndex = 0

        repN(knownFields.toInt,
          v64 ^^ { end ⇒
            val result = userType.fieldByIndex(fieldIndex)
            val pos = σ.fromReader.position
            result.dataChunks.append(ChunkInfo(pos + lastOffset, pos + end, lbpsi, count))
            lastOffset = end
            fieldIndex += 1

            result
          }
        ) >> { fields ⇒
            repN((fieldCount - knownFields).toInt, restrictions ~ fieldTypeDeclaration ~ v64 ~ v64 ^^ {
              case r ~ t ~ n ~ end ⇒
                val result = new FieldDeclaration(t, σ(n), fieldIndex)
                val pos = σ.fromReader.position
                result.dataChunks.append(ChunkInfo(pos + lastOffset, pos + end, userType.blockInfos(0).bpsi, userType.instanceCount))
                lastOffset = end
                fieldIndex += 1
                // TODO maybe we have to access the block list here
                userType.addField(result)
                result
            }) ^^ { newFields ⇒
              fields ++ newFields
            }
          }
      } else {
        // we append only fields to the type; it is not important whether or not it existed before;
        //  all fields contain all decalrations
        var fieldIndex = 0

        repN(fieldCount.toInt,
          restrictions ~ fieldTypeDeclaration ~ v64 ~ v64 ^^ {
            case r ~ t ~ n ~ end ⇒
              val name = σ(n)
              val result = new FieldDeclaration(t, name, fieldIndex)
              val pos = σ.fromReader.position
              result.dataChunks.append(ChunkInfo(pos + lastOffset, pos + end, lbpsi, count))
              lastOffset = end
              fieldIndex += 1
              result
          })

      }
    } ^^ { r ⇒
      fieldDeclarations = r;
      this
    }
  }

  /**
   * the state to be created is shared across a file parser instance; file parser instances are used to turn a file into
   * a new state anyway.
   */
  private val σ = new SerializableState;

  // helper structures required to build user types
  private var userTypeIndexMap = new HashMap[Long, UserType]
  private var userTypeNameMap = new HashMap[String, UserType]
  private var blockCounter = 0;
  // last seen offset value
  private var lastOffset = 0L;
  // required for duplicate definition detection
  private val newTypeNames = new HashSet[String]

  /**
   * The map contains start indices of base types, which are required at the end of the type header processing phase to
   *  create absolute base pool indices from relative ones.
   */
  private var localBlockBaseTypeStartIndices = new HashMap[String, Long]

  /**
   * @param σ the processed serializable state
   * @return a function that maps logical indices to the corresponding strings
   */
  private[this] implicit def poolAccess(σ: SerializableState): (Long ⇒ String) = σ.String.get(_)

  /**
   * turns a file into a raw serializable state
   *
   * @note hasMore is required for proper error reporting
   */
  private def file: Parser[SerializableState] = rep(hasMore ~>
    stringBlock ~
    (typeBlock ^^ { declarations ⇒
      // update counters
      declarations.filter(_.superName.isEmpty).foreach({ d ⇒
        localBlockBaseTypeStartIndices.put(d.name, d.userType.instanceCount)
      })

      newTypeNames.clear
      blockCounter += 1
      lastOffset = 0L
      ()
    })
  ) ^^ { _ ⇒ makeState; σ }

  /**
   * reads a string block
   */
  private def stringBlock: Parser[Unit] = v64 >> { count ⇒ repN(count.toInt, i32) } >> stringBlockData

  private def stringBlockData(offsets: List[Int]) = new Parser[Unit] {
    def apply(in: Input) = {
      // add absolute offsets and lengths to the states position buffer
      var it = offsets.iterator
      var last = 0
      val off = in.position
      val map = σ.String.stringPositions

      while (it.hasNext) {
        val next = it.next()
        map.put(map.size + 1, (off + last, next - last))
        last = next
      }

      // jump over string data
      Success((), in.drop(last.toInt))
    }
  }

  /**
   * reads a type block and adds the contained type information to the pool.
   * At the moment, it will process fields as well, because we do not have a good random access file stream
   *  implementation, yet.
   */
  private[this] def typeBlock: Parser[Array[TypeDeclaration]] = (v64 >> { count ⇒ repN(count.toInt, typeDeclaration) } ^^ { rawList: List[TypeDeclaration] ⇒
    val raw = rawList.toArray

    // create new user types for types that did not exist until now
    raw.filter(null == _.userType).foreach(_.mkUserType)

    // set super and base types of new user types
    // note: this requires another pass, because super types may be defined after a type
    def mkBase(t: UserType): Unit = if (null == t.baseType) {
      val s = userTypeNameMap(t.superName.get)
      mkBase(s)
      t.superType = s
      t.baseType = s.baseType
      s.subTypes += t
    }
    // note rather inefficient @see issue #9
    raw.foreach({ d ⇒ mkBase(d.userType) })

    raw.foreach({ t ⇒
      val u = t.userType

      // add local block info
      u.addBlockInfo(BlockInfo(localBlockBaseTypeStartIndices(u.baseType.name) + t.lbpsi, t.count))

      // eliminate preliminary user types in field declarations
      u.fields.values.foreach(f ⇒ {
        if (f.t.isInstanceOf[PreliminaryUserType]) {
          val index = f.t.asInstanceOf[PreliminaryUserType].index
          if (userTypeIndexMap.contains(index))
            f.t = userTypeIndexMap(index)
          else
            throw ParseException(σ.fromReader, blockCounter,
              s"${t.name}.${f.name} refers to inexistent user type $index (user types: ${
                userTypeIndexMap.mkString(", ")
              })"
            )
        }
      })
    })

    raw
  }) >> dropData

  // drop actual data
  private[this] def dropData(raw: Array[TypeDeclaration]) = new Parser[Array[TypeDeclaration]] {
    def apply(in: Input) = {
      in.drop(lastOffset.toInt)
      Success(raw, in)
    }
  }

  /**
   * see skill ref man §6.2
   */
  private[this] def typeDeclaration: Parser[TypeDeclaration] = (v64 ^^ { i ⇒ σ(i) }) >> { name ⇒
    // check for duplicate definition inside of the same block
    if (newTypeNames.contains(name))
      throw new ParseException(σ.fromReader, blockCounter, s"Duplicate definition of type $name")

    newTypeNames.add(name)

    // check if we append to an existing type
    if (userTypeNameMap.contains(name)) {
      val t = userTypeNameMap(name)
      var s: Option[String] = None

      superInfo(t.superName) ~ v64 ~ v64 ^^ {
        case lbpsi ~ count ~ fields ⇒
          new TypeDeclaration(name, s, lbpsi, count, fields, t)
      }

    } else {
      (v64 >> superInfo) ~ v64 ~ restrictions ~ v64 ^^ {
        case (sup, lbpsi) ~ count ~ restrictions ~ fields ⇒
          new TypeDeclaration(name, sup, lbpsi, count, fields, null)
      }
    }
  } >> { _.parseFieldDeclarations }

  /**
   *  @return a tuple with (super name, super index)
   */
  private[this] def superInfo(index: Long) = {
    if (index != 0)
      v64 ^^ { lbpsi ⇒ (Some(σ(index)), lbpsi) }
    else
      success((None, 1L))
  }

  /**
   * return a parser parsing local base pool start index, if necessary
   */
  private[this] def superInfo(superName: Option[String]) = superName match {
    case Some(_) ⇒ v64
    case None    ⇒ success(1L)
  }

  /**
   * restrictions are currently restored to their textual representation
   */
  private[this] def restrictions: Parser[List[String]] = v64 >> { i ⇒ repN(i.toInt, restriction) }
  private[this] def restriction = v64 >> { i ⇒
    i match {
      case 0 ⇒ try { v64 ~ v64 ~ v64 ^^ { case l ~ r ~ b ⇒ s"range(${σ(l)}, ${σ(r)}, ${σ(b)})" } }
      catch { case e: Exception ⇒ throw new SkillException("malformed range extension", e) }

      case 1  ⇒ success("nullable")
      case 2  ⇒ success("unique")
      case 3  ⇒ success("singleton")
      case 4  ⇒ success("constantLengthPointer")
      case 5  ⇒ success("monotone")
      case id ⇒ throw new java.lang.Error(s"Restrictions ID $id not yet supported!")
    }
  }

  /**
   * Turns a field type into a preliminary type information. In case of user types, the declaration of the respective
   *  user type may follow after the field declaration.
   */
  private def fieldTypeDeclaration: Parser[TypeInfo] = v64 >> { i ⇒
    i match {
      case 0            ⇒ i8 ^^ { ConstantI8Info(_) }
      case 1            ⇒ i16 ^^ { ConstantI16Info(_) }
      case 2            ⇒ i32 ^^ { ConstantI32Info(_) }
      case 3            ⇒ i64 ^^ { ConstantI64Info(_) }
      case 4            ⇒ v64 ^^ { ConstantV64Info(_) }
      case 5            ⇒ success(AnnotationInfo)
      case 6            ⇒ success(BoolInfo)
      case 7            ⇒ success(I8Info)
      case 8            ⇒ success(I16Info)
      case 9            ⇒ success(I32Info)
      case 10           ⇒ success(I64Info)
      case 11           ⇒ success(V64Info)
      case 12           ⇒ success(F32Info)
      case 13           ⇒ success(F64Info)
      case 14           ⇒ success(StringInfo)
      case 15           ⇒ v64 ~ baseTypeInfo ^^ { case i ~ t ⇒ new ConstantLengthArrayInfo(i.toInt, t) }
      case 17           ⇒ baseTypeInfo ^^ { new VariableLengthArrayInfo(_) }
      case 18           ⇒ baseTypeInfo ^^ { new ListInfo(_) }
      case 19           ⇒ baseTypeInfo ^^ { new SetInfo(_) }
      case 20           ⇒ v64 >> { n ⇒ repN(n.toInt, baseTypeInfo) } ^^ { new MapInfo(_) }
      case i if i >= 32 ⇒ success(PreliminaryUserType(i - 32))
      case id           ⇒ throw ParseException(σ.fromReader, blockCounter, s"Invalid type ID: $id")
    }
  }

  /**
   * matches only types which are legal arguments to ADTs
   */
  private def baseTypeInfo: Parser[TypeInfo] = v64 ^^ { i ⇒
    i match {
      case 5            ⇒ AnnotationInfo
      case 6            ⇒ BoolInfo
      case 7            ⇒ I8Info
      case 8            ⇒ I16Info
      case 9            ⇒ I32Info
      case 10           ⇒ I64Info
      case 11           ⇒ V64Info
      case 12           ⇒ F32Info
      case 13           ⇒ F64Info
      case 14           ⇒ StringInfo
      case i if i >= 32 ⇒ PreliminaryUserType(i - 32)
    }
  }

  def readFile(path: Path): SerializableState = {
    σ.fromPath = path
    σ.fromReader = new ByteReader(Files.newByteChannel(path).asInstanceOf[FileChannel])
    val in = σ.fromReader
    file(in) match {
      case Success(r, i) ⇒ r
      case NoSuccess(msg, i) ⇒ throw new SkillException(
        s"Failed to parse ${path}:\n  Message: $msg\n  Got stuck at byte ${in.pos.column} with at least ${in.minimumBytesToGo} bytes to go.\n  The next Byte is a ${try { in.next.toHexString } catch { case _: Exception ⇒ "EOF" }}.\n        ")
    }
  }
}

object FileParser extends ByteStreamParsers {
  import FileParser._

  /**
   * reads the contents of a file, creating a new state
   */
  def read(path: Path): SerializableState = (new FileParser).readFile(path)
}
""")

    //class prefix
    out.close()
  }
}
