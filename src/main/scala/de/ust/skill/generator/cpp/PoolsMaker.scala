/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.restriction.SingletonRestriction

trait PoolsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    makeHeader
    makeSource
  }

  private final def makeHeader {

    // one header file per base type
    for (base ← IR if null == base.getSuperType) {
      val out = files.open(s"${storagePool(base)}s.h")

      //prefix
      out.write(s"""${beginGuard(storagePool(base))}
#include <skill/api/String.h>
#include <skill/internal/BasePool.h>
#include <skill/internal/SubPool.h>
#include <skill/internal/UnknownSubPool.h>
#include <skill/internal/UnknownSubPool.implementation.h>

#include "TypesOf${name(base)}.h"

${packageParts.mkString("namespace ", " {\nnamespace ", " {")}
""")

      for (t ← IR if base == t.getBaseType) {
        val typeName = packageName + "::" + name(t)
        val isSingleton = !t.getRestrictions.collect { case r : SingletonRestriction ⇒ r }.isEmpty
        val fields = t.getFields

        out.write(s"""
    struct ${storagePool(t)} : public ::skill::internal::${
          if (t.getSuperType == null) s"BasePool<$typeName>"
          else s"SubPool<$typeName, $packageName::${name(t.getBaseType)}>"
        } {
        ${storagePool(t)}(::skill::TypeID typeID${
          if (t.getSuperType == null) ""
          else s", AbstractStoragePool *superPool"
        }, ::skill::api::String name, std::set<::skill::restrictions::TypeRestriction *> *restrictions)
                : ::skill::internal::${
          if (t.getSuperType == null) s"BasePool<$typeName>"
          else s"SubPool<$typeName, $packageName::${name(t.getBaseType)}>"
        }(typeID${
          if (t.getSuperType == null) ""
          else s", superPool"
        }, name, restrictions) { }

        virtual AbstractStoragePool *makeSubPool(::skill::TypeID typeID,
                                                 ::skill::api::String name,
                                                 ::std::set<::skill::restrictions::TypeRestriction *> *restrictions) {
            return new ::skill::internal::UnknownSubPool<${typeName}_UnknownSubType, $packageName::${name(t.getBaseType)}>(
                    typeID, this, name, restrictions);
        }

        virtual ::skill::internal::FieldDeclaration *addField(
                const ::skill::internal::AbstractStringKeeper *const keeper,
                ::skill::TypeID id,
                const ::skill::fieldTypes::FieldType *type,
                ::skill::api::String name);${
          if (t.getFields.isEmpty) ""
          else """

        virtual void complete(::skill::api::SkillFile *owner) override;"""
        }
    };""")
      }

      out.write(s"""
${packageParts.map(_ ⇒ "}").mkString}
$endGuard""")
      out.close()
    }
  }

  private final def makeSource {

    // one file per base type
    for (base ← IR if null == base.getSuperType) {
      val out = files.open(s"${storagePool(base)}s.cpp")

      // common includes
      out.write(s"""#include "${storagePool(base)}s.h"
#include "${name(base)}FieldDeclarations.h"
#include "StringKeeper.h"
#include "File.h"
#include <skill/internal/LazyField.h>
""")

      for (t ← IR if base == t.getBaseType) {
        out.write(s"""
::skill::internal::FieldDeclaration *$packageName::${storagePool(t)}::addField(
        const ::skill::internal::AbstractStringKeeper *const keeper,
        ::skill::TypeID id, const ::skill::fieldTypes::FieldType *type, ::skill::api::String name) {
    ::skill::internal::FieldDeclaration *target;
    ${
          if (t.getFields.isEmpty) "target = new ::skill::internal::LazyField(type, name, id, this);"
          else s"""const StringKeeper *const sk = (const StringKeeper *const) keeper;
   ${
            (for (f ← t.getFields)
              yield s""" if (name == sk->${escaped(f.getSkillName)})
        target = new $packageName::internal::${knownField(f)}(type, name, id, this);
    else"""
            ).mkString
          }
        target = new ::skill::internal::LazyField(type, name, id, this);
"""
        }
    dataFields.push_back(target);
    return target;
}${
          if (t.getFields.isEmpty) ""
          else s"""

void $packageName::${storagePool(t)}::complete(::skill::api::SkillFile *owner) {
    $packageName::api::SkillFile *const state = ($packageName::api::SkillFile *) owner;
    this->owner = state;

    ${
            val (afs, dfs) = t.getFields.partition(_.isAuto)
            (if (dfs.isEmpty) "// no data fields\n"
            else s"""// data fields
    const StringKeeper *const sk = (const StringKeeper *const)
            ((::skill::internal::StringPool *) state->strings)->keeper;
    std::unordered_set<::skill::api::String> fields;
${
              (for (f ← dfs) yield s"""
    fields.insert(sk->${escaped(f.getSkillName)});""").mkString
            }

    for (auto f : dataFields)
        fields.erase(f->name);${
              (for (f ← dfs)
                yield s"""

    if (fields.end() != fields.find(sk->${escaped(f.getSkillName)}))
        dataFields.push_back(new $packageName::internal::${knownField(f)}(
                ${mapFieldDefinition(f.getType)}, sk->${escaped(f.getSkillName)}, dataFields.size() + 1, this));"""
              ).mkString
            }
""") + (
              if (afs.isEmpty) "    // no auto fields\n  "
              else s"""    // auto fields
//    autoFields.sizeHint(${afs.size})${
                afs.map { f ⇒
                  s"""
//    autoFields += new ${knownField(f)}(this, ${mapFieldDefinition(f.getType)})"""
                }.mkString
              }
  """)
          }
}"""
        }
""")
      }
      out.close
    }
  }

  /**
   * escaped name for field classes
   */
  private final def clsName(f : Field) : String = escaped("Cls" + f.getName.camel)

  protected def mapFieldDefinition(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "string"     ⇒ "state->strings"
      case "annotation" ⇒ "state->getAnnotationType()"
      case "bool"       ⇒ "&::skill::fieldTypes::BoolType"
      case n            ⇒ "&::skill::fieldTypes::" + n.capitalize
    }
    case t : UserType                ⇒ s"state->${name(t)}"

    case t : ConstantLengthArrayType ⇒ s"new ::skill::fieldTypes::ConstantLengthArray(${t.getLength}, ${mapFieldDefinition(t.getBaseType)})"
    case t : VariableLengthArrayType ⇒ s"new ::skill::fieldTypes::VariableLengthArray(${mapFieldDefinition(t.getBaseType)})"
    case t : ListType                ⇒ s"new ::skill::fieldTypes::ListType(${mapFieldDefinition(t.getBaseType)})"
    case t : SetType                 ⇒ s"new ::skill::fieldTypes::SetType(${mapFieldDefinition(t.getBaseType)})"
    case t : MapType ⇒ t.getBaseTypes.init.foldRight(mapFieldDefinition(t.getBaseTypes.last)) {
      case (t, str) ⇒ s"new ::skill::fieldTypes::MapType(${mapFieldDefinition(t)}, $str)"
    }
    case _ ⇒ "???"
  }
}
