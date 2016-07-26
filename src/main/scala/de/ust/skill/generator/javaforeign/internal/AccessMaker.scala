/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.javaforeign.internal

import scala.collection.JavaConversions._

import de.ust.skill.generator.javaforeign.GeneralOutputMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.restriction.FloatRangeRestriction
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.NonNullRestriction

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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import de.ust.skill.common.jforeign.api.SkillException;
import de.ust.skill.common.jforeign.internal.*;
import de.ust.skill.common.jforeign.internal.fieldDeclarations.AutoField;
import de.ust.skill.common.jforeign.internal.fieldTypes.*;
import de.ust.skill.common.jforeign.internal.parts.Block;
import de.ust.skill.common.jforeign.restrictions.FieldRestriction;
""")

      //class declaration
      out.write(s"""
${
        comment(t)
      }${
        suppressWarnings
      }public class ${nameT}Access extends ${
        if (isBasePool) s"BasePool<${typeT}>"
        else s"SubPool<${typeT}, ${mapType(t.getBaseType)}>"
      } {
${
        if (isBasePool) s"""
    @Override
    final protected $typeT[] newArray(int size) {
        return new $typeT[size];
    }
"""
        else ""
      }
    /**
     * Can only be constructed by the SkillFile in this package.
     */
    ${nameT}Access(int poolIndex${
        if (isBasePool) ""
        else s", ${name(t.getSuperType)}Access superPool"
      }) {
        super(poolIndex, "${t.getSkillName}"${
        if (isBasePool) ""
        else ", superPool"
      }, new HashSet<String>(Arrays.asList(new String[] { ${
        t.getFields.map { f ⇒ s""""${f.getSkillName}"""" }.mkString(", ")
      } })), ${
        t.getFields.count(_.isAuto) match {
          case 0 ⇒ "noAutoFields()"
          case c ⇒ s"(AutoField<?, ${mapType(t)}>[]) java.lang.reflect.Array.newInstance(AutoField.class, $c)"
        }
      });
    }${
        // export data for sub pools
        if (isBasePool) s"""

    final ${mapType(t.getBaseType)}[] data() {
        return data;
    }"""
        else ""
      }

    @Override
    public void insertInstances() {${
        if (isBasePool) ""
        else s"""
        ${mapType(t.getBaseType)}[] data = ((${name(t.getBaseType)}Access)basePool).data();"""
      }
        final Block last = blocks().getLast();
        int i = (int) last.bpo;
        int high = (int) (last.bpo + last.count);
        while (i < high) {
            if (null != data[i])
                return;

            $typeT r = new $typeT(i + 1);
            data[i] = r;
            staticData.add(r);

            i += 1;
        }
    }
${
        if (t.getFields.isEmpty()) ""
        else s"""
    @SuppressWarnings("unchecked")
    @Override
    public void addKnownField(
        String name,
        de.ust.skill.common.jforeign.internal.fieldTypes.StringType string,
        de.ust.skill.common.jforeign.internal.fieldTypes.Annotation annotation) {

        final FieldDeclaration<?, $typeT> f;
        switch (name) {${
          (for (f ← t.getFields if !f.isAuto)
            yield s"""
        case "${f.getSkillName}":
            f = new KnownField_${nameT}_${name(f)}(${mapToFieldType(f)}, 1 + dataFields.size(), this);
            break;
"""
          ).mkString
        }${
          var index = 0;
          (for (f ← t.getFields if f.isAuto)
            yield s"""
        case "${f.getSkillName}":
            f = new KnownField_${nameT}_${name(f)}(${mapToFieldType(f)}, this);
            autoFields[${index += 1; index - 1}] = (AutoField<?, $typeT>) f;
            break;
"""
          ).mkString
        }
        default:
            super.addKnownField(name, string, annotation);
            return;
        }
        if (!(f instanceof AutoField))
            dataFields.add(f);
    }

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
        }${
          (for (f ← t.getFields if f.isAuto)
            yield s"""
        case "${f.getSkillName}":
            throw new SkillException(String.format(
                    "The file contains a field declaration %s.%s, but there is an auto field of similar name!",
                    this.name(), name));
"""
          ).mkString
        }
        default:
            return super.addField(ID, type, name, restrictions);
        }${
          if (t.getFields.forall(_.isAuto())) ""
          else """

        for (FieldRestriction<?> r : restrictions)
            f.addRestriction(r);
        dataFields.add(f);
        return f;"""
        }
    }"""
      }

    /**
     * @return a new $nameT instance with default field values
     */
    @Override
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
            instance.set${escaped(f.getName.capital)}(${name(f)});
            return this;
        }""").mkString
      }
    }

    /**
     * used internally for type forest construction
     */
    @Override
    public StoragePool<? extends ${mapType(t)}, ${mapType(t.getBaseType)}> makeSubPool(int index, String name) {
        return new UnknownSubPool(index, name, this);
    }

    private static final class UnknownSubPool extends SubPool<${mapType(t)}.SubType, ${mapType(t.getBaseType)}> {
        UnknownSubPool(int poolIndex, String name, StoragePool<? super ${mapType(t)}.SubType, ${mapType(t.getBaseType)}> superPool) {
            super(poolIndex, name, superPool, Collections.emptySet(), noAutoFields());
        }

        @Override
        public StoragePool<? extends ${mapType(t)}.SubType, ${mapType(t.getBaseType)}> makeSubPool(int index, String name) {
            return new UnknownSubPool(index, name, this);
        }

        @Override
        public void insertInstances() {
            final Block last = lastBlock();
            int i = (int) last.bpo;
            int high = (int) (last.bpo + last.count);
            ${mapType(t.getBaseType)}[] data = ((${name(t.getBaseType)}Access) basePool).data();
            while (i < high) {
                if (null != data[i])
                    return;

                @SuppressWarnings("unchecked")
                ${mapType(t)}.SubType r = new ${mapType(t)}.SubType(this, i + 1);
                data[i] = r;
                staticData.add(r);

                i += 1;
            }
        }
    }

    /**
     * punch a hole into the java type system :)
     */
    @SuppressWarnings("unchecked")
    static <T, U> FieldType<T> cast(FieldType<U> f) {
        return (FieldType<T>) f;
    }
}
""")

      out.close()
    }
  }

  private def mapToFieldType(f : Field) : String = {
    //@note temporary string & annotation will be replaced later on
    @inline def mapGroundType(t : Type) : String = t.getSkillName match {
      case "annotation" ⇒ "annotation"
      case "bool"       ⇒ "BoolType.get()"
      case "i8"         ⇒ if (f.isConstant) s"new ConstantI8((byte)${f.constantValue})" else "I8.get()"
      case "i16"        ⇒ if (f.isConstant) s"new ConstantI16((short)${f.constantValue})" else "I16.get()"
      case "i32"        ⇒ if (f.isConstant) s"new ConstantI32(${f.constantValue})" else "I32.get()"
      case "i64"        ⇒ if (f.isConstant) s"new ConstantI64(${f.constantValue}L)" else "I64.get()"
      case "v64"        ⇒ if (f.isConstant) s"new ConstantV64(${f.constantValue}L)" else "V64.get()"
      case "f32"        ⇒ "F32.get()"
      case "f64"        ⇒ "F64.get()"
      case "string"     ⇒ "string"

      case s ⇒ t match {
        case t : InterfaceType ⇒ s"cast(${mapGroundType(t.getSuperType)})"
        case _                 ⇒ s"""(FieldType<${mapType(t)}>)(owner().poolByName().get("${t.getSkillName}"))"""
      }
    }

    f.getType match {
      case t : GroundType ⇒ mapGroundType(t)
      case t : ConstantLengthArrayType ⇒
        s"new ConstantLengthArray<>(${t.getLength}, ${mapGroundType(t.getBaseType)})"

      case t : VariableLengthArrayType ⇒
        s"new VariableLengthArray<>(${mapGroundType(t.getBaseType)})"

      case t : ListType ⇒
        s"new ListType<>(${mapGroundType(t.getBaseType)})"

      case t : SetType ⇒
        s"new SetType<>(${mapGroundType(t.getBaseType)})"

      case t : MapType ⇒
        t.getBaseTypes().map(mapGroundType).reduceRight((k, v) ⇒ s"new MapType<>($k, $v)")

      case t : InterfaceType ⇒ s"cast(${mapGroundType(t.getSuperType)})"
      case t : Declaration ⇒
        s"""(FieldType<${mapType(t)}>)(owner().poolByName().get("${t.getSkillName}"))"""

    }
  }

  private def mkFieldRestrictions(f : Field) : String = {
    f.getRestrictions.map(_ match {
      case r : NonNullRestriction ⇒ s"_root_.${packagePrefix}internal.restrictions.NonNull"
      case r : IntRangeRestriction ⇒
        s"_root_.${packagePrefix}internal.restrictions.Range(${
          r.getLow
        }L.to${mapType(f.getType)}, ${r.getHigh}L.to${mapType(f.getType)})"

      case r : FloatRangeRestriction ⇒ f.getType.getSkillName match {
        case "f32" ⇒ s"_root_.${packagePrefix}internal.restrictions.Range(${r.getLowFloat}f, ${r.getHighFloat}f)"
        case "f64" ⇒ s"_root_.${packagePrefix}internal.restrictions.Range(${r.getLowDouble}, ${r.getHighDouble})"
      }
    }).mkString(", ")
  }
}
