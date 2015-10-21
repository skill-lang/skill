/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.io.PrintWriter

import scala.collection.JavaConversions._

import de.ust.skill.ir._
import de.ust.skill.ir.restriction._

trait TypesMaker extends GeneralOutputMaker {

  @inline def fieldName(implicit f : Field) : String = escaped(f.getName.camel())
  @inline def localFieldName(implicit f : Field) : String = escaped("_" + f.getName.camel())
  @inline def fieldAssignName(implicit f : Field) : String = escaped(f.getName.camel() + "_=")

  abstract override def make {
    super.make

    val packageName = if(this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.')+1)
    else this.packageName;

    // create one file for each type hierarchy to help parallel builds
    for(base <- IR if null==base.getSuperType){

      val out = open(s"TypesOf${base.getName.capital}.scala")

      //package
      out.write(s"""package ${this.packageName}

import de.ust.skill.common.scala.SkillID
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.UnknownObject
""")


    for (t ← IR if t.getBaseType == base) {
      val fields = t.getAllFields.filter(!_.isConstant)
      val relevantFields = fields.filter(!_.isIgnored)

      //class declaration
      out.write(s"""
${
        comment(t)
}sealed class ${name(t)} (_skillID : SkillID) ${
        if (null != t.getSuperType()) { s"extends ${name(t.getSuperType)}" }
        else { "extends SkillObject" }
      }(_skillID) {${
	  if(t.getSuperType == null) s"""

  //reveal skill id
  ${if(revealSkillID)"" else s"protected[${packageName}] "}final def getSkillID = skillID
"""
	  else ""
	}""")

      // constructor
	if(!relevantFields.isEmpty){
    	out.write(s"""
  private[$packageName] def this(_skillID : SkillID${appendConstructorArguments(t)}) {
    this(_skillID)
    ${relevantFields.map{f ⇒ s"${localFieldName(f)} = ${fieldName(f)}"}.mkString("\n    ")}
  }
""")
	}

	///////////////////////
	// getters & setters //
	///////////////////////
	for(f <- t.getFields if !f.isInstanceOf[View]){
    implicit val thisF = f;

      def makeField:String = {
		if(f.isIgnored || f.isConstant)
		  ""
		else
	      s"""
  final protected var $localFieldName : ${mapType(f.getType())} = ${defaultValue(f)}"""
	  }

      def makeGetterImplementation:String = {
        if(f.isIgnored)
          s"""throw new IllegalAccessError("${name(f)} has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
        else if(f.isConstant)
          s"${f.constantValue().toString}.to${mapType(f.getType)}"
        else
          localFieldName
      }

      def makeSetterImplementation:String = {
        if(f.isIgnored)
          s"""throw new IllegalAccessError("${name(f)} has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
        else
          s"{ ${ //@range check
            if(f.getType().isInstanceOf[GroundType]){
              if(f.getType().asInstanceOf[GroundType].isInteger)
                f.getRestrictions.collect{case r:IntRangeRestriction⇒r}.map{r ⇒ s"""require(${r.getLow}L <= ${name(f)} && ${name(f)} <= ${r.getHigh}L, "${name(f)} has to be in range [${r.getLow};${r.getHigh}]"); """}.mkString("")
              else if("f32".equals(f.getType.getName))
                f.getRestrictions.collect{case r:FloatRangeRestriction⇒r}.map{r ⇒ s"""require(${r.getLowFloat}f <= ${name(f)} && ${name(f)} <= ${r.getHighFloat}f, "${name(f)} has to be in range [${r.getLowFloat};${r.getHighFloat}]"); """}.mkString("")
              else if("f64".equals(f.getType.getName))
               f.getRestrictions.collect{case r:FloatRangeRestriction⇒r}.map{r ⇒ s"""require(${r.getLowDouble} <= ${name(f)} && ${name(f)} <= ${r.getHighDouble}, "${name(f)} has to be in range [${r.getLowDouble};${r.getHighDouble}]"); """}.mkString("")
              else
                ""
            }
            else
              ""
          }${//@monotone modification check
            if(!t.getRestrictions.collect{case r:MonotoneRestriction⇒r}.isEmpty){
              s"""require(skillID == -1L, "${t.getName} is specified to be monotone and this instance has already been subject to serialization!"); """
            }
            else
              ""
        }$localFieldName = ${name(f)} }"
      }

      if(f.isConstant)
        out.write(s"""$makeField
  ${comment(f)}final def $fieldName = $makeGetterImplementation
""")
      else
        out.write(s"""$makeField
  ${comment(f)}final def $fieldName : ${mapType(f.getType())} = $makeGetterImplementation
  ${comment(f)}final def $fieldAssignName(${name(f)} : ${mapType(f.getType())}) : scala.Unit = $makeSetterImplementation
""")
    }

    out.write(s"""
  override def prettyString : String = s"${name(t)}(#$$skillID${
    (
        for(f <- t.getAllFields)
          yield if(f.isIgnored) s""", ${f.getName()}: <<ignored>>"""
          else if (!f.isConstant) s""", ${if(f.isAuto)"auto "else""}${f.getName()}: $${${name(f)}}"""
          else s""", const ${f.getName()}: ${f.constantValue()}"""
    ).mkString
  })"

  override def getTypeName : String = "${t.getSkillName}"

  override def toString = "${t.getName.capital}#"+skillID
}
""")

      out.write(s"""
object ${name(t)} {
${ // create unapply method if the type has fields, that can be matched (none or more then 12 is pointless)
  val fs = t.getAllFields().filterNot(_.isConstant())
  if(fs.isEmpty() || fs.size > 12)""
  else s"""  def unapply(self : ${name(t)}) = ${(for (f ← fs) yield "self."+escaped(f.getName.camel)).mkString("Some(", ", ", ")")}
"""
}
  final class UnknownSubType(
    _skillID : SkillID,
    val owner : Access[_ <: ${name(t)}])
      extends ${name(t)}(_skillID) with UnknownObject[${name(t)}] {

    final override def getTypeName : String = owner.name

    final override def prettyString : String = s"$$getTypeName#$$skillID"
  }
}
""");
    }

    out.close()
    }
  }
}
