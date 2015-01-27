/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.io.PrintWriter

import scala.collection.JavaConversions._

import de.ust.skill.ir._
import de.ust.skill.ir.restriction._

trait TypesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("Types.scala")

    //package
    out.write(s"""package ${this.packageName}

import _root_.${packagePrefix}api.Access
import _root_.${packagePrefix}internal.FieldDeclaration
import _root_.${packagePrefix}internal.SkillType
import _root_.${packagePrefix}internal.NamedType
""")

    val packageName = if(this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.')+1) else this.packageName;

    for (t ← IR) {
      val fields = t.getAllFields.filter(!_.isConstant)
      val relevantFields = fields.filter(!_.isIgnored)

      //class declaration
      out.write(s"""
${
        comment(t)
}sealed class ${name(t)} private[$packageName] (skillID : Long) ${
        if (null != t.getSuperType()) { s"extends ${name(t.getSuperType)}" }
        else { "extends SkillType" }
      }(skillID) {""")

      // constructor
	if(!relevantFields.isEmpty){
    	out.write(s"""
  private[$packageName] def this(skillID : Long${appendConstructorArguments(t)}) {
    this(skillID)
    ${relevantFields.map{f ⇒ s"_${f.getName()} = ${escaped(f.getName.camel)}"}.mkString("\n    ")}
  }
""")
	}

	///////////////////////
	// getters & setters //
	///////////////////////
	for(f <- t.getFields if !f.isConstant && !f.isInstanceOf[View]){
      val name = f.getName()
      val name_ = escaped(name.camel)
      val Name = name.capital

      def makeField:String = {
		if(f.isIgnored || f.isConstant)
		  ""
		else
	      s"""
  protected var _${f.getName} : ${mapType(f.getType())} = ${defaultValue(f)}"""
	  }

      def makeGetterImplementation:String = {
        if(f.isIgnored)
          s"""throw new IllegalAccessError("$name has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
        else if(f.isConstant)
          f.constantValue().toString
        else
          s"_$name"
      }

      def makeSetterImplementation:String = {
        if(f.isIgnored)
          s"""throw new IllegalAccessError("$name has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
        else if(f.isConstant)
          s"""throw new IllegalAccessError("$name is a constant!")"""
        else
          s"{ ${ //@range check
            if(f.getType().isInstanceOf[GroundType]){
              if(f.getType().asInstanceOf[GroundType].isInteger)
                f.getRestrictions.collect{case r:IntRangeRestriction⇒r}.map{r ⇒ s"""require(${r.getLow}L <= $Name && $Name <= ${r.getHigh}L, "$name has to be in range [${r.getLow};${r.getHigh}]"); """}.mkString("")
              else if("f32".equals(f.getType.getName))
                f.getRestrictions.collect{case r:FloatRangeRestriction⇒r}.map{r ⇒ s"""require(${r.getLowFloat}f <= $Name && $Name <= ${r.getHighFloat}f, "$name has to be in range [${r.getLowFloat};${r.getHighFloat}]"); """}.mkString("")
              else if("f64".equals(f.getType.getName))
               f.getRestrictions.collect{case r:FloatRangeRestriction⇒r}.map{r ⇒ s"""require(${r.getLowDouble} <= $Name && $Name <= ${r.getHighDouble}, "$name has to be in range [${r.getLowDouble};${r.getHighDouble}]"); """}.mkString("")
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
        }_$name = $Name }"
      }

      out.write(s"""$makeField
  ${comment(f)}final def $name_ = $makeGetterImplementation
  ${comment(f)}final def ${name_}_=($Name : ${mapType(f.getType())}) : scala.Unit = $makeSetterImplementation
""")
    }

      // pretty string
    out.write(s"""
  override def prettyString : String = "${t.getName()}"+""")

    val prettyStringArgs = (for(f <- t.getAllFields)
      yield if(f.isIgnored) s"""+", ${f.getName()}: <<ignored>>" """
      else if (!f.isConstant) s"""+", ${if(f.isAuto)"auto "else""}${f.getName()}: "+_${f.getName()}"""
      else s"""+", const ${f.getName()}: ${f.constantValue()}""""
      ).mkString(""""(this: "+this""", "", """+")"""")
      
      out.write(prettyStringArgs)

    // toString
    out.write(s"""
  override def toString = "${t.getName.capital}#"+skillID
}
""")

      out.write(s"""
object ${name(t)} {
${ // create unapply method if the type has fields, that can be matched (none or more then 12 is pointless)
  val fs = t.getAllFields().filter(_.isConstant())
  if(fs.isEmpty() || fs.size > 12)""
  else s"""  def unapply(self : ${name(t)}) = ${(for (f ← fs) yield "self."+escaped(f.getName.camel)).mkString("Some(", ", ", ")")}
"""
}  final class SubType private[$packageName] (val τName : String, skillID : Long) extends ${name(t)}(skillID) with NamedType {
    override def prettyString : String = τName+$prettyStringArgs
    override def toString = τName+"#"+skillID
  }
}
""");
    }

    out.close()
  }
}
