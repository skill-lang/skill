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

    for (t ← IR) {
      val typeName = packageName+"::"+name(t)
      val isSingleton = !t.getRestrictions.collect { case r : SingletonRestriction ⇒ r }.isEmpty
      val fields = t.getFields.filterNot(_.isInstanceOf[View])

      val out = open(s"${storagePool(t)}.h")
      //package
      out.write(s"""#include <skill/api/String.h>
#include <skill/internal/${if (t.getSuperType == null) "Base" else "Sub"}Pool.h>
#include <skill/internal/UnknownSubPool.h>
#include <skill/internal/UnknownSubPool.implementation.h>

#include "Types.h"

${packageParts.mkString("namespace ", " {\nnamespace", " {")}

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

        virtual ::skill::internal::FieldDeclaration *addField(::skill::TypeID id,
                                                              const ::skill::fieldTypes::FieldType *type,
                                                              ::skill::api::String name);
    };
${packageParts.map(_ ⇒ "}").mkString}
""")
      out.close()
    }
  }

  private final def makeSource {
    for (t ← IR) {
      val out = open(s"${storagePool(t)}.cpp")
      out.write(s"""#include "${storagePool(t)}.h"
#include <skill/internal/LazyField.h>

::skill::internal::FieldDeclaration *$packageName::${storagePool(t)}::addField(
        ::skill::TypeID id, const ::skill::fieldTypes::FieldType *type, ::skill::api::String name) {
    auto rval = new ::skill::internal::LazyField(type, name);
    dataFields.push_back(rval);
    return rval;
}
""")
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
