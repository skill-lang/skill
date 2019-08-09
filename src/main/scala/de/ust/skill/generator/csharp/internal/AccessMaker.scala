/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp.internal

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.csharp.GeneralOutputMaker
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
              }public sealed class $accessT : ${
                if (isBasePool) s"BasePool<${typeT}>"
                else s"StoragePool<${typeT}, ${mapType(t.getBaseType)}>"
              } {
        ${
                if (isBasePool) s"""
            protected override $typeT[] newArray(int size) {
                return new $typeT[size];
            }
"""
        else ""
      }
            /// <summary>
            /// Can only be constructed by the SkillFile in this package.
            /// </summary>
            internal $accessT(int poolIndex${
                if (isBasePool) ""
                else s", ${access(t.getSuperType)} superPool"
              }) : base(poolIndex, "${t.getSkillName}"${
                if (isBasePool) ""
                else ", superPool"
              }, ${
                if (fields.isEmpty) "noKnownFields"
                else fields.map { f ⇒ s""""${f.getSkillName}"""" }.mkString("new string[] { ", ", ", " }")
              }, ${
                fields.count(_.isAuto) match {
                  case 0 ⇒ "NoAutoFields"
                  case c ⇒ s"new IAutoField[$c]"
                }
              }) {

            }${
                // export data for sub pools
                if (isBasePool) s"""

            internal ${mapType(t.getBaseType)}[] Data {
                get
                {
                    return (${mapType(t.getBaseType)}[])data;
                }
            }"""
                else ""
              }

            public override void allocateInstances(Block last) {
                int i = (int) last.bpo;
                int high = (int) (i + last.staticCount);
                while (i < high) {
                    data[i] = new $typeT(i + 1);
                    i += 1;
                }
            }
        ${
                if (fields.isEmpty) ""
                else s"""
            public override void addKnownField(string name, StringType @string, Annotation annotation) {

                switch (name) {${
                  (for (f ← fields)
                    yield s"""
                case "${f.getSkillName}":
                    unchecked{new ${knownField(projectedField(f))}(${mapToFieldType(f)}, this);}
                    return;
"""
          ).mkString
        }
                }
            }

            public override AbstractFieldDeclaration addField<R> (de.ust.skill.common.csharp.@internal.FieldType type, string name) {
                switch (name) {${
                  (for (f ← fields if !f.isAuto)
                    yield s"""
                case "${f.getSkillName}":
                    return new ${knownField(projectedField(f))}((de.ust.skill.common.csharp.@internal.FieldType) type, this);
"""
          ).mkString
                }${
                  (for (f ← fields if f.isAuto)
                    yield s"""
                case "${f.getSkillName}":
                    throw new SkillException(String.Format(
                            "The file contains a field declaration %s.%s, but there is an auto field of similar name!",
                            this.Name, name));
"""
          ).mkString
        }
                default:
                    return base.addField<R>(type, name);
                }
            }"""
      }

            /// <returns> a new $nameT instance with default field values </returns>
            public override object make() {
                $typeT rval = new $typeT();
                add(rval);
                return rval;
            }
        ${
                if (fields.filterNot { f ⇒ f.isConstant() || f.isIgnored() }.isEmpty) ""
                else s"""
            /// <returns> a new $typeT instance with the argument field values </returns>
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

            /// <summary>
            /// Builder for new $nameT instances.
            ///
            /// @author Simon Glaub, Timm Felden
            /// </summary>
            public sealed class ${nameT}Builder : Builder<$typeT> {

                public ${nameT}Builder(AbstractStoragePool pool, $typeT instance) : base(pool, instance) {

                }${
                (for (f ← t.getAllFields if !f.isIgnored() && !f.isConstant())
                  yield s"""

                public ${nameT}Builder ${name(f)}(${mapType(f.getType)} ${name(f)}) {
                    instance.${name(f)} = ${name(f)};
                    return this;
                }""").mkString
              }

                public override $typeT make() {
                    pool.add(instance);
                    $typeT rval = instance;
                    instance = null;
                    return rval;
                }
            }

            /// <summary>
            /// used internally for type forest construction
            /// </summary>
            public override AbstractStoragePool makeSubPool(int index, string name) {
                return new UnknownSubPool(index, name, this);
            }

            private sealed class UnknownSubPool : StoragePool<${mapType(t)}.SubType, ${mapType(t.getBaseType)}> {
                internal UnknownSubPool(int poolIndex, string name, AbstractStoragePool superPool) : base(poolIndex, name, superPool, noKnownFields, NoAutoFields){

                }

                public override AbstractStoragePool makeSubPool(int index, string name) {
                    return new UnknownSubPool(index, name, this);
                }

                public override void allocateInstances(Block last) {
                    int i = (int) last.bpo;
                    int high = (int)(i + last.staticCount);
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
      case "i8"         ⇒ if (f.isConstant) s"new ConstantI8((sbyte)${f.constantValue})" else "I8.get()"
      case "i16"        ⇒ if (f.isConstant) s"new ConstantI16((short)${f.constantValue})" else "I16.get()"
      case "i32"        ⇒ if (f.isConstant) s"new ConstantI32(${f.constantValue})" else "I32.get()"
      case "i64"        ⇒ if (f.isConstant) s"new ConstantI64(${f.constantValue}L)" else "I64.get()"
      case "v64"        ⇒ if (f.isConstant) s"new ConstantV64(${f.constantValue}L)" else "V64.get()"
      case "f32"        ⇒ "F32.get()"
      case "f64"        ⇒ "F64.get()"
      case "string"     ⇒ "@string"

      case _            ⇒ s"""(de.ust.skill.common.csharp.@internal.FieldType)(((SkillState)Owner).${name(t)}s())"""
    }

    var mainCount = 0;

    @inline def transformMapTypes(baseTypes: java.util.List[Type], count: Integer) : String = {
      if((baseTypes.size() - 1) == count) {
        mainCount -= 1;
        return s"${mapType(baseTypes.get(count))}"
      }
      else {
        return s"System.Collections.Generic.Dictionary<${mapType(baseTypes.get(count))}," +
          s"${transformMapTypes(baseTypes, count + 1)}>"
      }
    }

    f.getType match {
      case t : GroundType ⇒ mapGroundType(t)
      case t : ConstantLengthArrayType ⇒
        s"new ConstantLengthArray<${mapType(t.getBaseType())}>(${t.getLength}, ${mapGroundType(t.getBaseType)})"

      case t : VariableLengthArrayType ⇒
        s"new VariableLengthArray<${mapType(t.getBaseType())}>(${mapGroundType(t.getBaseType)})"

      case t : ListType ⇒
        s"new ListType<${mapType(t.getBaseType())}>(${mapGroundType(t.getBaseType)})"

      case t : SetType ⇒
        s"new SetType<${mapType(t.getBaseType())}>(${mapGroundType(t.getBaseType)})"

      case t : MapType ⇒ {
        mainCount = t.getBaseTypes().size();
        t.getBaseTypes().map(mapGroundType).reduceRight((k, v) ⇒ s"new MapType<${mapType(t.getBaseTypes().get(mainCount - 2))}, ${transformMapTypes(t.getBaseTypes(), mainCount - 1)}>($k, $v)")
      }
      case t ⇒ s"""(de.ust.skill.common.csharp.@internal.FieldType)(((SkillState)Owner).${name(t)}s())"""

    }
  }

  private def mkFieldRestrictions(f : Field) : String = {
    f.getRestrictions.map(_ match {
      case r : NonNullRestriction ⇒ s"_root_.${packagePrefix}@internal.restrictions.NonNull"
      case r : IntRangeRestriction ⇒
        s"_root_.${packagePrefix}@internal.restrictions.Range(${
          r.getLow
        }L.to${mapType(f.getType)}, ${r.getHigh}L.to${mapType(f.getType)})"

      case r : FloatRangeRestriction ⇒ f.getType.getSkillName match {
        case "f32" ⇒ s"_root_.${packagePrefix}@internal.restrictions.Range(${r.getLowFloat}f, ${r.getHighFloat}f)"
        case "f64" ⇒ s"_root_.${packagePrefix}@internal.restrictions.Range(${r.getLowDouble}, ${r.getHighDouble})"
      }
    }).mkString(", ")
  }
}
