/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.io.PrintWriter

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir._
import de.ust.skill.ir.restriction._

trait TypesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("Types.scala")

    //package
    out.write(s"""package ${this.packageName}

import ${packagePrefix}api.Access
import ${packagePrefix}internal.FieldDeclaration
import ${packagePrefix}internal.SkillType
import ${packagePrefix}internal.NamedType
""")

    val packageName = if(this.packageName.contains('.')) this.packageName.substring(this.packageName.lastIndexOf('.')+1) else this.packageName;

    for (t ← IR) {
      val fields = t.getAllFields.filter(!_.isConstant)
      val relevantFields = fields.filter(!_.isIgnored)

      //class comment
      if (t.getSkillComment.size > 0)
        out.write(s"/*${t.getSkillComment()}*/\n")

      //class prefix
      val Name = t.getName.capital;
      out.write(s"""
sealed class $Name private[$packageName] (skillID : Long) ${
        if (null != t.getSuperType()) { s"extends ${t.getSuperType().getName.capital}" }
        else { "extends SkillType" }
      }(skillID) {""")

      // constructor
	if(!relevantFields.isEmpty){
    	out.write(s"""
  private[$packageName] def this(skillID : Long${appendConstructorArguments(t)}) {
    this(skillID)
    ${relevantFields.map{f ⇒ s"_${f.getName()} = ${escaped(f.getName)}"}.mkString("\n    ")}
  }
""")
	}

	///////////////////////
	// getters & setters //
	///////////////////////
	for(f <- t.getFields if !f.isConstant){
      val name = f.getName()
      val name_ = escaped(name)
      val Name = name.capitalize

      def makeField:String = {
		if(f.isIgnored)
		  ""
		else
	      s"""
  protected var _${f.getName} : ${mapType(f.getType())} = ${defaultValue(f.getType)}"""
	  }

      def makeGetterImplementation:String = {
        if(f.isIgnored)
          s"""throw new IllegalAccessError("$name has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
        else
          s"_$name"
      }

      def makeSetterImplementation:String = {
        if(f.isIgnored)
          s"""throw new IllegalAccessError("$name has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
        else
          s"{ ${//non-null check
          if(!"annotation".equals(f.getType().getName()) && f.getType().isInstanceOf[ReferenceType] && f.getRestrictions().collect({case r:NullableRestriction⇒r}).isEmpty)
            s"""require($Name != null, "$name is specified to be nonnull!"); """
          else
            ""
          }${ //@range check
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

      if ("annotation".equals(f.getType().getName())) { 
        out.write(s"""$makeField
  final def $name_ : SkillType = $makeGetterImplementation
  final def ${name_}_=($Name : SkillType): Unit = $makeSetterImplementation
""")
      } else {
        out.write(s"""$makeField
  final def $name_ = $makeGetterImplementation
  final def ${name_}_=($Name : ${mapType(f.getType())}) = $makeSetterImplementation
""")
      }
    }

    // generic get
    out.write(s"""
  override def get[@specialized T](field : FieldDeclaration[T]) : T = field.name match {
${
      (for(f <- t.getAllFields.filterNot(_.isIgnored)) yield s"""    case "${f.getSkillName}" ⇒ _${f.getName}.asInstanceOf[T]""").mkString("\n")
}${
      (for(f <- t.getAllFields.filter(_.isIgnored)) yield s"""    case "${f.getSkillName}" ⇒ throw new IllegalAccessError("${f.getName} is ignored")""").mkString("\n")
}
    case _ ⇒ super.get(field)
  }
""")

    // generic set
    out.write(s"""
  override def set[@specialized T](field : FieldDeclaration[T], value : T) : Unit = field.name match {
${
  (
    for(f <- t.getAllFields.filterNot{t ⇒ t.isIgnored || t.isConstant()}) 
      yield s"""    case "${f.getSkillName}" ⇒ _${f.getName} = value.asInstanceOf[${mapType(f.getType)}]"""
  ).mkString("\n")
}${
  (
    for(f <- t.getAllFields.filter(_.isIgnored)) 
      yield s"""    case "${f.getSkillName}" ⇒ throw new IllegalAccessError("${f.getName} is ignored!")"""
  ).mkString("\n")
}${
  (
    for(f <- t.getAllFields.filter(_.isConstant)) 
      yield s"""    case "${f.getSkillName}" ⇒ throw new IllegalAccessError("${f.getName} is constant!")"""
  ).mkString("\n")
}
    case _ ⇒ super.set(field, value)
  }
""")

      // pretty string
    out.write(s"""  override def prettyString : String = "${t.getName()}"+""")

    val prettyStringArgs = (for(f <- t.getAllFields)
      yield if(f.isIgnored) s"""+", ${f.getName()}: <<ignored>>" """
      else if (!f.isConstant) s"""+", ${if(f.isAuto)"auto "else""}${f.getName()}: "+_${f.getName()}"""
      else s"""+", const ${f.getName()}: ${f.constantValue()}""""
      ).mkString(""""(this: "+this""", "", """+")"""")

      out.write(prettyStringArgs)

    // toString
    out.write(s"""
  override def toString = "$Name#"+skillID
}
""")

      out.write(s"""
object $Name {
  def unapply(self : $Name) = ${(for (f ← t.getAllFields) yield "self."+f.getName).mkString("Some(", ", ", ")")}

  final class SubType private[$packageName] (val τName : String, skillID : Long) extends $Name(skillID) with NamedType{
    override def prettyString : String = τName+$prettyStringArgs
    override def toString = τName+"#"+skillID
  }
}
""");
    }

    out.close()
  }
}
