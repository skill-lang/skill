/*  ___ _  ___ _ _                                                            *\
 * / __| |/ (_) | |       Your SKilL Scala Binding                            *
 * \__ \ ' <| | | |__     generated: 05.12.2016                               *
 * |___/_|\_\_|_|____|    by: feldentm                                        *
\*                                                                            */
package de.ust.skill.sir

import de.ust.skill.common.scala.SkillID
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.UnknownObject

sealed class Tool (_skillID : SkillID) extends SkillObject(_skillID) {

  //reveal skill id
  protected[sir] final def getSkillID = skillID

  private[sir] def this(_skillID : SkillID, buildTargets : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.BuildInformation], customFieldAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.ToolTypeCustomization], customTypeAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, _root_.de.ust.skill.sir.ToolTypeCustomization], description : java.lang.String, name : java.lang.String, selectedFields : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, scala.collection.mutable.HashMap[java.lang.String, _root_.de.ust.skill.sir.FieldLike]], selectedUserTypes : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.UserdefinedType]) {
    this(_skillID)
    _buildTargets = buildTargets
    _customFieldAnnotations = customFieldAnnotations
    _customTypeAnnotations = customTypeAnnotations
    _description = description
    _name = name
    _selectedFields = selectedFields
    _selectedUserTypes = selectedUserTypes
  }

  final protected var _buildTargets : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.BuildInformation] = scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.BuildInformation]()
  /**
   *  build targets associated with this tool
   */
  def buildTargets : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.BuildInformation] = _buildTargets
  final private[sir] def Internal_buildTargets = _buildTargets
  /**
   *  build targets associated with this tool
   */
  def `buildTargets_=`(buildTargets : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.BuildInformation]) : scala.Unit = { _buildTargets = buildTargets }
  final private[sir] def `Internal_buildTargets_=`(v : scala.collection.mutable.ArrayBuffer[_root_.de.ust.skill.sir.BuildInformation]) = _buildTargets = v

  final protected var _customFieldAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.ToolTypeCustomization] = scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.ToolTypeCustomization]()
  /**
   *  overrides existing annotations
   */
  def customFieldAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.ToolTypeCustomization] = _customFieldAnnotations
  final private[sir] def Internal_customFieldAnnotations = _customFieldAnnotations
  /**
   *  overrides existing annotations
   */
  def `customFieldAnnotations_=`(customFieldAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.ToolTypeCustomization]) : scala.Unit = { _customFieldAnnotations = customFieldAnnotations }
  final private[sir] def `Internal_customFieldAnnotations_=`(v : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.FieldLike, _root_.de.ust.skill.sir.ToolTypeCustomization]) = _customFieldAnnotations = v

  final protected var _customTypeAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, _root_.de.ust.skill.sir.ToolTypeCustomization] = scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, _root_.de.ust.skill.sir.ToolTypeCustomization]()
  /**
   *  overrides existing annotations
   */
  def customTypeAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, _root_.de.ust.skill.sir.ToolTypeCustomization] = _customTypeAnnotations
  final private[sir] def Internal_customTypeAnnotations = _customTypeAnnotations
  /**
   *  overrides existing annotations
   */
  def `customTypeAnnotations_=`(customTypeAnnotations : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, _root_.de.ust.skill.sir.ToolTypeCustomization]) : scala.Unit = { _customTypeAnnotations = customTypeAnnotations }
  final private[sir] def `Internal_customTypeAnnotations_=`(v : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, _root_.de.ust.skill.sir.ToolTypeCustomization]) = _customTypeAnnotations = v

  final protected var _description : java.lang.String = null
  def description : java.lang.String = _description
  final private[sir] def Internal_description = _description
  def `description_=`(description : java.lang.String) : scala.Unit = { _description = description }
  final private[sir] def `Internal_description_=`(v : java.lang.String) = _description = v

  final protected var _name : java.lang.String = null
  def name : java.lang.String = _name
  final private[sir] def Internal_name = _name
  def `name_=`(name : java.lang.String) : scala.Unit = { _name = name }
  final private[sir] def `Internal_name_=`(v : java.lang.String) = _name = v

  final protected var _selectedFields : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, scala.collection.mutable.HashMap[java.lang.String, _root_.de.ust.skill.sir.FieldLike]] = scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, scala.collection.mutable.HashMap[java.lang.String, _root_.de.ust.skill.sir.FieldLike]]()
  /**
   *  The set of fields selected by this tool. The string argument is used to ensure, that selected fields have unique
   *  names.
   */
  def selectedFields : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, scala.collection.mutable.HashMap[java.lang.String, _root_.de.ust.skill.sir.FieldLike]] = _selectedFields
  final private[sir] def Internal_selectedFields = _selectedFields
  /**
   *  The set of fields selected by this tool. The string argument is used to ensure, that selected fields have unique
   *  names.
   */
  def `selectedFields_=`(selectedFields : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, scala.collection.mutable.HashMap[java.lang.String, _root_.de.ust.skill.sir.FieldLike]]) : scala.Unit = { _selectedFields = selectedFields }
  final private[sir] def `Internal_selectedFields_=`(v : scala.collection.mutable.HashMap[_root_.de.ust.skill.sir.UserdefinedType, scala.collection.mutable.HashMap[java.lang.String, _root_.de.ust.skill.sir.FieldLike]]) = _selectedFields = v

  final protected var _selectedUserTypes : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.UserdefinedType] = scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.UserdefinedType]()
  /**
   *  the set of user types selected by this tool
   */
  def selectedUserTypes : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.UserdefinedType] = _selectedUserTypes
  final private[sir] def Internal_selectedUserTypes = _selectedUserTypes
  /**
   *  the set of user types selected by this tool
   */
  def `selectedUserTypes_=`(selectedUserTypes : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.UserdefinedType]) : scala.Unit = { _selectedUserTypes = selectedUserTypes }
  final private[sir] def `Internal_selectedUserTypes_=`(v : scala.collection.mutable.HashSet[_root_.de.ust.skill.sir.UserdefinedType]) = _selectedUserTypes = v

  override def prettyString : String = s"Tool(#$skillID, buildtargets: ${buildTargets}, customfieldannotations: ${customFieldAnnotations}, customtypeannotations: ${customTypeAnnotations}, description: ${description}, name: ${name}, selectedfields: ${selectedFields}, selectedusertypes: ${selectedUserTypes})"

  override def getTypeName : String = "tool"

  override def toString = "Tool#"+skillID
}

object Tool {
  def unapply(self : Tool) = Some(self.buildTargets, self.customFieldAnnotations, self.customTypeAnnotations, self.description, self.name, self.selectedFields, self.selectedUserTypes)

  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: Tool])
      extends Tool(_skillID) with UnknownObject[Tool] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$getTypeName#$skillID"
  }
}
