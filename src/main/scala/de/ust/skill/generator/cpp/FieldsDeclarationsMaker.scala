/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import scala.collection.JavaConversions._
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType

trait FieldDeclarationsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    makeHeader
    makeSource
  }
  private def makeHeader {
    val out = open("FieldDeclarations.h")

    out.write(s"""${beginGuard("field_declarations")}
#include <skill/fieldTypes/AnnotationType.h>
#include <skill/api/SkillFile.h>${
      (for (t ← IR)
        yield s"""
#include "${storagePool(t)}.h"""").mkString
    }

${packageParts.mkString("namespace ", " {\nnamespace", " {")}
    namespace internal {
${
      (for (t ← IR; f ← t.getFields) yield s"""
        /**
         * ${f.getType.toString} ${t.getName.capital}.${f.getName.camel}
         */
        class ${knownField(f)} : public ::skill::internal::FieldDeclaration {
        public:
            ${knownField(f)}(
                    const ::skill::FieldType *const type, const ::skill::string_t *name,
                    ::skill::internal::AbstractStoragePool *const owner)
                    : FieldDeclaration(type, name, owner) { }

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
        };""").mkString
    }
    }
${packageParts.map(_ ⇒ "}").mkString}
$endGuard""")

    out.close()
  }

  private def makeSource {
    val out = open("FieldDeclarations.cpp")

    out.write(s"""
#include "FieldDeclarations.h"
${
      (for (t ← IR; f ← t.getFields) yield {
        val readI = s"d[i]->${internalName(f)} = (${mapType(f.getType)})type->read(in).${unbox(f.getType)};"
        s"""
void $packageName::internal::${knownField(f)}::read(
        const ::skill::streams::MappedInStream *part,
        const ::skill::internal::Chunk *target) {
${
          if (f.isConstant()) "    // reading constants is O(0)"
          else s"""
    auto d = ((${storagePool(t)} *) owner)->data;
    skill::streams::MappedInStream in(part, target->begin, target->end);

//${mapKnownReadType(f.getType)}
/*    try*/ {
        if (target->isSimple()) {
            for (::skill::SKilLID i = 1 + ((::skill::internal::SimpleChunk *) target)->bpo,
                         high = i + target->count; i != high; i++)
                $readI
        } else {
          //case bci : BulkChunk ⇒
            for(const auto& b : owner->blocks){
                for(::skill::SKilLID i = 1 + b.bpo, end = i + b.dynamicCount; i != end; i++)
                    $readI
            }
        }
    }/* catch {
      case e : BufferUnderflowException ⇒
        throw new PoolSizeMissmatchError(dataChunks.size - 1,
          part.position() + target.begin,
          part.position() + target.end,
          this, in.position())
    }

    if(!in.eof())
      throw new PoolSizeMissmatchError(dataChunks.size - 1,
        part.position() + target.begin,
        part.position() + target.end,
        this, in.position())*/"""
        }
}
"""
      }).mkString
    }""")

    out.close()
  }

  /**
   * tell the compiler which code will be executed, to support optimization
   */
  private final def mapKnownReadType(t : Type) : String = t match {

    case t : UserType ⇒ s"    val t = this.t.asInstanceOf[${storagePool(t)}]"
    case _            ⇒ "" // it is always an option not to tell anything
  }
}
