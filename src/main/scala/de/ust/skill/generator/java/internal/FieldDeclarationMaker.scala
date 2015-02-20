/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import java.io.PrintWriter
import de.ust.skill.generator.java.GeneralOutputMaker
import scala.collection.JavaConversions._
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
import de.ust.skill.ir.View
import de.ust.skill.ir.UserType

trait FieldDeclarationMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← IR; f ← t.getFields; if !f.isInstanceOf[View]) {
      val nameT = mapType(t)
      val nameF = s"KnownField_${name(t)}_${name(f)}"

      val out = open(s"internal/$nameF.java")
      //package
      out.write(s"""package ${packagePrefix}internal;

import java.util.Iterator;

import de.ust.skill.common.java.internal.*;
import de.ust.skill.common.java.internal.fieldDeclarations.*;
import de.ust.skill.common.java.internal.fieldTypes.*;
import de.ust.skill.common.java.internal.parts.Chunk;
import de.ust.skill.common.java.internal.parts.SimpleChunk;
import de.ust.skill.common.jvm.streams.MappedInStream;

""")

      out.write(s"""
/**
 * ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
 */
final class $nameF extends FieldDeclaration<${mapType(f.getType, true)}, ${mapType(t)}> implements ${
        f.getType match {
          case ft : GroundType ⇒ ft.getSkillName match {
            case "i8"                    ⇒ s"""KnownByteField<${mapType(t)}>"""
            case "i16"                   ⇒ s"""KnownShortField<${mapType(t)}>"""
            case "i32"                   ⇒ s"""KnownIntField<${mapType(t)}>"""
            case "i64" | "v64"           ⇒ s"""KnownLongField<${mapType(t)}>"""
            case "f32"                   ⇒ s"""KnownFloatField<${mapType(t)}>"""
            case "f64"                   ⇒ s"""KnownDoubleField<${mapType(t)}>"""
            case "annotation" | "string" ⇒ s"""KnownField<${mapType(f.getType)}, ${mapType(t)}>"""
            case ft                      ⇒ "???missing specialization for type "+ft
          }
          case _ ⇒ s"""KnownField<${mapType(f.getType)}, ${mapType(t)}>"""
        }
      }${
        // mark ignored fields as ignored; read function is inherited
        if (f.isIgnored()) ", IgnoredField"
        else ""
      }${
        if (f.isAuto()) ", AutoField"
        else"" // generate a read function 
      } {

    public $nameF(${
        if (f.isAuto()) ""
        else "long index, "
      }${name(t)}Access owner) {
        super(${mapToFieldType(f.getType)}, "${f.getSkillName}", ${
        if (f.isAuto()) "0"
        else "index"
      }, owner);
            // TODO insert known restrictions?
    }

    @Override
    public void read(MappedInStream in) {${
        if (f.isConstant())
          """
        // this field is constant"""
        else
          s"""
        final Iterator<$nameT> is;
        Chunk last = dataChunks.getLast();
        if (last instanceof SimpleChunk) {
            SimpleChunk c = (SimpleChunk) last;
            is = ((${name(t)}Access) owner).dataViewIterator((int) c.bpo, (int) (c.bpo + c.count));
        } else
            is = owner.iterator();
${
            // preparation code
            f.getType match {
              case t : GroundType if "string".equals(t.getSkillName) ⇒ s"""
        final StringPool sp = (StringPool)owner.owner().Strings();"""
              case t : UserType ⇒ s"""
        final ${name(t)}Access target = (${name(t)}Access)type;"""
              case _ ⇒ ""
            }
          }
        while (is.hasNext()) {
            ${
            // read next element
            f.getType match {
              case t : GroundType ⇒ t.getSkillName match {
                case "annotation" ⇒ s"""is.next().set${f.getName.capital}(type.readSingleField(in));"""
                case "string"     ⇒ s"""is.next().set${f.getName.capital}(sp.get(in.v64()));"""
                case _            ⇒ s"""is.next().set${f.getName.capital}(in.${t.getSkillName}());"""
              }

              case t : UserType ⇒ s"""is.next().set${f.getName.capital}(target.getByID(in.v64()));"""
              case _            ⇒ "???"
            }
          }
        }"""
      }
    }

    @Override
    public ${mapType(f.getType, true)} getR(${mapType(t)} ref) {
        ${
        if (f.isConstant()) s"return ${mapType(t)}.get${f.getName.capital}();"
        else s"return ref.get${f.getName.capital}();"
      }
    }

    @Override
    public void setR(${mapType(t)} ref, ${mapType(f.getType, true)} value) {
        ${
        if (f.isConstant()) s"""throw new IllegalAccessError("${f.getName.camel} is a constant!");"""
        else s"ref.set${f.getName.capital}(value);"
      }
    }

    @Override
    public ${mapType(f.getType)} get(${mapType(t)} ref) {
        ${
        if (f.isConstant()) s"return ${mapType(t)}.get${f.getName.capital}();"
        else s"return ref.get${f.getName.capital}();"
      }
    }

    @Override
    public void set(${mapType(t)} ref, ${mapType(f.getType)} value) {
        ${
        if (f.isConstant()) s"""throw new IllegalAccessError("${f.getName.camel} is a constant!");"""
        else s"ref.set${f.getName.capital}(value);"
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

      case s            ⇒ s"""TypeDefinitionName[${mapType(t)}]("$s")"""
    }

    t match {
      case t : GroundType              ⇒ mapGroundType(t)
      case t : ConstantLengthArrayType ⇒ s"ConstantLengthArray(${t.getLength}, ${mapGroundType(t.getBaseType)})"
      case t : VariableLengthArrayType ⇒ s"VariableLengthArray(${mapGroundType(t.getBaseType)})"
      case t : ListType                ⇒ s"ListType(${mapGroundType(t.getBaseType)})"
      case t : SetType                 ⇒ s"SetType(${mapGroundType(t.getBaseType)})"
      case t : MapType                 ⇒ t.getBaseTypes().map(mapGroundType).reduceRight((k, v) ⇒ s"MapType($k, $v)")
      case t : Declaration             ⇒ s"""TypeDefinitionName[${mapType(t)}]("${t.getSkillName}")"""
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
