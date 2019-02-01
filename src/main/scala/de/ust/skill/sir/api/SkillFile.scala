/*  ___ _  ___ _ _                                                                                                    *\
** / __| |/ (_) | |     Your SKilL scala Binding                                                                      **
** \__ \ ' <| | | |__   generated: 01.02.2019                                                                         **
** |___/_|\_\_|_|____|  by: feldentm                                                                                  **
\*                                                                                                                    */
package de.ust.skill.sir.api


import java.io.File
import java.nio.file.Path

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import de.ust.skill.common.jvm.streams.FileInputStream
import de.ust.skill.common.jvm.streams.MappedInStream
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.Create
import de.ust.skill.common.scala.api.Read
import de.ust.skill.common.scala.api.ReadMode
import de.ust.skill.common.scala.api.ReadOnly
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Write
import de.ust.skill.common.scala.api.WriteMode
import de.ust.skill.common.scala.internal.SkillState
import de.ust.skill.common.scala.internal.StoragePool
import de.ust.skill.common.scala.internal.StringPool
import de.ust.skill.common.scala.internal.fieldTypes
import de.ust.skill.common.scala.internal.fieldTypes.AnnotationType
import de.ust.skill.common.scala.internal.InterfacePool
import de.ust.skill.common.scala.internal.UnrootedInterfacePool

/**
 * A skill file that corresponds to your specification. Have fun!
 *
 * @author Timm Felden
 */
final class SkillFile(
  _path : Path,
  _mode : WriteMode,
  _String : StringPool,
  _annotationType : fieldTypes.AnnotationType,
  _types : ArrayBuffer[StoragePool[_ <: SkillObject, _ <: SkillObject]],
  _typesByName : HashMap[String, StoragePool[_ <: SkillObject, _ <: SkillObject]])
    extends SkillState(_path, _mode, _String, _annotationType, _types, _typesByName) {

  private[api] def AnnotationType : AnnotationType = annotationType

  val BuildInformation : internal.BuildInformationPool = typesByName("buildinformation").asInstanceOf[internal.BuildInformationPool]
  val Comment : internal.CommentPool = typesByName("comment").asInstanceOf[internal.CommentPool]
  val CommentTag : internal.CommentTagPool = typesByName("commenttag").asInstanceOf[internal.CommentTagPool]
  val CustomFieldOption : internal.CustomFieldOptionPool = typesByName("customfieldoption").asInstanceOf[internal.CustomFieldOptionPool]
  val FieldLike : internal.FieldLikePool = typesByName("fieldlike").asInstanceOf[internal.FieldLikePool]
  val CustomField : internal.CustomFieldPool = typesByName("customfield").asInstanceOf[internal.CustomFieldPool]
  val Field : internal.FieldPool = typesByName("field").asInstanceOf[internal.FieldPool]
  val FieldView : internal.FieldViewPool = typesByName("fieldview").asInstanceOf[internal.FieldViewPool]
  val FilePath : internal.FilePathPool = typesByName("filepath").asInstanceOf[internal.FilePathPool]
  val Hint : internal.HintPool = typesByName("hint").asInstanceOf[internal.HintPool]
  val Identifier : internal.IdentifierPool = typesByName("identifier").asInstanceOf[internal.IdentifierPool]
  val Restriction : internal.RestrictionPool = typesByName("restriction").asInstanceOf[internal.RestrictionPool]
  val Tool : internal.ToolPool = typesByName("tool").asInstanceOf[internal.ToolPool]
  val ToolTypeCustomization : internal.ToolTypeCustomizationPool = typesByName("tooltypecustomization").asInstanceOf[internal.ToolTypeCustomizationPool]
  val Type : internal.TypePool = typesByName("type").asInstanceOf[internal.TypePool]
  val BuiltinType : internal.BuiltinTypePool = typesByName("builtintype").asInstanceOf[internal.BuiltinTypePool]
  val MapType : internal.MapTypePool = typesByName("maptype").asInstanceOf[internal.MapTypePool]
  val SingleBaseTypeContainer : internal.SingleBaseTypeContainerPool = typesByName("singlebasetypecontainer").asInstanceOf[internal.SingleBaseTypeContainerPool]
  val ConstantLengthArrayType : internal.ConstantLengthArrayTypePool = typesByName("constantlengtharraytype").asInstanceOf[internal.ConstantLengthArrayTypePool]
  val SimpleType : internal.SimpleTypePool = typesByName("simpletype").asInstanceOf[internal.SimpleTypePool]
  val ConstantInteger : internal.ConstantIntegerPool = typesByName("constantinteger").asInstanceOf[internal.ConstantIntegerPool]
  val UserdefinedType : internal.UserdefinedTypePool = typesByName("userdefinedtype").asInstanceOf[internal.UserdefinedTypePool]
  val ClassType : internal.ClassTypePool = typesByName("classtype").asInstanceOf[internal.ClassTypePool]
  val EnumType : internal.EnumTypePool = typesByName("enumtype").asInstanceOf[internal.EnumTypePool]
  val InterfaceType : internal.InterfaceTypePool = typesByName("interfacetype").asInstanceOf[internal.InterfaceTypePool]
  val TypeDefinition : internal.TypeDefinitionPool = typesByName("typedefinition").asInstanceOf[internal.TypeDefinitionPool]

  val Annotations : UnrootedInterfacePool[_root_.de.ust.skill.sir.Annotations] =
    new UnrootedInterfacePool[_root_.de.ust.skill.sir.Annotations]("Annotations", AnnotationType,
      Array[StoragePool[_ <: _root_.de.ust.skill.sir.Annotations, _ <: SkillObject]](FieldLike, ToolTypeCustomization, UserdefinedType));

  val GroundType : InterfacePool[_root_.de.ust.skill.sir.GroundType, _root_.de.ust.skill.sir.Type] =
    new InterfacePool[_root_.de.ust.skill.sir.GroundType, _root_.de.ust.skill.sir.Type]("GroundType", Type,
      Array[StoragePool[_ <: _root_.de.ust.skill.sir.GroundType, _ <: _root_.de.ust.skill.sir.Type]](SimpleType, UserdefinedType));
}

/**
 * @author Timm Felden
 */
object SkillFile {
  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def open(path : String, read : ReadMode = Read, write : WriteMode = Write) : SkillFile = {
    val f = new File(path)
    if (!f.exists())
      f.createNewFile()
    readFile(f.toPath, read, write)
  }
  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def open(file : File, read : ReadMode, write : WriteMode) : SkillFile = {
    if (!file.exists())
      file.createNewFile()
    readFile(file.toPath, read, write)
  }
  /**
   * Reads a binary SKilL file and turns it into a SKilL state.
   */
  def open(path : Path, read : ReadMode, write : WriteMode) : SkillFile = readFile(path, read, write)

  /**
   * same as open(create)
   */
  def create(path : Path, write : WriteMode = Write) : SkillFile = readFile(path, Create, write)

  /**
   * same as open(read)
   */
  def read(path : Path, write : WriteMode = Write) : SkillFile = readFile(path, Read, write)

  private def readFile(path : Path, read : ReadMode, write : WriteMode) : SkillFile = read match {
    case Read ⇒ internal.FileParser.read(FileInputStream.open(path, write == ReadOnly), write)

    case Create ⇒
      val String = new StringPool(null)
      val types = new ArrayBuffer[StoragePool[_ <: SkillObject, _ <: SkillObject]]()
      val typesByName = new HashMap[String, StoragePool[_ <: SkillObject, _ <: SkillObject]]()
      val Annotation = new AnnotationType(types, typesByName)
      val dataList = new ArrayBuffer[MappedInStream]()
      internal.FileParser.makeState(path, write, String, Annotation, types, typesByName, dataList)
  }
}
