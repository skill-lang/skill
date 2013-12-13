package de.ust.skill.generator.scala.internal.types

import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.Type
import de.ust.skill.ir.restriction.NullableRestriction
import de.ust.skill.ir.ReferenceType
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.FloatRangeRestriction
import de.ust.skill.ir.restriction.MonotoneRestriction

trait DeclarationImplementationMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    IR.foreach({ d ⇒
      makeDeclaration(open("internal/types/"+d.getName()+".scala"), d)
    })
  }

  private def makeDeclaration(out: PrintWriter, d: Declaration) {
    val name = d.getName()
    val fields = d.getAllFields.filter(!_.isConstant)
    val relevantFields = fields.filter(!_.isIgnored)

    // head
    out.write(s"""package ${packagePrefix}internal.types

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

import ${packagePrefix}api._
import ${packagePrefix}internal.AnnotationTypeCastException

final class $name extends _root_.${packagePrefix}$name {""")

	if(!relevantFields.isEmpty){
		out.write("""
  @inline def this(""")

  		// data
    	out.write(relevantFields.map({ f ⇒ s"${escaped(f.getName)} : ${mapType(f.getType())}" }).mkString(", "))

    	out.write(s""") {
    this()
    ${relevantFields.map{f ⇒ s"_${f.getName()} = ${escaped(f.getName)}"}.mkString("\n    ")}
  }""")
	}
    out.write(s"""
  private[internal] var skillID = -1L
  override final def getSkillID = skillID
  private[internal] def setSkillID(newID: Long) = { ${// @monotone delete-check
      if(!d.getRestrictions.collect{case r:MonotoneRestriction⇒r}.isEmpty)
        s"""require(newID != 0L || newID != -1L || skillID == -1L || skillID == 0L, "${d.getName} is specified to be monotone and this instance has already been subject to serialization!"); """
      else
        ""
    }skillID = newID }
""")

	///////////////////////
	// getters & setters //
	///////////////////////
	fields.foreach({ f ⇒
      val name = f.getName()
      val name_ = escaped(name)
      val Name = name.capitalize

      def makeField:String = {
		if(f.isIgnored)
		  ""
		else
	      s"""
  private var _${f.getName}: ${mapType(f.getType())} = ${defaultValue(f)}"""
	  }

      def makeGetterImplementation:String = {
        if(f.isIgnored)
          s"""throw new IllegalAccessError("$name has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")"""
        else if("annotation".equals(f.getType().getName()))
          s"""{
    if (m.runtimeClass.isAssignableFrom(_$name.getClass()))
      _$name.asInstanceOf[T]
    else
      throw AnnotationTypeCastException(s"annotation access: $${m.runtimeClass} vs. $${_$name.getClass}", null)
  }"""
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
            if(!d.getRestrictions.collect{case r:MonotoneRestriction⇒r}.isEmpty){
              s"""require(skillID == -1L, "${d.getName} is specified to be monotone and this instance has already been subject to serialization!"); """
            }
            else
              ""
        }_$name = $Name }"
      }

      if ("annotation".equals(f.getType().getName())) { 
        out.write(s"""$makeField
  override final def $name_[T <: SkillType]()(implicit m: ClassTag[T]): T = $makeGetterImplementation
  override final def ${name_}_=[T <: SkillType]($Name: T): Unit = $makeSetterImplementation
""")
      } else {
        out.write(s"""$makeField
  override final def $name_ = $makeGetterImplementation
  override final def ${name_}_=($Name: ${mapType(f.getType())}) = $makeSetterImplementation
""")
      }
    });

    // pretty string
    out.write(s"""  override def prettyString(): String = "${d.getName()}(this: "+this""")
    d.getAllFields.foreach({ f ⇒
      if(f.isIgnored) out.write(s"""+", ${f.getName()}: <<ignored>>" """)
      else if (!f.isConstant) out.write(s"""+", ${if(f.isAuto)"auto "else""}${f.getName()}: "+_${f.getName()}""")
      else out.write(s"""+", const ${f.getName()}: ${f.constantValue()}"""")
    })
    out.write("+\")\"")

    // toString
    out.write(s"""
  override def toString = "$name#"+skillID
}
""")
    out.close()
  }
}
