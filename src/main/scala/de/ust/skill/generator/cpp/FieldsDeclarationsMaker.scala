/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import scala.collection.JavaConversions._
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.Restriction
import de.ust.skill.ir.restriction.NonNullRestriction
import de.ust.skill.ir.restriction.ConstantLengthPointerRestriction
import de.ust.skill.ir.restriction.IntRangeRestriction
import de.ust.skill.ir.restriction.FloatRangeRestriction

trait FieldDeclarationsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    makeHeader
    makeSource
  }
  private def makeHeader {

    // one file per base type
    for (base ← IR.par if null == base.getSuperType) {
      val out = open(s"${name(base)}FieldDeclarations.h")

      out.write(s"""${beginGuard(s"${name(base)}_field_declarations")}
#include <skill/fieldTypes/AnnotationType.h>
#include <skill/api/SkillFile.h>
#include "${storagePool(base)}s.h"

${packageParts.mkString("namespace ", " {\nnamespace", " {")}
    namespace internal {
""")

      out.write((for (t ← IR if base == t.getBaseType; f ← t.getFields) yield s"""
        /**
         * ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
         */
        class ${knownField(f)} : public ::skill::internal::FieldDeclaration {
        public:
            ${knownField(f)}(
                    const ::skill::FieldType *const type, const ::skill::string_t *name,
                    ::skill::internal::AbstractStoragePool *const owner);

            virtual bool check() const;

            virtual void read(const ::skill::streams::MappedInStream *in,
                              const ::skill::internal::Chunk *target);

            virtual ::skill::api::Box getR(const ::skill::api::Object *i) {
                ${
        if (f.isConstant()) s"return ::skill::api::box((${mapType(f.getType)})${f.constantValue()}L);"
        else s"return ::skill::api::box(((${mapType(t)})i)->${internalName(f)});"
      }
            }

            virtual void setR(::skill::api::Object *i, ::skill::api::Box v) {${
        if (f.isConstant()) ""
        else s"""
                ((${mapType(t)})i)->${internalName(f)} = (${mapType(f.getType)})v.${unbox(f.getType)};"""
      }}
        };""").mkString)

      out.write(s"""
    }
${packageParts.map(_ ⇒ "}").mkString}
$endGuard""")

      out.close()
    }
  }

  private def makeSource {

    // one file per base type
    for (base ← IR.par if null == base.getSuperType) {
      val out = open(s"${name(base)}FieldDeclarations.cpp")

      out.write(s"""
#include "${name(base)}FieldDeclarations.h"
${
        (for (t ← IR if base == t.getBaseType; f ← t.getFields) yield {
          val readI = s"d[i]->${internalName(f)} = ${readType(f.getType)}; // TODO schlicht und ergreifend falsch, weil hier boxen produziert werden"
          s"""
$packageName::internal::${knownField(f)}::${knownField(f)}(
        const ::skill::FieldType *const type,
        const ::skill::string_t *name,
        ::skill::internal::AbstractStoragePool *const owner)
        : FieldDeclaration(type, name, owner) {
${
            (for (r ← f.getRestrictions) yield s"""
    addRestriction(${makeRestriction(f.getType, r)});""").mkString
          }
}

void $packageName::internal::${knownField(f)}::read(
        const ::skill::streams::MappedInStream *part,
        const ::skill::internal::Chunk *target) {
${
            if (f.isConstant()) "    // reading constants is O(0)"
            else s"""
    auto d = ((${storagePool(t)} *) owner)->data;
    skill::streams::MappedInStream in(part, target->begin, target->end);

    try {
        if (dynamic_cast<const ::skill::internal::SimpleChunk *>(target)) {
            for (::skill::SKilLID i = 1 + ((const ::skill::internal::SimpleChunk *) target)->bpo,
                         high = i + target->count; i != high; i++)
                $readI
        } else {
            //case bci : BulkChunk ⇒
            for (int i = 0; i < ((const ::skill::internal::BulkChunk *) target)->blockCount; i++) {
                const auto &b = owner->blocks[i];
                for(::skill::SKilLID i = 1 + b.bpo, end = i + b.dynamicCount; i != end; i++)
                    $readI
            }
        }
    } catch (::skill::SkillException e) {
        throw ParseException(
                in.getPosition(),
                part->getPosition() + target->begin,
                part->getPosition() + target->end, e.message);
    } catch (...) {
        throw ParseException(
                in.getPosition(),
                part->getPosition() + target->begin,
                part->getPosition() + target->end, "unexpected foreign exception");
    }

    if (!in.eof())
        throw ParseException(
                in.getPosition(),
                part->getPosition() + target->begin,
                part->getPosition() + target->end, "did not consume all bytes");"""
          }
}

bool $packageName::internal::${knownField(f)}::check() const {${
            if (f.isConstant) "\n    // constants are always correct"
            else s"""
    if (checkedRestrictions.size()) {
        ${storagePool(t)} *p = (${storagePool(t)} *) owner;
        for (const auto& i : *p) {
            for (auto r : checkedRestrictions)
                if (!r->check(::skill::api::box(i.${internalName(f)})))
                    return false;
        }
    }"""
          }
    return true;
}
"""
        }).mkString
      }""")

      out.close()
    }
  }

  /**
   * choose a good parse expression
   */
  private final def readType(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ "type->read(in).annotation"
      case "string"     ⇒ "type->read(in).string"
      case "bool"       ⇒ "in.boolean()"
      case t            ⇒ s"in.$t()"
    }

    //case t : UserType ⇒ s"    val t = this.t.asInstanceOf[${storagePool(t)}]"
    case _ ⇒ s"(${mapType(t)})type->read(in).${unbox(t)}"
  }

  private final def makeRestriction(t : Type, r : Restriction) : String = r match {
    case r : NonNullRestriction ⇒ "::skill::restrictions::NonNull::get()"
    case r : IntRangeRestriction ⇒
      val typename = s"int${t.getSkillName.substring(1)}_t"
      s"new ::skill::restrictions::Range<$typename>(($typename)${r.getLow}L, ($typename)${r.getHigh}L)"

    case r : FloatRangeRestriction ⇒ t.getSkillName match {
      case "f32" ⇒ s"new ::skill::restrictions::Range<float>(${r.getLowFloat}f, ${r.getHighFloat}f)"
      case "f64" ⇒ s"new ::skill::restrictions::Range<double>(${r.getLowDouble}, ${r.getHighDouble})"
    }
    case r : ConstantLengthPointerRestriction ⇒
      "::skill::restrictions::ConstantLengthPointer::get()"
  }
}
