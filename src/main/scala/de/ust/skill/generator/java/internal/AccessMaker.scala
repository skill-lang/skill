/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.java.GeneralOutputMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.restriction.FloatRangeRestriction
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.NonNullRestriction
import de.ust.skill.ir.UserType
import de.ust.skill.io.PrintWriter

trait AccessMaker extends GeneralOutputMaker {
  final def makePools(out : PrintWriter) {

    // reflection has to know projected definitions
    val flatIR = this.types.removeSpecialDeclarations.getUsertypes

    for (t ← IR) {
      val isBasePool = (null == t.getSuperType)
      val nameT = name(t)
      val typeT = mapType(t)
      val accessT = access(t)

      // find all fields that belong to the projected version, but use the unprojected variant
      val flatIRFieldNames = flatIR.find(_.getName == t.getName).get.getFields.map(_.getSkillName).toSet
      val fields = t.getAllFields.filter(f ⇒ flatIRFieldNames.contains(f.getSkillName))
      val projectedField = flatIR.find(_.getName == t.getName).get.getFields.map {
        case f ⇒ fields.find(_.getSkillName.equals(f.getSkillName)).get -> f
      }.toMap


      //class declaration
      out.write(s"""
${
        comment(t)
      }public static final class $accessT extends ${
        if (isBasePool) s"BasePool<${typeT}>"
        else s"StoragePool<${typeT}, ${mapType(t.getBaseType)}>"
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
    $accessT(int poolIndex${
        if (isBasePool) ""
        else s", ${access(t.getSuperType)} superPool"
      }) {
        super(poolIndex, "${t.getSkillName}"${
        if (isBasePool) ""
        else ", superPool"
      }, ${
        if (fields.isEmpty) "noKnownFields"
        else fields.map { f ⇒ s""""${f.getSkillName}"""" }.mkString("new String[] { ", ", ", " }")
      }, ${
        fields.count(_.isAuto) match {
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
    protected void allocateInstances(Block last) {
        int i = (int) last.bpo;
        final int high = (int) (i + last.staticCount);
        while (i < high) {
            data[i] = new $typeT(i + 1);
            i += 1;
        }
    }
${
        if (fields.isEmpty) ""
        else s"""
    @SuppressWarnings("unchecked")
    @Override
    public void addKnownField(
        String name,
        de.ust.skill.common.java.internal.StringPool string,
        de.ust.skill.common.java.internal.fieldTypes.Annotation annotation) {

        switch (name) {${
          (for (f ← fields)
            yield s"""
        case "${f.getSkillName}":
            new ${knownField(projectedField(f))}(${mapToFieldType(f)}, this);
            return;
"""
          ).mkString
        }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> FieldDeclaration<R, $typeT> addField(FieldType<R> type, String name) {
        switch (name) {${
          (for (f ← fields if !f.isAuto)
            yield s"""
        case "${f.getSkillName}":
            return (FieldDeclaration<R, $typeT>) new ${knownField(projectedField(f))}((FieldType<${mapType(f.getType, true)}>) type, this);
"""
          ).mkString
        }${
          (for (f ← fields if f.isAuto)
            yield s"""
        case "${f.getSkillName}":
            throw new SkillException(String.format(
                    "The file contains a field declaration %s.%s, but there is an auto field of similar name!",
                    this.name(), name));
"""
          ).mkString
        }
        default:
            return super.addField(type, name);
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
        if (fields.filterNot { f ⇒ f.isConstant() || f.isIgnored() }.isEmpty) ""
        else s"""
    /**
     * @return a new $typeT instance with the argument field values
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

        @Override
        public $typeT make() {
            pool.add(instance);
            $typeT rval = instance;
            instance = null;
            return rval;
        }
    }

    /**
     * used internally for type forest construction
     */
    @Override
    public StoragePool<? extends ${mapType(t)}, ${mapType(t.getBaseType)}> makeSubPool(int index, String name) {
        return new UnknownSubPool(index, name, this);
    }

    private static final class UnknownSubPool extends StoragePool<${mapType(t)}.SubType, ${mapType(t.getBaseType)}> {
        UnknownSubPool(int poolIndex, String name, StoragePool<? super ${mapType(t)}.SubType, ${mapType(t.getBaseType)}> superPool) {
            super(poolIndex, name, superPool, noKnownFields, noAutoFields());
        }

        @Override
        public StoragePool<? extends ${mapType(t)}.SubType, ${mapType(t.getBaseType)}> makeSubPool(int index, String name) {
            return new UnknownSubPool(index, name, this);
        }

        @Override
        protected void allocateInstances(Block last) {
            int i = (int) last.bpo;
            final int high = (int) (i + last.staticCount);
            while (i < high) {
                data[i] = new ${mapType(t)}.SubType(this, i + 1);
                i += 1;
            }
        }
    }
}
""")
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

      case _            ⇒ s"""((SkillState)owner()).${name(t)}s()"""
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

      case t ⇒ s"""((SkillState)owner()).${name(t)}s()"""

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
