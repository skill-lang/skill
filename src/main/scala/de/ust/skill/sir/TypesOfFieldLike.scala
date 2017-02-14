/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 06.12.2016                               *
 * |___/_|\_\_|_|____|    by: feldentm                                        *
\*                                                                            */
package de.ust.skill.sir

import de.ust.skill.common.scala.SkillID
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.UnknownObject

sealed class FieldLike (_skillID : SkillID) extends SkillObject(_skillID) with Annotations {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  private[sir] def this(_skillID : SkillID, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, `type` : _root_.de.ust.skill.sir.Type, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) {
    this(_skillID)
    _comment = comment
    _name = name
    _type = `type`
    _hints = hints
    _restrictions = restrictions
  }

  final protected var _comment : _root_.de.ust.skill.sir.Comment = null
  /**
   *  the comment provided by the user or null
   */
  def comment : _root_.de.ust.skill.sir.Comment = _comment
  final private[sir] def Internal_comment = _comment
  /**
   *  the comment provided by the user or null
   */
  def `comment_=`(comment : _root_.de.ust.skill.sir.Comment) : scala.Unit = { _comment = comment }
  final private[sir] def `Internal_comment_=`(v : _root_.de.ust.skill.sir.Comment) = _comment = v

  final protected var _name : _root_.de.ust.skill.sir.Identifier = null
  /**
   *  the skill name
   */
  def name : _root_.de.ust.skill.sir.Identifier = _name
  final private[sir] def Internal_name = _name
  /**
   *  the skill name
   */
  def `name_=`(name : _root_.de.ust.skill.sir.Identifier) : scala.Unit = { _name = name }
  final private[sir] def `Internal_name_=`(v : _root_.de.ust.skill.sir.Identifier) = _name = v

  final protected var _type : _root_.de.ust.skill.sir.Type = null
  /**
   *  The type of the field.
   */
  def `type` : _root_.de.ust.skill.sir.Type = _type
  final private[sir] def Internal_type = _type
  /**
   *  The type of the field.
   */
  def `type_=`(`type` : _root_.de.ust.skill.sir.Type) : scala.Unit = { _type = `type` }
  final private[sir] def `Internal_type_=`(v : _root_.de.ust.skill.sir.Type) = _type = v

  override def prettyString : String = s"FieldLike(#$skillID, comment: ${comment}, name: ${name}, type: ${`type`}, hints: ${hints}, restrictions: ${restrictions})"

  override def getTypeName : String = "fieldlike"

  override def toString = "FieldLike#"+skillID
}

object FieldLike {
  def unapply(self : FieldLike) = Some(self.comment, self.name, self.`type`, self.hints, self.restrictions)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: FieldLike])
      extends FieldLike(_skillID) with UnknownObject[FieldLike] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

/**
 *  a language custom field
 */
sealed class CustomField (_skillID : SkillID) extends FieldLike(_skillID) {
  private[sir] def this(_skillID : SkillID, language : java.lang.String, options : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CustomFieldOption], typename : java.lang.String, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, `type` : _root_.de.ust.skill.sir.Type, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) {
    this(_skillID)
    _language = language
    _options = options
    _typename = typename
    _comment = comment
    _name = name
    _type = `type`
    _hints = hints
    _restrictions = restrictions
  }

  final protected var _language : java.lang.String = null
  /**
   *  the name of the language that treats this fields
   */
  def language : java.lang.String = _language
  final private[sir] def Internal_language = _language
  /**
   *  the name of the language that treats this fields
   */
  def `language_=`(language : java.lang.String) : scala.Unit = { _language = language }
  final private[sir] def `Internal_language_=`(v : java.lang.String) = _language = v

  final protected var _options : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CustomFieldOption] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CustomFieldOption]()
  def options : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CustomFieldOption] = _options
  final private[sir] def Internal_options = _options
  def `options_=`(options : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CustomFieldOption]) : scala.Unit = { _options = options }
  final private[sir] def `Internal_options_=`(v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.CustomFieldOption]) = _options = v

  final protected var _typename : java.lang.String = null
  /**
   *  the type name that will be used to create a language specific field type
   */
  def typename : java.lang.String = _typename
  final private[sir] def Internal_typename = _typename
  /**
   *  the type name that will be used to create a language specific field type
   */
  def `typename_=`(typename : java.lang.String) : scala.Unit = { _typename = typename }
  final private[sir] def `Internal_typename_=`(v : java.lang.String) = _typename = v

  override def prettyString : String = s"CustomField(#$skillID, language: ${language}, options: ${options}, typename: ${typename}, comment: ${comment}, name: ${name}, type: ${`type`}, hints: ${hints}, restrictions: ${restrictions})"

  override def getTypeName : String = "customfield"

  override def toString = "CustomField#"+skillID
}

object CustomField {
  def unapply(self : CustomField) = Some(self.language, self.options, self.typename, self.comment, self.name, self.`type`, self.hints, self.restrictions)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: CustomField])
      extends CustomField(_skillID) with UnknownObject[CustomField] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

sealed class Field (_skillID : SkillID) extends FieldLike(_skillID) {
  private[sir] def this(_skillID : SkillID, isAuto : scala.Boolean, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, `type` : _root_.de.ust.skill.sir.Type, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) {
    this(_skillID)
    _isAuto = isAuto
    _comment = comment
    _name = name
    _type = `type`
    _hints = hints
    _restrictions = restrictions
  }

  final protected var _isAuto : scala.Boolean = false
  /**
   *  true, iff the field is an auto field
   */
  def isAuto : scala.Boolean = _isAuto
  final private[sir] def Internal_isAuto = _isAuto
  /**
   *  true, iff the field is an auto field
   */
  def `isAuto_=`(isAuto : scala.Boolean) : scala.Unit = { _isAuto = isAuto }
  final private[sir] def `Internal_isAuto_=`(v : scala.Boolean) = _isAuto = v

  override def prettyString : String = s"Field(#$skillID, isauto: ${isAuto}, comment: ${comment}, name: ${name}, type: ${`type`}, hints: ${hints}, restrictions: ${restrictions})"

  override def getTypeName : String = "field"

  override def toString = "Field#"+skillID
}

object Field {
  def unapply(self : Field) = Some(self.isAuto, self.comment, self.name, self.`type`, self.hints, self.restrictions)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: Field])
      extends Field(_skillID) with UnknownObject[Field] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}

/**
 *  a view onto a field
 *  @note  some components such as annotations must not be set by views
 */
sealed class FieldView (_skillID : SkillID) extends FieldLike(_skillID) {
  private[sir] def this(_skillID : SkillID, target : _root_.de.ust.skill.sir.FieldLike, comment : _root_.de.ust.skill.sir.Comment, name : _root_.de.ust.skill.sir.Identifier, `type` : _root_.de.ust.skill.sir.Type, hints : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Hint], restrictions : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.Restriction]) {
    this(_skillID)
    _target = target
    _comment = comment
    _name = name
    _type = `type`
    _hints = hints
    _restrictions = restrictions
  }

  final protected var _target : _root_.de.ust.skill.sir.FieldLike = null
  /**
   *  the viewed component
   */
  def target : _root_.de.ust.skill.sir.FieldLike = _target
  final private[sir] def Internal_target = _target
  /**
   *  the viewed component
   */
  def `target_=`(target : _root_.de.ust.skill.sir.FieldLike) : scala.Unit = { _target = target }
  final private[sir] def `Internal_target_=`(v : _root_.de.ust.skill.sir.FieldLike) = _target = v

  override def prettyString : String = s"FieldView(#$skillID, target: ${target}, comment: ${comment}, name: ${name}, type: ${`type`}, hints: ${hints}, restrictions: ${restrictions})"

  override def getTypeName : String = "fieldview"

  override def toString = "FieldView#"+skillID
}

object FieldView {
  def unapply(self : FieldView) = Some(self.target, self.comment, self.name, self.`type`, self.hints, self.restrictions)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: FieldView])
      extends FieldView(_skillID) with UnknownObject[FieldView] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}
