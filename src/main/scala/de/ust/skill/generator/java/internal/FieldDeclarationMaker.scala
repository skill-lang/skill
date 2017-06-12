/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.java.GeneralOutputMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import jdk.nashorn.internal.codegen.CompilerConstants.FieldAccess
import de.ust.skill.ir.Field
import scala.collection.mutable.ArrayBuffer

trait FieldDeclarationMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    // project IR, so that reflection implements the binary file format correctly
    val IR = this.types.removeSpecialDeclarations.getUsertypes

    for (t ← IR) {
      val autoFieldIndex : Map[Field, Int] = t.getFields.filter(_.isAuto()).zipWithIndex.toMap

      for (f ← t.getFields) {
        // the field before interface projection
        val originalF = this.types.removeTypedefs.removeEnums.get(t.getSkillName).asInstanceOf[UserType]
          .getAllFields.find(_.getName == f.getName).get

        // the type before the interface projection
        val fieldActualType = mapType(originalF.getType, true)
        val fieldActualTypeUnboxed = mapType(originalF.getType, false)

        val tIsBaseType = t.getSuperType == null

        val nameT = mapType(t)
        val nameF = knownField(f)

        // casting access to data array using index i
        val dataAccessI = if (null == t.getSuperType) "data[i]" else s"((${mapType(t)})data[i])"
        val fieldAccess = s"""get${escaped(f.getName.capital)}()"""

        val out = files.open(s"internal/$nameF.java")
        //package
        out.write(s"""package ${packagePrefix}internal;

import java.util.ArrayList;
import java.lang.reflect.Field;

import de.ust.skill.common.java.internal.*;
import de.ust.skill.common.java.internal.fieldDeclarations.*;

""")

        out.write(s"""
/**
 * ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
 */
${
          suppressWarnings
        }final class $nameF extends ${
          if (f.isAuto) "AutoField"
          else "KnownDataField"
        }<$fieldActualType, ${mapType(t)}>${
          var interfaces = new ArrayBuffer[String]()

          // mark ignored fields as ignored; read function is inherited
          if (f.isIgnored()) interfaces += "IgnoredField"

          // mark interface fields
          if (f.getType.isInstanceOf[InterfaceType]) interfaces += "InterfaceField"

          if (interfaces.isEmpty) ""
          else interfaces.mkString(" implements ", ", ", "")
        } {

    public $nameF(FieldType<$fieldActualType> type, ${access(t)} owner) {
        super(type, "${f.getSkillName}"${
          if (f.isAuto()) ", " + -autoFieldIndex(f)
          else ""
        }, owner);
            // TODO insert known restrictions?
    }
${
          if (f.isAuto) ""
          else if (f.isConstant) """
    @Override
    protected final Field javaField() {
        return null;
    }
"""
          else s"""
    @Override
    protected final Field javaField() throws Exception {
        return ${nameT}.class.getDeclaredField("${name(f)}");
    }
"""
        }
    @Override
    public $fieldActualType get(SkillObject ref) {
        ${
          if (f.isConstant()) s"return ${mapType(t)}.get${escaped(f.getName.capital)}();"
          else s"return ((${mapType(t)}) ref).get${escaped(f.getName.capital)}();"
        }
    }

    @Override
    public void set(SkillObject ref, $fieldActualType value) {
        ${
          if (f.isConstant()) s"""throw new IllegalAccessError("${f.getName.camel} is a constant!");"""
          else s"((${mapType(t)}) ref).set${escaped(f.getName.capital)}(value);"
        }
    }
}
""")
        out.close()
      }
    }
  }

  /**
   * creates code to write exactly one field element
   */
  private final def writeCode(t : UserType, f : Field) : String = {
    val dataAccessI = if (null == t.getSuperType) "d[i]" else s"((${mapType(t)})d[i])"
    val fieldAccess = s""".get${escaped(f.getName.capital)}()"""

    f.getType match {
      case t : GroundType ⇒ t.getSkillName match {
        case "annotation" | "string" ⇒ s"""type.writeSingleField($dataAccessI$fieldAccess, out);"""
        case _                       ⇒ s"""out.${t.getSkillName}($dataAccessI$fieldAccess);"""
      }

      case t : UserType ⇒ s"""${mapType(t)} v = $dataAccessI$fieldAccess;
            if (null == v)
                out.i8((byte) 0);
            else
                out.v64(v.getSkillID());"""
      case _ ⇒ s"""type.writeSingleField($dataAccessI$fieldAccess, out);"""
    }
  }
}
