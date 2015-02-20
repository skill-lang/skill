/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import scala.collection.JavaConversions.asScalaBuffer
import de.ust.skill.generator.java.GeneralOutputMaker
import de.ust.skill.ir.restriction.SingletonRestriction
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.ListType
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Type
import de.ust.skill.ir.MapType
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.FloatRangeRestriction
import de.ust.skill.ir.restriction.NullableRestriction

trait AccessMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← IR) {
      val isBasePool = (null == t.getSuperType)
      val nameT = name(t)
      val typeT = mapType(t)

      val out = open(s"internal/${nameT}Access.java")
      //package & imports
      out.write(s"""package ${packagePrefix}internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import de.ust.skill.common.java.internal.*;
import de.ust.skill.common.java.internal.fieldTypes.*;
import de.ust.skill.common.java.restrictions.FieldRestriction;
""")

      //class declaration
      out.write(s"""
${
        comment(t)
      }public class ${nameT}Access extends ${
        if (isBasePool) s"BasePool<${typeT}>"
        else s"SubPool<${typeT}, ${mapType(t.getBaseType)}>"
      } {
${
        if (isBasePool) s"""
    // TODO optimize this method away by replacing empty arrays by null pointers
    @Override
    protected $typeT[] emptyArray() {
        return new $typeT[0];
    }
"""
        else ""
      }
    /**
     * Can only be constructed by the SkillFile in this package.
     */
    ${nameT}Access(long poolIndex${
        if (isBasePool) ""
        else s", ${name(t.getSuperType)}Access superPool"
      }) {
        super(poolIndex, "${t.getSkillName}"${
        if (isBasePool) ""
        else ", superPool"
      }, new HashSet<String>(Arrays.asList(new String[] { ${t.getFields.map { f ⇒ s""""${f.getSkillName}"""" }.mkString(", ")} })));
    }${
        // export data for sub pools
        if (isBasePool) s"""

    final ${mapType(t.getBaseType)}[] data() {
        return data;
    }"""
        else ""
      }

    @Override
    public boolean insertInstance(int skillID) {${
        if (isBasePool) ""
        else s"""
        ${mapType(t.getBaseType)}[] data = ((${name(t.getBaseType)}Access)basePool).data();"""
      }
        int i = skillID - 1;
        if (null != data[i])
            return false;

        $typeT r = new $typeT(skillID);
        data[i] = r;
        staticData.add(r);
        return true;
    }
${
        if (t.getFields.isEmpty()) ""
        else s"""
    @SuppressWarnings("unchecked")
    @Override
    public void addKnownField(String name, de.ust.skill.common.java.internal.fieldTypes.StringType string, de.ust.skill.common.java.internal.fieldTypes.Annotation annotation) {
        final FieldDeclaration<?, $typeT> f;
        switch (name) {${
          (for (f ← t.getFields)
            yield s"""
        case "${f.getSkillName}":
            f = new KnownField_${nameT}_${name(f)}(${mapToFieldType(f.getType)}${
            if (f.isAuto()) ""
            else ", fields.size()"
          }, this);
            break;
"""
          ).mkString
        }
        default:
            super.addKnownField(name, string, annotation);
            return;
        }
        fields.add(f);
    }"""
      }${
        if (t.getFields.forall(_.isAuto)) ""
        else s"""

    @SuppressWarnings("unchecked")
    @Override
    public <R> FieldDeclaration<R, $typeT> addField(int ID, FieldType<R> type, String name,
            HashSet<FieldRestriction<?>> restrictions) {
        final FieldDeclaration<R, $typeT> f;
        switch (name) {${
          (for (f ← t.getFields if !f.isAuto)
            yield s"""
        case "${f.getSkillName}":
            f = (FieldDeclaration<R, $typeT>) new KnownField_${nameT}_${name(f)}((FieldType<${mapType(f.getType, true)}>) type, ID, this);
            break;
"""
          ).mkString
        }
        default:
            return super.addField(ID, type, name, restrictions);
        }

        for (FieldRestriction<?> r : restrictions)
            f.addRestriction(r);
        fields.add(f);
        return f;
    }"""
      }

    /**
     * @return a new $nameT instance with default field values
     */
    public $typeT make() {
        $typeT rval = new $typeT();
        add(rval);
        return rval;
    }
${
        if (t.getAllFields.filterNot { f ⇒ f.isConstant() || f.isIgnored() }.isEmpty) ""
        else s"""
    /**
     * @return a new age instance with the argument field values
     */
    public $typeT make(${makeConstructorArguments(t)}) {
        $typeT rval = new $typeT(-1${appendConstructorArguments(t, false)});
        add(rval);
        return rval;
    }
"""
      }
    public ${nameT}Builder build() {
        return new ${nameT}Builder(this, new $typeT());
    }

    /**
     * Builder for new $nameT instances.
     * 
     * @author Timm Felden
     */
    public static final class ${nameT}Builder extends Builder<$typeT> {

        protected ${nameT}Builder(StoragePool<$typeT, ? super $typeT> pool, $typeT instance) {
            super(pool, instance);
        }${
        (for (f ← t.getAllFields if !f.isIgnored() && !f.isConstant())
          yield s"""

        public ${nameT}Builder ${name(f)}(${mapType(f.getType)} ${name(f)}) {
            instance.set${f.getName.capital()}(${name(f)});
            return this;
        }""").mkString
      }
    }
}
""")

      out.close()
    }
  }

  private def mapToFieldType(t : Type) : String = {
    //@note temporary string & annotation will be replaced later on
    def mapGroundType(t : Type) = t.getSkillName match {
      case "annotation" ⇒ "Annotation.tmp()"
      case "bool"       ⇒ "BoolType.get()"
      case "i8"         ⇒ "I8.get()"
      case "i16"        ⇒ "I16.get()"
      case "i32"        ⇒ "I32.get()"
      case "i64"        ⇒ "I64.get()"
      case "v64"        ⇒ "V64.get()"
      case "f32"        ⇒ "F32.get()"
      case "f64"        ⇒ "F64.get()"
      case "string"     ⇒ "StringType.tmp()"

      case s            ⇒ s"""(FieldType<${mapType(t)}>)(owner().poolByName().get("${t.getSkillName}"))"""
    }

    t match {
      case t : GroundType              ⇒ mapGroundType(t)
      case t : ConstantLengthArrayType ⇒ s"new ConstantLengthArray<>(${t.getLength}, ${mapGroundType(t.getBaseType)})"
      case t : VariableLengthArrayType ⇒ s"new VariableLengthArray<>(${mapGroundType(t.getBaseType)})"
      case t : ListType                ⇒ s"new ListType<>(${mapGroundType(t.getBaseType)})"
      case t : SetType                 ⇒ s"new SetType<>(${mapGroundType(t.getBaseType)})"
      case t : MapType                 ⇒ t.getBaseTypes().map(mapGroundType).reduceRight((k, v) ⇒ s"new MapType<>($k, $v)")
      case t : Declaration             ⇒ s"""(FieldType<${mapType(t)}>)(owner().poolByName().get("${t.getSkillName}"))"""
    }
  }

  private def mkFieldRestrictions(f : Field) : String = {
    f.getRestrictions.map(_ match {
      case r : NullableRestriction ⇒ s"_root_.${packagePrefix}internal.restrictions.NonNull"
      case r : IntRangeRestriction ⇒ s"_root_.${packagePrefix}internal.restrictions.Range(${r.getLow}L.to${mapType(f.getType)}, ${r.getHigh}L.to${mapType(f.getType)})"
      case r : FloatRangeRestriction ⇒ f.getType.getSkillName match {
        case "f32" ⇒ s"_root_.${packagePrefix}internal.restrictions.Range(${r.getLowFloat}f, ${r.getHighFloat}f)"
        case "f64" ⇒ s"_root_.${packagePrefix}internal.restrictions.Range(${r.getLowDouble}, ${r.getHighDouble})"
      }
    }).mkString(", ")
  }
}
