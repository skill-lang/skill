/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 09.11.2016                               *
 * |___/_|\_\_|_|____|    by: feldentm                                        *
\*                                                                            */
package de.ust.skill.sir.api.internal

import java.nio.file.Path

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import de.ust.skill.common.jvm.streams.MappedInStream
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.TypeSystemError
import de.ust.skill.common.scala.api.WriteMode
import de.ust.skill.common.scala.internal.BasePool
import de.ust.skill.common.scala.internal.SkillFileParser
import de.ust.skill.common.scala.internal.StoragePool
import de.ust.skill.common.scala.internal.StringPool
import de.ust.skill.common.scala.internal.UnknownBasePool
import de.ust.skill.common.scala.internal.fieldTypes.AnnotationType
import de.ust.skill.common.scala.internal.restrictions.TypeRestriction

import _root_.de.ust.skill.sir.api.SkillFile

/**
 * Parametrization of the common skill parser.
 *
 * @author Timm Felden
 */
object FileParser extends SkillFileParser[SkillFile] {

  // TODO we can make this faster using a hash map (for large type systems)
  def newPool(
    typeId : Int,
    name : String,
    superPool : StoragePool[_ <: SkillObject, _ <: SkillObject],
    rest : HashSet[TypeRestriction]) : StoragePool[_ <: SkillObject, _ <: SkillObject] = {
    name match {
      case "buildinformation" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type BuildInformation with super type ${superPool.name}, but none was expected")
        else
          new BuildInformationPool(typeId)

      case "comment" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type Comment with super type ${superPool.name}, but none was expected")
        else
          new CommentPool(typeId)

      case "commenttag" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type CommentTag with super type ${superPool.name}, but none was expected")
        else
          new CommentTagPool(typeId)

      case "customfieldoption" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type CustomFieldOption with super type ${superPool.name}, but none was expected")
        else
          new CustomFieldOptionPool(typeId)

      case "fieldlike" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type FieldLike with super type ${superPool.name}, but none was expected")
        else
          new FieldLikePool(typeId)

      case "customfield" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type CustomField with no super type, but FieldLike was expected")
        else
          new CustomFieldPool(typeId, superPool.asInstanceOf[FieldLikePool])

      case "field" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type Field with no super type, but FieldLike was expected")
        else
          new FieldPool(typeId, superPool.asInstanceOf[FieldLikePool])

      case "fieldview" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type FieldView with no super type, but FieldLike was expected")
        else
          new FieldViewPool(typeId, superPool.asInstanceOf[FieldLikePool])

      case "filepath" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type FilePath with super type ${superPool.name}, but none was expected")
        else
          new FilePathPool(typeId)

      case "hint" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type Hint with super type ${superPool.name}, but none was expected")
        else
          new HintPool(typeId)

      case "identifier" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type Identifier with super type ${superPool.name}, but none was expected")
        else
          new IdentifierPool(typeId)

      case "restriction" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type Restriction with super type ${superPool.name}, but none was expected")
        else
          new RestrictionPool(typeId)

      case "tool" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type Tool with super type ${superPool.name}, but none was expected")
        else
          new ToolPool(typeId)

      case "tooltypecustomization" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type ToolTypeCustomization with super type ${superPool.name}, but none was expected")
        else
          new ToolTypeCustomizationPool(typeId)

      case "type" ⇒
        if (null != superPool)
          throw TypeSystemError(s"the opened file contains a type Type with super type ${superPool.name}, but none was expected")
        else
          new TypePool(typeId)

      case "builtintype" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type BuiltinType with no super type, but Type was expected")
        else
          new BuiltinTypePool(typeId, superPool.asInstanceOf[TypePool])

      case "maptype" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type MapType with no super type, but BuiltinType was expected")
        else
          new MapTypePool(typeId, superPool.asInstanceOf[BuiltinTypePool])

      case "singlebasetypecontainer" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type SingleBaseTypeContainer with no super type, but BuiltinType was expected")
        else
          new SingleBaseTypeContainerPool(typeId, superPool.asInstanceOf[BuiltinTypePool])

      case "constantlengtharraytype" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type ConstantLengthArrayType with no super type, but SingleBaseTypeContainer was expected")
        else
          new ConstantLengthArrayTypePool(typeId, superPool.asInstanceOf[SingleBaseTypeContainerPool])

      case "simpletype" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type SimpleType with no super type, but BuiltinType was expected")
        else
          new SimpleTypePool(typeId, superPool.asInstanceOf[BuiltinTypePool])

      case "constantinteger" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type ConstantInteger with no super type, but SimpleType was expected")
        else
          new ConstantIntegerPool(typeId, superPool.asInstanceOf[SimpleTypePool])

      case "userdefinedtype" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type UserdefinedType with no super type, but Type was expected")
        else
          new UserdefinedTypePool(typeId, superPool.asInstanceOf[TypePool])

      case "classtype" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type ClassType with no super type, but UserdefinedType was expected")
        else
          new ClassTypePool(typeId, superPool.asInstanceOf[UserdefinedTypePool])

      case "enumtype" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type EnumType with no super type, but UserdefinedType was expected")
        else
          new EnumTypePool(typeId, superPool.asInstanceOf[UserdefinedTypePool])

      case "interfacetype" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type InterfaceType with no super type, but UserdefinedType was expected")
        else
          new InterfaceTypePool(typeId, superPool.asInstanceOf[UserdefinedTypePool])

      case "typedefinition" ⇒
        if (null == superPool)
          throw TypeSystemError(s"the opened file contains a type TypeDefinition with no super type, but UserdefinedType was expected")
        else
          new TypeDefinitionPool(typeId, superPool.asInstanceOf[UserdefinedTypePool])
      case _ ⇒
        if (null == superPool)
          new UnknownBasePool(name, typeId)
        else
          superPool.makeSubPool(name, typeId)
    }
  }

  def makeState(path : Path,
                mode : WriteMode,
                String : StringPool,
                Annotation : AnnotationType,
                types : ArrayBuffer[StoragePool[_ <: SkillObject, _ <: SkillObject]],
                typesByName : HashMap[String, StoragePool[_ <: SkillObject, _ <: SkillObject]],
                dataList : ArrayBuffer[MappedInStream]) : SkillFile = {

    // ensure that pools exist at all
    if(!typesByName.contains("buildinformation")) {
      val p = newPool(types.size + 32, "buildinformation", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("buildinformation", p)
    }
    if(!typesByName.contains("comment")) {
      val p = newPool(types.size + 32, "comment", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("comment", p)
    }
    if(!typesByName.contains("commenttag")) {
      val p = newPool(types.size + 32, "commenttag", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("commenttag", p)
    }
    if(!typesByName.contains("customfieldoption")) {
      val p = newPool(types.size + 32, "customfieldoption", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("customfieldoption", p)
    }
    if(!typesByName.contains("fieldlike")) {
      val p = newPool(types.size + 32, "fieldlike", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("fieldlike", p)
    }
    if(!typesByName.contains("customfield")) {
      val p = newPool(types.size + 32, "customfield", typesByName("fieldlike"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("customfield", p)
    }
    if(!typesByName.contains("field")) {
      val p = newPool(types.size + 32, "field", typesByName("fieldlike"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("field", p)
    }
    if(!typesByName.contains("fieldview")) {
      val p = newPool(types.size + 32, "fieldview", typesByName("fieldlike"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("fieldview", p)
    }
    if(!typesByName.contains("filepath")) {
      val p = newPool(types.size + 32, "filepath", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("filepath", p)
    }
    if(!typesByName.contains("hint")) {
      val p = newPool(types.size + 32, "hint", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("hint", p)
    }
    if(!typesByName.contains("identifier")) {
      val p = newPool(types.size + 32, "identifier", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("identifier", p)
    }
    if(!typesByName.contains("restriction")) {
      val p = newPool(types.size + 32, "restriction", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("restriction", p)
    }
    if(!typesByName.contains("tool")) {
      val p = newPool(types.size + 32, "tool", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("tool", p)
    }
    if(!typesByName.contains("tooltypecustomization")) {
      val p = newPool(types.size + 32, "tooltypecustomization", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("tooltypecustomization", p)
    }
    if(!typesByName.contains("type")) {
      val p = newPool(types.size + 32, "type", null, StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("type", p)
    }
    if(!typesByName.contains("builtintype")) {
      val p = newPool(types.size + 32, "builtintype", typesByName("type"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("builtintype", p)
    }
    if(!typesByName.contains("maptype")) {
      val p = newPool(types.size + 32, "maptype", typesByName("builtintype"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("maptype", p)
    }
    if(!typesByName.contains("singlebasetypecontainer")) {
      val p = newPool(types.size + 32, "singlebasetypecontainer", typesByName("builtintype"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("singlebasetypecontainer", p)
    }
    if(!typesByName.contains("constantlengtharraytype")) {
      val p = newPool(types.size + 32, "constantlengtharraytype", typesByName("singlebasetypecontainer"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("constantlengtharraytype", p)
    }
    if(!typesByName.contains("simpletype")) {
      val p = newPool(types.size + 32, "simpletype", typesByName("builtintype"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("simpletype", p)
    }
    if(!typesByName.contains("constantinteger")) {
      val p = newPool(types.size + 32, "constantinteger", typesByName("simpletype"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("constantinteger", p)
    }
    if(!typesByName.contains("userdefinedtype")) {
      val p = newPool(types.size + 32, "userdefinedtype", typesByName("type"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("userdefinedtype", p)
    }
    if(!typesByName.contains("classtype")) {
      val p = newPool(types.size + 32, "classtype", typesByName("userdefinedtype"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("classtype", p)
    }
    if(!typesByName.contains("enumtype")) {
      val p = newPool(types.size + 32, "enumtype", typesByName("userdefinedtype"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("enumtype", p)
    }
    if(!typesByName.contains("interfacetype")) {
      val p = newPool(types.size + 32, "interfacetype", typesByName("userdefinedtype"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("interfacetype", p)
    }
    if(!typesByName.contains("typedefinition")) {
      val p = newPool(types.size + 32, "typedefinition", typesByName("userdefinedtype"), StoragePool.noTypeRestrictions)
      types.append(p)
      typesByName.put("typedefinition", p)
    }

    // trigger allocation and instance creation
    locally {
      val ts = types.iterator
      while(ts.hasNext) {
        val t = ts.next
        t.allocateData
        if(t.isInstanceOf[BasePool[_]])
          StoragePool.setNextPools(t)
      }
    }
    types.par.foreach(_.allocateInstances)
    
    // create restrictions (may contain references to instances)

    // read eager fields
    triggerFieldDeserialization(types, dataList)

    val r = new SkillFile(path, mode, String, Annotation, types, typesByName)
    types.par.foreach(_.ensureKnownFields(r))
    r
  }
}
