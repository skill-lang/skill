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
import de.ust.skill.common.java.internal.parts.Chunk;
import de.ust.skill.common.java.internal.parts.SimpleChunk;
import de.ust.skill.common.jvm.streams.MappedInStream;
import de.ust.skill.common.jvm.streams.MappedOutStream;

""")

      out.write(s"""
/**
 * ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
 */
final class $nameF extends FieldDeclaration<${mapType(f.getType, true)}, ${mapType(t)}> implements ${
        f.getType match {
          case ft : GroundType ⇒ ft.getSkillName match {
            case "bool"                  ⇒ s"""KnownBooleanField<${mapType(t)}>"""
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

    public $nameF(FieldType<${mapType(f.getType, true)}> type, ${
        if (f.isAuto()) ""
        else "int index, "
      }${name(t)}Access owner) {
        super(type, "${f.getSkillName}", ${
        if (f.isAuto()) "0"
        else "index"
      }, owner);
            // TODO insert known restrictions?
    }

    @Override
    public void read(MappedInStream in, Chunk last) {${
        if (f.isConstant())
          """
        // this field is constant"""
        else
          s"""
        final Iterator<$nameT> is;
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
        int count = (int) last.count;
        while (0 != count--) {
            ${
            // read next element
            f.getType match {
              case t : GroundType ⇒ t.getSkillName match {
                case "annotation" ⇒ s"""is.next().set${f.getName.capital}(type.readSingleField(in));"""
                case "string"     ⇒ s"""is.next().set${f.getName.capital}(sp.get(in.v64()));"""
                case _            ⇒ s"""is.next().set${f.getName.capital}(in.${t.getSkillName}());"""
              }

              case t : UserType ⇒ s"""is.next().set${f.getName.capital}(target.getByID(in.v64()));"""
              case _            ⇒ s"""is.next().set${f.getName.capital}(type.readSingleField(in));"""
            }
          }
        }"""
      }
    }

    @Override
    public void write(MappedOutStream out) {
        throw new Error("TODO");
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
}
