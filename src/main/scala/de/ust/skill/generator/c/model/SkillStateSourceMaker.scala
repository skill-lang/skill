/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.model

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.c.GeneralOutputMaker
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.ContainerType
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.ReferenceType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType
import de.ust.skill.ir.VariableLengthArrayType

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait SkillStateSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"model/${prefix}skill_state.c")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#include <stdlib.h>
#include <glib.h>
#include "../model/${prefix}skill_state.h"
#include "../model/${prefix}type_information.h"
#include "../model/${prefix}field_information.h"
#include "../model/${prefix}storage_pool.h"

#include "../io/${prefix}binary_reader.h"
#include "../io/${prefix}binary_writer.h"

// Store read- and write-functions for each field.${
      // Use additional methods for maps, that have more than 2 base types need forward declaration here
      // (forward declaration)
      //@todo delete if possible (use higher order functions aka function pointers)
      (
        for (
          t ← IR;
          f ← t.getFields;
          index ← 0 to (f.getType match {
            case t : MapType ⇒ t.getBaseTypes.size
            case _           ⇒ -1
          })
        ) yield s"""
GHashTable *${name(t)}_read_${name(f)}_nested_$index(${prefix}skill_state state, ${prefix}string_access strings, char **buffer );""").mkString
    }
${

      // read and write functions
      (
        for (
          t ← IR;
          f ← t.getFields;
          if !f.isAuto
        ) yield s"""
static void ${name(t)}_read_${name(f)}(${prefix}skill_state state, ${prefix}string_access strings, ${prefix}skill_type instance, char **buffer) {
    ${readSingleField(f, access(f))}
}

int64_t ${name(t)}_write_${name(f)}(${prefix}skill_state state, ${prefix}string_access strings, ${prefix}skill_type instance, ${prefix}binary_writer out) {
    ${writeSingleField(f, access(f))}
}
""").mkString
    }
${

      /**
       *  create function that removes references to deleted objects
       *  this function is called prior to actual deletion of these objects and is needed in order to prevent dangling
       *  references
       */
      (
        for (
          t ← IR
        ) yield s"""
void remove_${name(t)}_null_references(${prefix}skill_type instance) {${
          (for (f ← t.getFields if holdsReference(f))
            yield f.getType match {
            case t : ReferenceType ⇒ s"""
    {
        ${prefix}skill_type target = ${cast()}${access(f)};
        if(0!=target && 0==target->skill_id) {
            ${access(f)} = 0;
        }
    }"""
            case t : ConstantLengthArrayType ⇒ s"""
    {
        GArray *array = ${access(f)};
        for(int64_t i = 0; i < array->len; i++){
            ${prefix}skill_type target = g_array_index(array, ${prefix}skill_type, i);
            if(0!=target && 0==target->skill_id) {
                ${access(f)} = 0;
            }
        }
    }"""
            case t : VariableLengthArrayType ⇒ s"""
    {
        GArray *array = ${access(f)};
        for(int64_t i = 0; i < array->len; i++){
            ${prefix}skill_type target = g_array_index(array, ${prefix}skill_type, i);
            if(0!=target && 0==target->skill_id) {
                ${access(f)} = 0;
            }
        }
    }"""
            case t : ListType ⇒ s"""
    {
        // TODO replace by something with sane runtime behaviour
        GList *list = ${access(f)};
        GList *elements_to_delete = 0;
        for ( iterator = list; iterator; iterator = iterator->next ) {
            target = (${prefix}skill_type) iterator->data;
            if ( target != 0 && target->skill_id == 0 ) {
                elements_to_delete = g_list_append ( elements_to_delete, (${prefix}skill_type) iterator->data );
            }
        }
        for ( iterator = elements_to_delete; iterator; iterator = iterator->next ) {
            list = g_list_remove ( list, (${prefix}skill_type) iterator->data );
        }
        g_list_free(elements_to_delete);
        ${access(f)} = list;
    }"""
            case t : SetType ⇒ s"""
    {
        // TODO replace by something with sane runtime behaviour
        values = g_hash_table_get_values ( ( (${prefix}${name(t)}) instance )->_${name(f)} );
    <#if field.base_type.is_declaration || field.base_type.is_annotation>
    for ( iterator = values; iterator; iterator = iterator->next ) {
        target = (${prefix}skill_type) iterator->data;
        if ( target != 0 && target->skill_id == 0 ) {
            g_hash_table_remove(((${prefix}${name(t)}) instance )->_${name(f)}, target );
        }
    }
    }"""
            case t ⇒ s"""// TODO I do not know what to do with $t"""
          }
          ).mkString
        }
}"""
      ).mkString
    }

${prefix}skill_state ${prefix}skill_state_new() {
    ${prefix}skill_state result = malloc(sizeof(${prefix}skill_state_struct));
    result->filename = 0;
    result->pools = g_hash_table_new(g_str_hash, g_str_equal);
${
      // create types
      (for (t ← IR) yield s"""
    // create ${t.getName.cStyle} pool
    {
        ${prefix}type_declaration declaration = ${prefix}type_declaration_new();
        declaration->name = "${t.getSkillName}";
        declaration->remove_null_references = remove_${name(t)}_null_references;
        declaration->size = sizeof(${prefix}${name(t)}_struct);
${
        (for (f ← t.getFields; if !f.isAuto) yield s"""
        {
            ${prefix}field_information current_field = ${prefix}field_information_new("${name(f)}");
            current_field->name = "${name(f)}";${
          if (f.isConstant()) ""
          else s"""
            current_field->read = ${name(t)}_read_${name(f)};
            current_field->write = ${name(t)}_write_${name(f)};"""
        }

            current_field->type_info = ${prefix}type_information_new();${
          if (!f.isConstant()) ""
          else s"""
            current_field->type_info->constant_value = ${f.constantValue};"""
        }${
          val ft = f.getType
          if (ft.isInstanceOf[UserType]) s"""
            current_field->type_info->type = ${prefix}USER_TYPE;
            current_field->type_info->name = "${f.getType.getSkillName}";"""
          else s"""
            current_field->type_info->type = ${enumType(f)};${
            ft match {
              case ft : MapType ⇒ s"""
            ${prefix}type_information current_type_info;${
                (for (bt ← ft.getBaseTypes) yield s"""
            current_type_info = ${prefix}type_information_new();${
                  if (bt.isInstanceOf[UserType]) s"""
            current_type_info->type = ${prefix}USER_TYPE;
            current_type_info->name = "${bt.getSkillName}";"""
                  else s"""
            current_type_info->type = ${enumType(bt)};"""
                }
            current_field->type_info->base_types = g_list_append(current_field->type_info->base_types, current_type_info);"""
                ).mkString
              }"""
              case ft : SingleBaseTypeContainer ⇒
                val bt = ft.getBaseType
                s"""
            current_field->type_info->element_type = ${prefix}type_information_new();${
                  if (bt.isInstanceOf[UserType]) s"""
            current_field->type_info->element_type->type = ${prefix}USER_TYPE;
            current_field->type_info->element_type->name = "${bt.getSkillName}";"""

                  else s"""
            current_field->type_info->element_type->type = ${enumType(bt)};"""
                }${
                  ft match {
                    case t : ConstantLengthArrayType ⇒ s"current_field->type_info->array_length = ${t.getLength};"
                    case _                           ⇒ ""
                  }
                }"""
              case _ ⇒ ""
            }
          }"""
        }

            g_hash_table_insert(declaration->fields, "${f.getSkillName}", current_field);
        }
""").mkString
      }
        ${prefix}storage_pool pool = ${prefix}storage_pool_new(declaration);
        result->${name(t)} = pool;
        g_hash_table_insert(result->pools, "${t.getSkillName}", pool);
    ${
        if (null != t.getSuperType) s"""
        // set super-pool and sub-pool references
        ${prefix}storage_pool super_pool = result->${name(t.getSuperType)};
        pool->super_pool = super_pool;
        super_pool->sub_pools = g_list_append(super_pool->sub_pools, pool);

        // set super-declaration references
        pool->declaration->super_type = super_pool->declaration;
"""
        else ""
      }
        // set base-pool references. Those are references to the root of the inheritance tree.
        pool->base_pool = result->${name(t.getBaseType)};
    }
"""
      ).mkString
    }
    result->strings = ${prefix}string_access_new();
    return result;
}

void ${prefix}skill_state_delete_internal(${prefix}skill_state this) {${
      (for (t ← IR) yield s"""
    ${prefix}type_declaration_destroy(this->${name(t)}->declaration);
    ${prefix}storage_pool_destroy(this->${name(t)});"""
      ).mkString
    }

    g_hash_table_destroy(this->pools);
    ${prefix}string_access_destroy(this->strings);
    free(this);
}
""")

    out.close()
  }

  def readSingleField(f : Field, target : String) : String = f.getType match {
    case t : UserType ⇒ s"""
    int64_t reference_id = ${prefix}read_v64(buffer);
    $target = reference_id ? ${cast(t)} ${prefix}storage_pool_get_instance_by_id(state->${name(t)}, reference_id) : 0;"""
    case t : GroundType ⇒ t.getSkillName() match {
      case "string" ⇒
        s"""$target = ${prefix}string_access_get_string_by_id ( strings, ${prefix}read_v64 ( buffer ) );"""
      case "annotation" ⇒
        // TODO change this implementation to TR17
        s"""int64_t type_name_id = ${prefix}read_v64 ( buffer );
    if ( type_name_id == 0 ) {
        ${prefix}read_v64 ( buffer );
    } else {
        // TODO error handling
        ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, ${prefix}string_access_get_string_by_id ( strings, type_name_id ) );
        int64_t reference_id = ${prefix}read_v64 ( buffer );
        $target = ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
    }"""
      case tn ⇒ s"$target = ${prefix}read_$tn(buffer);"
    }
    case _ ⇒ "???"
  }

  def writeSingleField(f : Field, target : String) : String = f.getType match {
    case t : UserType ⇒ s"""    ${prefix}skill_type target = (${prefix}skill_type)target;
    return ${prefix}write_v64(out, target?target->skill_id:0);
"""
    case t : GroundType ⇒ t.getSkillName() match {
      case "string" ⇒
        s"""${prefix}write_v64(out, ${prefix}string_access_get_id_by_string(strings, ${access(f)}));"""
      case "annotation" ⇒
        // TODO change this implementation to TR17
        s"""${prefix}skill_type annotation = $target;
    if(0 == annotation) {
        write_i8(out, 0);
        write_i8(out, 0);
        return 2;
    } else {
        register int64_t bytes_written = 0;
        storage_pool target_pool = (storage_pool) g_hash_table_lookup ( state->pools, annotation->declaration->name );
        char *base_type_name = target_pool->base_pool->declaration->name;
        bytes_written += write_v64 ( out, string_access_get_id_by_string ( strings, base_type_name ) );
        bytes_written += write_v64 ( out, annotation->skill_id );
        return bytes_written;
    }"""
      case tn ⇒ s"""return ${prefix}write_$tn(out, $target);"""
    }
    case _ ⇒ "???"
  }

  /**
   * @return true, iff the field may hold a reference to an instance managed by the state
   */
  def holdsReference(f : Field) : Boolean = f.getType match {
    case t : ReferenceType           ⇒ true
    case t : SingleBaseTypeContainer ⇒ t.getBaseType.isInstanceOf[ReferenceType]
    case t : MapType                 ⇒ t.getBaseTypes.exists(_.isInstanceOf[ReferenceType])
    case _                           ⇒ false
  }

  /**
   * conversion from a field's type to type_enum name
   * @note may be broken in case of compound types
   */
  def enumType(t : Type) : String = if (t.isInstanceOf[ContainerType]) "???" else prefix + t.getName.cStyle().toUpperCase()
  def enumType(f : Field) : String = enumType(f.getType)
}
