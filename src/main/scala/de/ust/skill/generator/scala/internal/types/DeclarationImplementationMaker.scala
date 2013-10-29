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

final class $name(
  """)

    // data
    out.write(fields.map({ f ⇒ s"private var _${f.getName()}: ${mapType(f.getType())}" }).toArray.mkString("", ",\n  ", ""))

    out.write(s""")
    extends _root_.${packagePrefix}$name {
  def this() {
    this(${fields.map(defaultValue(_)).mkString(", ")})
  }
""")

    // getters & setters
    fields.foreach({ f ⇒
      val name = f.getName()
      val Name = name.capitalize
      if ("annotation".equals(f.getType().getName())) {
        out.write(s"""
  override final def get$Name[T <: SkillType]()(implicit m: ClassTag[T]): T = {
    if (!m.runtimeClass.isAssignableFrom(_$name.getClass()))
      AnnotationTypeCastException(s"annotation access: $${m.runtimeClass} vs. $${_$name.getClass}", null)
    _$name.asInstanceOf[T]
  }
  override final def set$Name[T <: SkillType]($Name: T): Unit = _$name = $Name
""")
      } else {
        if(f.getType().isInstanceOf[ReferenceType] && f.getRestrictions().collect({case r:NullableRestriction⇒r}).isEmpty){
          // the setter shall check the non-null property
          out.write(s"""
  override final def get$Name = _$name
  override final def set$Name($Name: ${mapType(f.getType())}) = { require($Name != null, "$name is specified to be nonnull!"); _$name = $Name }
""")
        } else {      
          out.write(s"""
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
