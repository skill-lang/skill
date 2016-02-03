/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
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
import de.ust.skill.ir.View
import de.ust.skill.ir.restriction.SingletonRestriction

trait PoolsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    makeHeader
    makeSource
  }

  private final def makeHeader {

    // one header file per base type
    for (base ← IR.par if null == base.getSuperType) {
      val out = open(s"${storagePool(base)}s.h")

      //prefix
      out.write(s"""${beginGuard(storagePool(base))}
#include <skill/api/String.h>
#include <skill/internal/BasePool.h>
#include <skill/internal/SubPool.h>
#include <skill/internal/UnknownSubPool.h>
#include <skill/internal/UnknownSubPool.implementation.h>

#include "TypesOf${name(base)}.h"

${packageParts.mkString("namespace ", " {\nnamespace", " {")}
""")

      for (t ← IR if base == t.getBaseType) {
        val typeName = packageName+"::"+name(t)
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

        virtual ::skill::internal::FieldDeclaration *addField(const ::skill::internal::AbstractStringKeeper *const keeper,
                                                              ::skill::TypeID id,
                                                              const ::skill::fieldTypes::FieldType *type,
                                                              ::skill::api::String name);
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
    for (base ← IR.par if null == base.getSuperType) {
      val out = open(s"${storagePool(base)}s.cpp")

      // common includes
      out.write(s"""#include "${storagePool(base)}s.h"
#include "${name(base)}FieldDeclarations.h"
#include "StringKeeper.h"
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
}
""")
      }
      out.close
    }
  }

  /**
   * escaped name for field classes
   */
  private final def clsName(f : Field) : String = escaped("Cls"+f.getName.camel)

  protected def mapFieldDefinition(t : Type) : String = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "string"     ⇒ "state.String"
      case "annotation" ⇒ "state.AnnotationType"
      case "bool"       ⇒ "BoolType"
      case n            ⇒ n.capitalize
    }
    case t : UserType                ⇒ s"state.${name(t)}"

    case t : ConstantLengthArrayType ⇒ s"ConstantLengthArray(${t.getLength}, ${mapFieldDefinition(t.getBaseType)})"
    case t : VariableLengthArrayType ⇒ s"VariableLengthArray(${mapFieldDefinition(t.getBaseType)})"
    case t : ListType                ⇒ s"ListType(${mapFieldDefinition(t.getBaseType)})"
    case t : SetType                 ⇒ s"SetType(${mapFieldDefinition(t.getBaseType)})"
    case t : MapType ⇒ t.getBaseTypes.init.foldRight(mapFieldDefinition(t.getBaseTypes.last)) {
      case (t, str) ⇒ s"MapType(${mapFieldDefinition(t)}, $str)"
    }
    case _ ⇒ "???"
  }
}
