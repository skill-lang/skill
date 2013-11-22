package de.ust.skill.generator.scala.internal.types

import java.io.PrintWriter
import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.scala.GeneralOutputMaker
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.Type
import de.ust.skill.ir.restriction.NullableRestriction
import de.ust.skill.ir.ReferenceType

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

    // head
    out.write(s"""package ${packagePrefix}internal.types

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

import ${packagePrefix}api._
import ${packagePrefix}internal.AnnotationTypeCastException

final class $name extends _root_.${packagePrefix}$name {
  @inline def this(""")

    // data
    out.write(fields.map({ f ⇒ s"${escaped(f.getName)} : ${mapType(f.getType())}" }).mkString(", "))

    out.write(s""") {
    this()
    ${fields.map{f ⇒ s"_${f.getName()} = ${escaped(f.getName)}"}.mkString("\n    ")}
  }

  private[internal] var skillID = -1L
  override final def getSkillID = skillID
  private[internal] def setSkillID(newID: Long) = skillID = newID
""")

    // getters & setters
    fields.foreach({ f ⇒
      val name = f.getName()
      val Name = name.capitalize
      if ("annotation".equals(f.getType().getName())) {
        if(f.isIgnored)
          out.write(s"""
  override final def get$Name[T <: SkillType]()(implicit m: ClassTag[T]): T = throw new IllegalAccessError("$name has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")
  override final def set$Name[T <: SkillType]($Name: T): Unit = throw new IllegalAccessError("$name has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")
""")
        else 
        out.write(s"""
  private var _${f.getName}: ${mapType(f.getType())} = ${defaultValue(f)}
  override final def get$Name[T <: SkillType]()(implicit m: ClassTag[T]): T = {
    if (m.runtimeClass.isAssignableFrom(_$name.getClass()))
      _$name.asInstanceOf[T]
    else
      throw AnnotationTypeCastException(s"annotation access: $${m.runtimeClass} vs. $${_$name.getClass}", null)
  }
  override final def set$Name[T <: SkillType]($Name: T): Unit = _$name = $Name
""")

      } else {
        
        if(f.isIgnored){
          out.write(s"""
  override final def get$Name = throw new IllegalAccessError("$name has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")
  override final def set$Name($Name: ${mapType(f.getType())}) = throw new IllegalAccessError("$name has ${if(f.hasIgnoredType)"a type with "else""}an !ignore hint")
""")
      } else if(f.getType().isInstanceOf[ReferenceType] && f.getRestrictions().collect({case r:NullableRestriction⇒r}).isEmpty){
          // the setter shall check the non-null property
          out.write(s"""
  private var _${f.getName}: ${mapType(f.getType())} = ${defaultValue(f)}
  override final def get$Name = _$name
  override final def set$Name($Name: ${mapType(f.getType())}) = { require($Name != null, "$name is specified to be nonnull!"); _$name = $Name }
""")
        } else {      
          out.write(s"""
  private var _${f.getName}: ${mapType(f.getType())} = ${defaultValue(f)}
  override final def get$Name = _$name
  override final def set$Name($Name: ${mapType(f.getType())}) = _$name = $Name
""")
        }
      }
    });

    // pretty string
    out.write(s"""  override def prettyString(): String = "${d.getName()}(this: "+this""")
    d.getAllFields.foreach({ f ⇒
      if (!f.isConstant()) out.write(s"""+", ${if(f.isAuto)"auto "else""}${f.getName()}: "+_${f.getName()}""")
      else out.write(s"""+", const ${f.getName()}: ${f.constantValue()}"""")
    })
    out.write("+\")\"\n")

    out.write("}")
    out.close()
  }
}
