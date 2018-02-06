/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.io

import scala.collection.JavaConversions._
import java.io.PrintWriter
import de.ust.skill.generator.c.GeneralOutputMaker
import de.ust.skill.ir.UserType

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait WriterSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"io/${prefix}writer.c")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <glib.h>

#include "../io/${prefix}writer.h"
#include "../io/${prefix}binary_writer.h"

#include "../model/${prefix}types.h"
#include "../model/${prefix}string_access.h"
#include "../model/${prefix}type_information.h"
#include "../model/${prefix}storage_pool.h"
#include "../model/${prefix}field_information.h"


// collect string functions
// TODO see schema

// This adds all strings required for writing/appending to the given state's string_access.
// If 'for_appending' is set to true, this collects only strings from new instances, otherwise, this collects strings from all instances.
static void collect_strings ( ${prefix}skill_state state, ${prefix}string_access strings, bool for_appending ) {
  // TODO see schema
}

static void write_strings ( ${prefix}string_access strings, ${prefix}binary_writer out ) {
    int64_t number_of_strings = ${prefix}string_access_get_size ( strings );
    ${prefix}write_v64 ( out, number_of_strings );

    int64_t i;
    // Write the offsets
    int64_t offset = 0;
    for ( i = 1; i <= number_of_strings; i++ ) {
        offset += strlen ( ${prefix}string_access_get_string_by_id ( strings, i ) );
        ${prefix}write_i32 ( out, offset );
    }
    for ( i = 1; i <= number_of_strings; i++ ) {
        ${prefix}write_string ( out, ${prefix}string_access_get_string_by_id ( strings, i ) );
    }
}

static void append_strings ( ${prefix}string_access strings, ${prefix}binary_writer out, int64_t start_id ) {
    int64_t i;
    int64_t size = ${prefix}string_access_get_size ( strings );
    ${prefix}write_v64 ( out, ( size - start_id ) + 1 );

    int64_t offset = 0;
    for ( i = start_id; i <= size; i++ ) {
        offset += strlen ( ${prefix}string_access_get_string_by_id ( strings, i ) );
        ${prefix}write_i32 ( out, offset );
    }
    for ( i = start_id; i <= size; i++ ) {
        ${prefix}write_string ( out, ${prefix}string_access_get_string_by_id ( strings, i ) );
    }
}

// Writes field information and returns the list of field offsets, with the current offset appended.
static GList *write_field_information ( ${prefix}field_information field, ${prefix}skill_state state, ${prefix}string_access strings, GList *field_offsets, ${prefix}binary_writer out ) {

    ${prefix}type_information type_info = field->type_info;
    int64_t field_restrictions = 0;
    ${prefix}write_v64 ( out, field_restrictions );

    int64_t field_type_number = ${prefix}type_enum_to_int ( type_info->type );
    if ( type_info->type == ${prefix}USER_TYPE ) {
        // For user types, the number of the storage pool of the target has to be coded into the field_type_number
        ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, type_info->name );
        if ( target_pool == 0 ) {
            // TODO
            printf ( "Error: storage_pool not found for type name %s.\\n", type_info->name );
            exit ( EXIT_FAILURE );
        }
        int64_t pool_index = target_pool->id;
        if ( pool_index < 0 ) {
            // TODO
            exit ( EXIT_FAILURE );
        }
        field_type_number += pool_index;
    }
    ${prefix}write_i8 ( out, field_type_number );

    if ( ${prefix}type_enum_is_constant ( type_info->type ) ) {
        if ( type_info->type == ${prefix}CONSTANT_I8 ) {
            ${prefix}write_i8 ( out, type_info->constant_value );
        } else if ( type_info->type == ${prefix}CONSTANT_I16 ) {
            ${prefix}write_i16 ( out, type_info->constant_value );
        } else if ( type_info->type == ${prefix}CONSTANT_I32 ) {
            ${prefix}write_i32 ( out, type_info->constant_value );
        } else if ( type_info->type == ${prefix}CONSTANT_I64 ) {
            ${prefix}write_i64 ( out, type_info->constant_value );
        } else if ( type_info->type == ${prefix}CONSTANT_V64 ) {
            ${prefix}write_v64 ( out, type_info->constant_value );
        }
    }
    if ( ${prefix}type_enum_is_container_type ( type_info->type ) ) {
        if ( type_info->type == ${prefix}MAP ) {
            ${prefix}write_v64 ( out, g_list_length ( type_info->base_types ) );
            GList *iterator;
            for ( iterator = type_info->base_types; iterator; iterator = iterator->next ) {
                ${prefix}write_i8 ( out, ${prefix}type_enum_to_int ( ( (${prefix}type_information) iterator->data )->type ) );
            }
        } else {
            if ( type_info->type == ${prefix}CONSTANT_LENGTH_ARRAY ) {
                ${prefix}write_v64 ( out, type_info->array_length );
            }
            if ( type_info->element_type->type == ${prefix}USER_TYPE ) {
                ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, type_info->element_type->name );
                ${prefix}write_i8 ( out, 32 + target_pool->id );
            } else {
                ${prefix}write_i8 ( out, ${prefix}type_enum_to_int ( type_info->element_type->type ) );
            }
        }
    }
    int64_t field_name_index = ${prefix}string_access_get_id_by_string ( strings, field->name );
    ${prefix}write_v64 ( out, field_name_index );

    if ( !${prefix}type_enum_is_constant ( type_info->type ) ) {
        // Reserve data for the field offset. This will be written later.
        return g_list_append ( field_offsets, ${prefix}binary_writer_reserve_data ( out ) );
    }
    return field_offsets;
}

// This returns a list of binary_writer - data-structs so that the offsets can be written later.
// This is required as the offsets may not be known yet.
static GList *write_type_information ( ${prefix}skill_state state, ${prefix}string_access strings, ${prefix}storage_pool storage_pool, bool for_appending, ${prefix}binary_writer out ) {
    GList *instances;
    if ( for_appending ) {
        instances = ${prefix}storage_pool_get_new_instances ( storage_pool );
    } else {
        instances = ${prefix}storage_pool_get_instances ( storage_pool );
    }
    ${prefix}type_declaration declaration = storage_pool->declaration;

    int64_t type_name_index = ${prefix}string_access_get_id_by_string ( strings, declaration->name );
    ${prefix}write_v64 ( out, type_name_index );
    if ( !storage_pool->declared_in_file ) {
        int64_t super_type_string_index = 0;
        if ( declaration->super_type ) {
            super_type_string_index = ${prefix}string_access_get_id_by_string ( strings, declaration->super_type->name );
        }
        ${prefix}write_v64 ( out, super_type_string_index );
    }
    // if this has no super-type, don't write the lbpsi-field.
    if ( declaration->super_type ) {
        ${prefix}write_v64 ( out, storage_pool->lbpsi );
    }

    int64_t number_of_instances = g_list_length ( instances );
    ${prefix}write_v64 ( out, number_of_instances );

    // Don't write restrictions, if this type was already declared in a previous type block.
    if ( !storage_pool->declared_in_file ) {
        int64_t restrictions = 0;
        // TODO implement restrictions
        ${prefix}write_v64 ( out, restrictions );
    }

    // If we don't add new instances, we must only count fields, that are not yet declared in the binary file.
    // If new instances are added, count all fields.
    int64_t number_of_fields = 0;
    if ( number_of_instances == 0 ) {
        number_of_fields = g_hash_table_size ( declaration->fields ) - g_list_length ( storage_pool->fields );
    } else {
        number_of_fields = g_hash_table_size ( declaration->fields );
    }
    ${prefix}write_v64 ( out, number_of_fields );

    // store references to each field offset, as they can not be written before the actual instances are serialized
    GList *field_offsets = 0; // GList of data ( from the binary-out.h )
    GList *pool_fields = storage_pool->fields;
    GList *declaration_fields = g_hash_table_get_values ( storage_pool->declaration->fields );
    GList *iterator;
    ${prefix}field_information field_info;

    if ( g_list_length ( instances ) > 0 ) {
        for ( iterator = pool_fields; iterator; iterator = iterator->next ) {
            // Those are the fields, already declared in the binary file.
            // If we add new instances, we only need to write the offset for the field data, not the full type information.
            // Otherwise, they are not written at all.
            field_info = (${prefix}field_information) iterator->data;
            if ( !${prefix}type_enum_is_constant ( field_info->type_info->type ) ) {
                field_offsets = g_list_append ( field_offsets, ${prefix}binary_writer_reserve_data ( out ) );
            }
        }
    }
    for ( iterator = declaration_fields; iterator; iterator = iterator->next ) {
        field_info = (${prefix}field_information) iterator->data;
        if ( !g_list_find ( pool_fields, field_info ) ) {
            // This field is not yet contained in the binary file. Write the full field information.
            field_offsets = write_field_information ( field_info, state, strings, field_offsets, out );
        }
    }
    storage_pool->declared_in_file = true;
    g_list_free ( declaration_fields );
    g_list_free ( instances );
    return field_offsets;
}

// Writes field data for each field and each instance of the given storage_pool.
// The field_offsets list contains references to binary-out data-structs so that the offsets can be written after writing the field data.
static void write_instances ( ${prefix}skill_state state, ${prefix}string_access strings, ${prefix}storage_pool storage_pool, ${prefix}binary_writer out, GList *field_offsets, int64_t *offset ) {

    GList *instances = ${prefix}storage_pool_get_instances ( storage_pool );
    ${prefix}type_declaration declaration = storage_pool->declaration;

    // write the actual data
    int64_t bytes_written = 0;

    GList *field_iterator;
    GList *field_offset_iterator = field_offsets;
    GList *instance_iterator;

    GList *fields = g_hash_table_get_values ( declaration->fields );
    for ( field_iterator = fields; field_iterator; field_iterator = field_iterator->next ) {
        ${prefix}field_information current_field = (${prefix}field_information) field_iterator->data;
        if ( !${prefix}type_enum_is_constant ( current_field->type_info->type ) ) {
            for ( instance_iterator = instances; instance_iterator; instance_iterator = instance_iterator->next ) {
                bytes_written += current_field->write ( state, strings, (${prefix}skill_type) instance_iterator->data, out );
            }
            *offset = *offset + bytes_written;
            ${prefix}write_delayed_v64 ( out, ( ( ${prefix}data ) field_offset_iterator->data ), *offset );
            bytes_written = 0;
            field_offset_iterator = field_offset_iterator->next;
        }
        storage_pool->fields = g_list_append ( storage_pool->fields, current_field );
    }
    g_list_free ( instances );
    g_list_free ( field_offsets );
    g_list_free ( fields );
}

// Writes field data for each field and each instance of the given storage_pool.
// The field_offsets list contains references to binary-out data-structs so that the offsets can be written after writing the field data.
static void append_instances ( ${prefix}skill_state state, ${prefix}string_access strings, ${prefix}storage_pool storage_pool, ${prefix}binary_writer out, GList *field_offsets, int64_t *offset ) {

    GList *all_instances = ${prefix}storage_pool_get_instances ( storage_pool );
    GList *new_instances = ${prefix}storage_pool_get_new_instances ( storage_pool );
    ${prefix}type_declaration declaration = storage_pool->declaration;

    // write the actual data
    int64_t bytes_written = 0;

    GList *field_iterator;
    GList *field_offset_iterator = field_offsets;
    GList *instance_iterator;

    // First, write previously declared fields for new instances
    GList *old_fields = storage_pool->fields;
    if ( g_list_length ( new_instances ) > 0 ) {
        for ( field_iterator = old_fields; field_iterator; field_iterator = field_iterator->next ) {
            ${prefix}field_information current_field = (${prefix}field_information) field_iterator->data;
            for ( instance_iterator = new_instances; instance_iterator; instance_iterator = instance_iterator->next ) {
                bytes_written += current_field->write ( state, strings, (${prefix}skill_type) instance_iterator->data, out );
            }
            *offset = *offset + bytes_written;
            ${prefix}write_delayed_v64 ( out, ( ( ${prefix}data ) field_offset_iterator->data ), *offset );
            bytes_written = 0;
            field_offset_iterator = field_offset_iterator->next;
        }
    }

    GList *fields = g_hash_table_get_values ( declaration->fields );
    for ( field_iterator = fields; field_iterator; field_iterator = field_iterator->next ) {
        ${prefix}field_information current_field = (${prefix}field_information) field_iterator->data;
        if ( !g_list_find ( old_fields, current_field ) ) {
            // This is a new field. This needs to be written for all instances.
            for ( instance_iterator = all_instances; instance_iterator; instance_iterator = instance_iterator->next ) {
                bytes_written += current_field->write ( state, strings, (${prefix}skill_type) instance_iterator->data, out );
            }
            *offset = *offset + bytes_written;
            ${prefix}write_delayed_v64 ( out, ( ( ${prefix}data ) field_offset_iterator->data ), *offset );
            bytes_written = 0;
            field_offset_iterator = field_offset_iterator->next;
            storage_pool->fields = g_list_append ( storage_pool->fields, current_field );
        }
    }
    g_list_free ( new_instances );
    g_list_free ( all_instances );
    g_list_free ( field_offsets );
    g_list_free ( fields );
}

void ${prefix}write ( ${prefix}skill_state state, char *filename ) {
    // storage pools are ordered, so that subtype-pools come directly after their super-type pool.

    GList *all_pools = g_hash_table_get_values ( state->pools );
    ${prefix}storage_pool pool;
    GList *iterator;
    // free memory for deleted instances
    // references to deleted instances have to be set to 0 so that freeing memory of deleted instances does not create dangling pointers.
    for ( iterator = all_pools; iterator; iterator = iterator->next ) {
        ${prefix}storage_pool_remove_null_references ( (${prefix}storage_pool) iterator->data );
    }

    GList *base_pools = 0;

    // Collect base_pools, i.e. pools, whose types are roots of their inheritance tree.
    // This is required, as ids for instances are unique in the context of the same base type.
    for ( iterator = all_pools; iterator; iterator = iterator->next ) {
        pool = (${prefix}storage_pool) iterator->data;
        if ( pool->super_pool == 0 ) {
            base_pools = g_list_append ( base_pools, pool );
        }
    }
    for ( iterator = base_pools; iterator; iterator = iterator->next ) {
        ${prefix}storage_pool_prepare_for_writing ( (${prefix}storage_pool) iterator->data );
    }

    GList *pools = 0;
    for ( iterator = base_pools; iterator; iterator = iterator->next ) {
        pool = (${prefix}storage_pool) iterator->data;
        pools = g_list_append ( pools, pool );
        pools = g_list_concat ( pools, ${prefix}storage_pool_get_sub_pools ( pool ) );
    }
    ${prefix}string_access strings = ${prefix}string_access_new ();
    collect_strings ( state, strings, false );
    ${prefix}binary_writer out = ${prefix}binary_writer_new ();
    write_strings ( strings, out );

    // reset pool_ids as for writing, we can assign them in any order.
    for ( iterator = all_pools; iterator; iterator = iterator->next ) {
        ( (${prefix}storage_pool) iterator->data )->id = -1;
    }

    // In the binary file, storage pools are identified via the order, in which they appear.
    int64_t pool_id = 0; // pool-ids start at 0.

    for ( iterator = pools; iterator; iterator = iterator->next ) {
        ( (${prefix}storage_pool) iterator->data )->id = pool_id;
        pool_id++;
    }

    int64_t number_of_instantiated_types = g_list_length ( pools );
    GList *offsets = 0; // store the field offsets for each field and for each type
    ${prefix}write_v64 ( out, number_of_instantiated_types );
    for ( iterator = pools; iterator; iterator = iterator->next ) {
        offsets = g_list_append ( offsets, write_type_information ( state, strings, (${prefix}storage_pool) iterator->data, false, out ) );
    }
    GList *offset_iterator = offsets;
    int64_t offset = 0;
    for ( iterator = pools; iterator; iterator = iterator->next ) {
        write_instances ( state, strings, (${prefix}storage_pool) iterator->data, out, (GList*) offset_iterator->data, &offset );
        offset_iterator = offset_iterator->next;
    }
    ${prefix}binary_writer_write_to_file ( out, filename );
    ${prefix}binary_writer_destroy ( out );
    g_list_free ( offsets );
    g_list_free ( pools );
    g_list_free ( base_pools );
    g_list_free ( all_pools );
    ${prefix}string_access_destroy ( state->strings );
    state->strings = strings;
    state->filename = filename;
}

void ${prefix}append ( ${prefix}skill_state state ) {
    // For appending, the state must either have been written to a file earlier, or must have been created by
    // reading a binary file.
    // If the filename of the state is not set, neither is the case, and appending is not legal.
    if ( state->filename == 0 ) {
        printf ( "Error: appending not allowed. The state must either have been written to a file earlier, or have been created by reading a binary file.\\n" );
        exit ( EXIT_FAILURE );
    }

    GList *all_pools = g_hash_table_get_values ( state->pools );
    GList *iterator;
    ${prefix}storage_pool pool;

    // free memory for deleted instances
    // references to deleted instances have to be set to 0 so that freeing memory of deleted instances does not create dangling pointers.
    for ( iterator = all_pools; iterator; iterator = iterator->next ) {
        ${prefix}storage_pool_remove_null_references ( (${prefix}storage_pool) iterator->data );
    }

    // Collect base_pools, i.e. pools, whose types are roots of their inheritance tree.
    GList *base_pools = 0;
    for ( iterator = all_pools; iterator; iterator = iterator->next ) {
        pool = (${prefix}storage_pool) iterator->data;
        if ( pool->super_pool == 0 ) {
            base_pools = g_list_append ( base_pools, pool );
        }
    }
    for ( iterator = base_pools; iterator; iterator = iterator->next ) {
        ${prefix}storage_pool_prepare_for_appending ( (${prefix}storage_pool) iterator->data );
    }

    GList *pools = 0;
    for ( iterator = base_pools; iterator; iterator = iterator->next ) {
        pool = (${prefix}storage_pool) iterator->data;
        pools = g_list_append ( pools, pool );
        pools = g_list_concat ( pools, ${prefix}storage_pool_get_sub_pools ( pool ) );
    }

    int64_t first_new_id = ${prefix}string_access_get_size ( state->strings ) + 1;
    collect_strings ( state, state->strings, true );

    ${prefix}binary_writer out = ${prefix}binary_writer_new ();
    append_strings ( state->strings, out, first_new_id );

    // pool-ids, that are already set, remain the same for appending.
    // for pools, that do not have an id yet (i.e. their id is -1), assign new ids.

    // find the current max-id
    int64_t max_id = 0;
    for ( iterator = all_pools; iterator; iterator = iterator->next ) {
        pool = (${prefix}storage_pool) iterator->data;
        if ( pool->id > max_id ) {
            max_id = pool->id;
        }
    }
    // This is the first id for pools, whose id is not set yet.
    int64_t pool_id = max_id + 1;
    for ( iterator = pools; iterator; iterator = iterator->next ) {
        pool = (${prefix}storage_pool) iterator->data;
        if ( pool->id == -1 ) {
            pool->id = pool_id;
        }
    }

    int64_t number_of_instantiated_types = g_list_length ( pools );
    GList *offsets = 0; // store the field offsets for each field and for each type
    ${prefix}write_v64 ( out, number_of_instantiated_types );
    for ( iterator = pools; iterator; iterator = iterator->next ) {
        offsets = g_list_append ( offsets, write_type_information ( state, state->strings, (${prefix}storage_pool) iterator->data, true, out ) );
    }
    GList *offset_iterator = offsets;
    int64_t offset = 0;
    for ( iterator = pools; iterator; iterator = iterator->next ) {
        append_instances ( state, state->strings, (${prefix}storage_pool) iterator->data, out, (GList*) offset_iterator->data, &offset );
        offset_iterator = offset_iterator->next;
    }
    ${prefix}binary_writer_append_to_file ( out, state->filename );
    ${prefix}binary_writer_destroy ( out );

    // New instances have now to be merged in to the old instances array
    for ( iterator = all_pools; iterator; iterator = iterator->next ) {
        ${prefix}storage_pool_mark_instances_as_appended ( (${prefix}storage_pool) iterator->data );
    }
    g_list_free ( offsets );
    g_list_free ( pools );
    g_list_free ( all_pools );
    g_list_free ( base_pools );
}
""")

    out.close()
  }
}
