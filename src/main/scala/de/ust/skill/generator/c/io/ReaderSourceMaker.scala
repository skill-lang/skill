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
trait ReaderSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"io/${prefix}reader.c")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <inttypes.h>
#include <glib.h>

#include "../io/${prefix}reader.h"
#include "../io/${prefix}binary_reader.h"

#include "../model/${prefix}string_access.h"
#include "../model/${prefix}storage_pool.h"
#include "../model/${prefix}skill_state.h"
#include "../model/${prefix}field_information.h"
#include "../model/${prefix}type_information.h"
#include "../model/${prefix}types.h"

// This reads all strings of the string block starting at the position of the buffer
// and advances the buffer to the end of the string block.
static void read_string_block ( ${prefix}string_access strings, char **buffer ) {
    int64_t number_of_strings = ${prefix}read_v64 ( buffer );

    // Store the offsets for all strings contained in the block
    int32_t offsets[number_of_strings];
    int64_t i;
    for ( i = 0; i < number_of_strings; i++ ) {
        offsets[i] = ${prefix}read_i32 ( buffer );
    }
    int64_t previous_offset = 0;
    for ( i = 0; i < number_of_strings; i++ ) {
        char *new_string = ${prefix}read_string ( buffer, offsets[i] - previous_offset );
        ${prefix}string_access_add_string ( strings, new_string );
        previous_offset = offsets[i];
    }
}

// Holds information for one type inside of one type block
typedef struct read_information {
    char *type_name;
    char *super_type_name;
    GList *field_info; // GList of field_read_information. This also defines the order, in which the fields have to be read.
    int64_t number_of_instances;
    int64_t lbpsi; // Set to 0, if this has no super-type
    GList *subtype_order; // GList of read_information. Unknown subtypes will cause an error, as their size is not known,
                          // thus they cannot be initialized
} read_information;

typedef struct field_read_information {
    bool is_constant;   // True, if this field is a constant value. In this case, we must not read field data for it.
    char *field_name;
    int64_t offset;
    int64_t pool_index; // If this field holds a reference to a user defined type, this stores the index of its storage pool.
                        // This is stored so that it can be checked later, whether the referenced type is correct, which can
                        // not be done immediately, as the referenced type may not be known yet.
} field_read_information;

// This function checks, that the type information for a field in the binary file matches the expected type info for that field.
static void validate_field_type ( ${prefix}type_information target_type_info, ${prefix}type_information read_type_info, char *field_name ) {
    if ( target_type_info->type != read_type_info->type ) {
        printf ( "Error: expected type %s of the type named %s, but found type %s",
                ${prefix}type_enum_to_string ( target_type_info->type ), target_type_info->name, ${prefix}type_enum_to_string ( read_type_info->type ) );
        exit ( EXIT_FAILURE );
    }
    if ( ${prefix}type_enum_is_constant ( target_type_info->type ) ) {
        // Check the constant value...
        if ( target_type_info->constant_value != read_type_info->constant_value ) {
            printf ( "Error: expected constant value %" PRId64 " of the type named %s, but found %" PRId64 ".\\n",
                target_type_info->constant_value, target_type_info->name, read_type_info->constant_value );
            exit ( EXIT_FAILURE );
        }
    }
    if ( ${prefix}type_enum_is_container_type ( target_type_info->type ) ) {
        if ( target_type_info->type == ${prefix}MAP ) {
            // First, check that the number of base types are equal
            if ( g_list_length ( target_type_info->base_types ) != g_list_length ( read_type_info->base_types ) ) {
                int64_t target_base_types_length = g_list_length ( target_type_info->base_types );
                int64_t read_base_types_length = g_list_length ( read_type_info->base_types );
                printf ( "Error: the map named %s of type %s should have %" PRId64 " base types, but found %" PRId64 " base types.\\n",
                        field_name, target_type_info->name, target_base_types_length, read_base_types_length );
                exit ( EXIT_FAILURE );
            }
            // compare the type of each base type of the map
            int64_t i;
            ${prefix}type_enum first;
            ${prefix}type_enum second;
            for ( i = 0; i < g_list_length ( target_type_info->base_types ); i++ ) {
                first = *( (${prefix}type_enum*) g_list_nth_data ( target_type_info->base_types, i ) );
                second = *( (${prefix}type_enum*) g_list_nth_data ( read_type_info->base_types, i ) );
                if ( first != second ) {
                    printf ( "Error: The base types of the map do not match. Field name %s, type name %s.\\n",
                            field_name, target_type_info->name );
                    exit ( EXIT_FAILURE );
                }
            }
        } else {
            if ( target_type_info->element_type->type != read_type_info->element_type->type ) {
                printf ( "Expected %s as base type, but found %s: field name %s, type name %s.\\n",
                        ${prefix}type_enum_to_string ( target_type_info->element_type->type ), ${prefix}type_enum_to_string ( read_type_info->element_type->type ),
                        field_name, target_type_info->name );
                exit ( EXIT_FAILURE );
            }
            if ( target_type_info->type == ${prefix}CONSTANT_LENGTH_ARRAY ) {
                if ( target_type_info->array_length != read_type_info->array_length ) {
                    printf ( "Error: Expected array length %" PRId64 ", but found length %" PRId64 ". Field name %s of type %s.\\n",
                            target_type_info->array_length, read_type_info->array_length, field_name, target_type_info->name );
                    exit ( EXIT_FAILURE );
                }
            }
        }
    }
}

static field_read_information *read_field_info ( ${prefix}skill_state state, ${prefix}string_access strings, char *type_name, char **buffer ) {
    field_read_information *result = malloc ( sizeof ( field_read_information ) );
    result->field_name = 0;
    result->offset = 0;
    result->pool_index = -1;
    result->is_constant = false;

    // The type information comes before the field name in the binary file.
    // Therefore it is stored in a local variable so that it can be compared to the known information later
    // if the type is known.
    ${prefix}type_information local_type_info = ${prefix}type_information_new ();

    int64_t field_restrictions = ${prefix}read_v64 ( buffer );
    if ( field_restrictions != 0 ) {
        // TODO
        printf ( "Error. Field restrictions not yet implemented.\\n" );
        exit ( EXIT_FAILURE );
    }
    int8_t type_index = ${prefix}read_i8 ( buffer );
    ${prefix}type_enum type = ${prefix}type_enum_from_int ( type_index );
    local_type_info->type = type;
    if ( ${prefix}type_enum_is_constant ( type ) ) {
        result->is_constant = true;
        if ( type == ${prefix}CONSTANT_I8 ) {
            local_type_info->constant_value = ${prefix}read_i8 ( buffer );
        } else if ( type == ${prefix}CONSTANT_I16 ) {
            local_type_info->constant_value = ${prefix}read_i16 ( buffer );
        } else if ( type == ${prefix}CONSTANT_I32 ) {
            local_type_info->constant_value = ${prefix}read_i32 ( buffer );
        } else if ( type == ${prefix}CONSTANT_I64 ) {
            local_type_info->constant_value = ${prefix}read_i64 ( buffer );
        } else if ( type == ${prefix}CONSTANT_V64 ) {
            local_type_info->constant_value = ${prefix}read_v64 ( buffer );
        }
    }

    if ( type == ${prefix}USER_TYPE ) {
        int64_t storage_pool_index = type_index - 32;
        result->pool_index = storage_pool_index;
    }

    if ( ${prefix}type_enum_is_container_type ( type ) ) {
        local_type_info->element_type = ${prefix}type_information_new ();
        if ( type == ${prefix}CONSTANT_LENGTH_ARRAY ) {
            int64_t array_length = ${prefix}read_v64 ( buffer );
            local_type_info->array_length = array_length;
            int64_t element_type_index = ${prefix}read_i8 ( buffer );
            if ( ${prefix}type_enum_from_int ( element_type_index ) == ${prefix}USER_TYPE ) {
                local_type_info->element_type->type = ${prefix}USER_TYPE;
            } else {
                local_type_info->element_type->type = ${prefix}type_enum_from_int ( element_type_index );
            }
        } else if ( type == ${prefix}MAP ) {
            int64_t number_of_types = ${prefix}read_v64 ( buffer );
            int64_t i;
            ${prefix}type_information current_type;
            for ( i = 0; i < number_of_types; i++ ) {
                current_type = ${prefix}type_information_new ();
                current_type->type = ${prefix}type_enum_from_int ( ${prefix}read_i8 ( buffer ) );
                local_type_info->base_types = g_list_append ( local_type_info->base_types, current_type );
            }
        } else {
            int64_t element_type_index = ${prefix}read_i8 ( buffer );
            if ( ${prefix}type_enum_from_int ( element_type_index ) == ${prefix}USER_TYPE ) {
                local_type_info->element_type->type = ${prefix}USER_TYPE;
            } else {
                local_type_info->element_type->type = ${prefix}type_enum_from_int ( element_type_index );
            }
        }
    }

    int64_t field_name_index = ${prefix}read_v64 ( buffer );
    char *field_name = ${prefix}string_access_get_string_by_id ( strings, field_name_index );
    if ( !field_name ) {
        // TODO
        printf ( "Error: didn't find string with index %" PRId64 ".\\n", field_name_index );
        exit ( EXIT_FAILURE );
    }
    result->field_name = field_name;

    ${prefix}storage_pool pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, type_name );
    if ( pool ) {
        // This is a known type
        ${prefix}field_information field_info = (${prefix}field_information) g_hash_table_lookup ( pool->declaration->fields, field_name );
        if ( field_info ) {
            // This is a known field, so check, that the previously read type-information matches the known information of that field
            validate_field_type ( field_info->type_info, local_type_info, field_info->name );
        }
    }

    if ( !${prefix}type_enum_is_constant ( type ) ) {
        int64_t offset = ${prefix}read_v64 ( buffer );
        result->offset = offset;
    }
    ${prefix}type_information_destroy ( local_type_info );

    return result;
}


// Reads type information of one instantiated type and returns the read_information describing that type
static read_information *read_single_type_info ( ${prefix}skill_state state, ${prefix}string_access strings, int64_t *pool_id, GHashTable *seen_types, char **buffer ) {
    bool known_type = false; // will be set to true, if this binding knows the type.
    bool seen_type = false; // will be set to true, if there already was a storage pool with that type in the binary file.
    read_information *result;
    result = malloc ( sizeof ( read_information ) );
    result->type_name = 0;
    result->super_type_name = 0;
    result->field_info = 0;
    result->number_of_instances = 0;
    result->subtype_order = 0;
    result->lbpsi = 0;

    // Read the name
    int64_t type_name_index = ${prefix}read_v64 ( buffer );
    char *type_name = ${prefix}string_access_get_string_by_id ( strings, type_name_index );
    if ( type_name == 0 ) {
        printf ( "Error: string with id '%" PRId64 "' not found.", type_name_index );
        exit ( EXIT_FAILURE );
    }
    result->type_name = type_name;
    if ( g_hash_table_contains ( seen_types, type_name ) ) {
        seen_type = true;
    }
    ${prefix}storage_pool pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, type_name );
    if ( pool ) {
        known_type = true;
        pool->declared_in_file = true;
        if ( !seen_type ) {
            pool->id = *pool_id;
        }
    } else {
        known_type = false;
    }
    if ( !seen_type ) {
        (*pool_id)++;
    }

    // If this type already appeared in a previous type block, the super type field is not present.
    char *super_type_name = 0;
    if ( seen_type ) {
        super_type_name = ( (read_information*) g_hash_table_lookup ( seen_types, type_name ) )->super_type_name;
    } else {
        int64_t super_type_string_index = ${prefix}read_v64 ( buffer );
        super_type_name = ${prefix}string_access_get_string_by_id ( strings, super_type_string_index );
    }
    if ( known_type && !seen_type ) {
        ${prefix}type_declaration declaration = pool->declaration;
        // Check that the right super type is referenced
        if ( super_type_name == 0 ) {
            if ( result->super_type_name != 0 ) {
                printf ( "Error: type %s defines no super type, but should be: %s.\\n", type_name, declaration->super_type->name );
                exit ( EXIT_FAILURE );
            }
        } else {
            if ( !declaration->super_type ) {
                // TODO
                printf ( "Binary file defines super type %s of type %s, but should be none.\\n", super_type_name, type_name );
                exit ( EXIT_FAILURE );
            }
            if ( !( strcmp ( super_type_name, declaration->super_type->name ) == 0 ) ) {
                // TODO
                printf ( "Expected super-type '%s', but was '%s'.\\n", declaration->super_type->name, super_type_name );
                exit ( EXIT_FAILURE );
            }
        }
    }
    if ( known_type && super_type_name ) {
        // Type information about the super type should already be present.
        ${prefix}storage_pool super_pool = pool->super_pool;
        read_information *super_read_info = (read_information*) g_hash_table_lookup ( seen_types, super_pool->declaration->name );
        if ( super_read_info == 0 ) {
            printf ( "Error. super type '%s' has to be declared before the supbtype '%s'.\\n", super_pool->declaration->name, pool->declaration->name );
            exit ( EXIT_FAILURE );
        }
        super_read_info->subtype_order = g_list_append ( super_read_info->subtype_order, result );
    }
    if ( super_type_name ) {
        result->super_type_name = super_type_name;

        // The lbpsi field is only present, if there is a super type.
        int64_t lbpsi = ${prefix}read_v64 ( buffer );
        result->lbpsi = lbpsi;
        result->super_type_name = super_type_name;
    }
    int64_t number_of_instances = ${prefix}read_v64 ( buffer );
    result->number_of_instances = number_of_instances;

    // If this type already appeared in a previous type block, the restrictions are not read again.
    if ( !seen_type ) {
        int64_t restrictions = ${prefix}read_v64 ( buffer );
        if ( restrictions != 0 ) {
            // TODO
            printf ( "Restrictions not yet implemented.\\n" );
            exit ( EXIT_FAILURE );
        }
    }

    int64_t number_of_fields = ${prefix}read_v64 ( buffer );
    // Check, how many fields of this type have already been declared in previous type blocks.
    // Those fields don't declare their type, name, and restrictions again.
    int64_t number_of_known_fields = 0;
    read_information *previous_read_info = (read_information*) g_hash_table_lookup ( seen_types, type_name );
    if ( previous_read_info ) {
        number_of_known_fields = g_list_length ( previous_read_info->field_info );
    }
    int64_t i = 0;
    for ( i = 0; i < number_of_known_fields; i++ ) {
        field_read_information *pool_field_info = (field_read_information*) g_list_nth_data ( previous_read_info->field_info, i );
        field_read_information *new_field_info = malloc ( sizeof ( field_read_information ) );
        new_field_info->field_name = pool_field_info->field_name;
        new_field_info->pool_index = pool_field_info->pool_index;

        if ( number_of_instances > 0 ) {
        // The offset for previously declared fields is only present, if new instances are added
            new_field_info->offset = ${prefix}read_v64 ( buffer );
        } else {
            new_field_info->offset = 0;
        }
        result->field_info = g_list_append ( result->field_info, new_field_info );
    }
    if ( number_of_instances == 0 ) {
        // If there are no new instances, we haven't actually read fields, only taken field information from previous blocks.
        i = 0;
    }
    while ( i < number_of_fields ) {
        // This is a field, which is declared in this block and has not beed declared before.
        result->field_info = g_list_append ( result->field_info, read_field_info ( state, strings, type_name, buffer ) );
        i++;
    }
    g_hash_table_insert ( seen_types, type_name, result );
    return result;
}

// This reads the type information, but not the actual instances.
// It returns a list of read_information in the order in which they have to be read
static GList *read_type_information ( ${prefix}skill_state state, ${prefix}string_access strings, int64_t *pool_id, GHashTable *seen_types, char **buffer ) { // returns a GList of read_information
    GList *result = 0;

    int64_t number_of_instantiated_types = ${prefix}read_v64 ( buffer );
    int64_t i;
    for ( i = 0; i < number_of_instantiated_types; i++ ) {
        result = g_list_append ( result, read_single_type_info ( state, strings, pool_id, seen_types, buffer ) );
    }
    return result;
}

// The SKilL specification requires fields to be in the same order as they appeared in previous type blocks.
// Thus, check for each previously declared field, that it appears in the current fields at the same position.
static void validate_field_order ( ${prefix}skill_state state, read_information *read_information ) {
    ${prefix}storage_pool pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, read_information->type_name );
    if ( !pool ) {
        // This is an unknown type. Its fields are ignored anyway.
        return;
    }

    GList *previous_fields = pool->fields;
    GList *new_field_iter = read_information->field_info;
    ${prefix}field_information previous_field;
    char *current_field_name;
    while ( previous_fields ) {
        previous_field = (${prefix}field_information) previous_fields->data;
        if ( !new_field_iter ) {
            // There are more previously defined fields than the current type block defines.
            // TODO
            printf ( "Field '%s' not found. It was declared in a previous block.\\n", previous_field->name );
            exit ( EXIT_FAILURE );
        }
        current_field_name = ( (field_read_information*) new_field_iter->data )->field_name;
        if ( !( strcmp ( previous_field->name, current_field_name ) == 0 ) ) {
            printf ( "Error: expected field named '%s', which was defined in a previous block but found field named '%s'.\\n",
                    previous_field->name, current_field_name );
            exit ( EXIT_FAILURE );
        }
        previous_fields = previous_fields->next;
        new_field_iter = new_field_iter->next;
    }
}

static void create_sub_pool_instances ( ${prefix}skill_state state, GArray *instances, read_information *read_info, GHashTable *seen_types, int64_t *index ) {
    int64_t old_index = *index; // This will be required later.
    ${prefix}storage_pool pool = g_hash_table_lookup ( state->pools, read_info->type_name );
    int64_t number_of_new_instances = read_info->number_of_instances;
    int64_t number_of_old_instances = pool->instances->len;
    int64_t number_of_sub_instances = 0;
    GList *iterator;
    read_information *subtype_read_info;
    ${prefix}storage_pool subtype_pool;
    for ( iterator = read_info->subtype_order; iterator; iterator = iterator->next ) {
        subtype_read_info = (read_information*) iterator->data;
        number_of_sub_instances += subtype_read_info->number_of_instances;
    }
    if ( *index + number_of_new_instances > instances->len ) {
        printf ( "Error: nuber of instances of subtype is not correct: type %s.\\n", pool->declaration->name );
        exit ( EXIT_FAILURE );
    }
    int64_t i;
    for ( i = 0; i < number_of_new_instances - number_of_sub_instances; i++ ) {
        // Those are new instances of this exact type
        g_array_index ( instances, ${prefix}skill_type, *index ) = calloc ( 1, pool->declaration->size );
        g_array_index ( instances, ${prefix}skill_type, *index )->skill_id = 1;
        g_array_index ( instances, ${prefix}skill_type, *index )->declaration = pool->declaration;
        g_array_index ( instances, ${prefix}skill_type, *index )->state = state;
        (*index)++;
    }
    // Now allocate memory for sub-types
    for ( iterator = read_info->subtype_order; iterator; iterator = iterator->next ) {
        create_sub_pool_instances ( state, instances, (read_information*) iterator->data, seen_types, index );
    }

    // At this point, all instances of this type (including subtypes) already have memory allocated
    // and are referenced in the base-pool's instance array. Thus now, we can set the references of this pool's instance array.
    g_array_set_size ( pool->instances, number_of_new_instances + number_of_old_instances );
    for ( i = 0; i < number_of_new_instances; i++ ) {
        g_array_index ( pool->instances, ${prefix}skill_type, number_of_old_instances + i ) = g_array_index ( instances, ${prefix}skill_type, old_index + i );
    }
}

// This allocates memory for new instances.
// This has to be done for all types before reading any field data so that references to user defined types
// Can already be set to the correct target.
static void create_new_instances ( ${prefix}skill_state state, read_information *read_info, GHashTable *seen_types ) {
    ${prefix}storage_pool pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, read_info->type_name );
    if ( !pool ) {
        // This is an unknown type. No instances of that type will be created.
        return;
    }

    // Some of the new instances may be instances of subtypes. Therefore collect the number of new instances of sub-pools
    int64_t number_of_sub_instances = 0;
    GList *iterator;
    read_information *subtype_read_info;
    for ( iterator = read_info->subtype_order; iterator; iterator = iterator->next ) {
        subtype_read_info = (read_information*) iterator->data;
        number_of_sub_instances += subtype_read_info->number_of_instances;
    }

    int64_t number_of_old_instances = pool->instances->len;
    int64_t number_of_new_instances = read_info->number_of_instances;

    // create new instances
    int64_t index;
    g_array_set_size ( pool->instances, number_of_old_instances + number_of_new_instances );
    // Allocating memory for new instances needs to use the size of the actual type (may be a subtype).
    // So first, only the new instances of this exact type are allocated.
    for ( index = number_of_old_instances; index < number_of_old_instances + number_of_new_instances - number_of_sub_instances; index++ ) {
        g_array_index ( pool->instances, ${prefix}skill_type, index ) = calloc ( 1, pool->declaration->size );
        g_array_index ( pool->instances, ${prefix}skill_type, index )->skill_id = 1;
        g_array_index ( pool->instances, ${prefix}skill_type, index )->declaration = pool->declaration;
        g_array_index ( pool->instances, ${prefix}skill_type, index )->state = state;
    }
    // index is now the position of the first instance of a sub-type.
    // Now allocate memory for instances of sub-types.
    // The order is given by read_info->subtype_order
    for ( iterator = read_info->subtype_order; iterator; iterator = iterator->next ) {
        create_sub_pool_instances ( state, pool->instances, (read_information*) iterator->data, seen_types, &index );
    }
}

// Reads all instances of the type specified by the given read_information
static void read_field_data ( ${prefix}skill_state state, ${prefix}string_access strings, read_information *read_information, int64_t *last_offset, char **buffer ) {
    ${prefix}field_information field_info;
    ${prefix}storage_pool pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, read_information->type_name );
    if ( !pool ) {
        // This is an unknown type, its field data will simply be skipped.
        GList *iterator;
        for ( iterator = read_information->field_info; iterator; iterator = iterator->next ) {
            field_read_information *field_read_info = (field_read_information*) ( iterator->data );
            if ( !field_read_info->is_constant ) {
                (*buffer) += field_read_info->offset - *last_offset;
                *last_offset = field_read_info->offset;
            }
        }
        return;
    }

    // the fields have already been validated, so at this point, it is guaranteed, that the fields already declared
    // in previous blocks, appear in this block in the same order and before new fields.
    // Set the new_fields pointer to the first field, that hasn't been declared previously.
    GList *new_fields = g_list_nth ( read_information->field_info, g_list_length ( pool->fields ) );
    int64_t i;
    int64_t number_of_new_instances = read_information->number_of_instances;
    // To read field data, those steps need to be done:
    //    1. If this block adds new instances, read data for those instances of fields,
    //          that have already been declared in the previous block
    //    2. read new fields for all instances of that type. At this point, no new instances have to be created.

    // Store the last read offset, so that field data can be skipped

    // 1. read previously declared fields
    field_read_information *pool_field_info;
    for ( i = 0; i < g_list_length ( pool->fields ); i++ ) {
        pool_field_info = (field_read_information*) g_list_nth_data ( read_information->field_info, i );
        if ( !pool_field_info->is_constant ) {
            field_info = (${prefix}field_information) g_hash_table_lookup ( pool->declaration->fields, pool_field_info->field_name );
            if ( !field_info ) { // This is an unknown field, thus the field data can be skipped
                (*buffer) += pool_field_info->offset - *last_offset;
                *last_offset = pool_field_info->offset;
            } else {
                // This is the  index of the first instance for which field data has to be set in the instance array of the storage pool
                int64_t start = pool->instances->len - number_of_new_instances;
                // This is the last index + 1 (so that it can be used in a for-loop)
                int64_t end = pool->instances->len;
                int64_t j;
                for ( j = start; j < end; j++ ) {
                    field_info->read ( state, strings, g_array_index ( pool->instances, ${prefix}skill_type, j ), buffer );
                }
                *last_offset = pool_field_info->offset;
            }
        }
    }

    // 2. read new fields
    GList *field_info_iter;
    field_read_information *field_read_info;
    for ( field_info_iter = new_fields; field_info_iter; field_info_iter = field_info_iter->next ) {
        field_read_info = (field_read_information*) field_info_iter->data;
        if ( !field_read_info->is_constant ) {
            field_info = (${prefix}field_information) g_hash_table_lookup ( pool->declaration->fields, field_read_info->field_name );
            if ( !field_info ) { // This is an unknown field, thus the field data can be skipped
                (*buffer) += field_read_info->offset - *last_offset;
                *last_offset = field_read_info->offset;
            } else {
                // This is field data of new fields, thus they have to be read for all existing instances of that type.
                for ( i = 0; i < pool->instances->len; i++ ) {
                    field_info->read ( state, strings, g_array_index ( pool->instances, ${prefix}skill_type, i ), buffer );
                }
                *last_offset = field_read_info->offset;
            }
        }
    }

    // Now update the field-list of the storage_pool so that it contains the new fields as well.
    for ( field_info_iter = new_fields; field_info_iter; field_info_iter = field_info_iter->next ) {
        field_info = (${prefix}field_information) g_hash_table_lookup ( pool->declaration->fields, ( (field_read_information*) field_info_iter->data )->field_name );
        // for an unknown type, the field_info will be null. In that case, just ignore it.
        if ( field_info ) {
            pool->fields = g_list_append ( pool->fields, field_info );
        }
    }
}

// This reads type information and all instances contained in one type block.
static void read_type_block ( ${prefix}skill_state state, ${prefix}string_access strings, char **buffer, int64_t *pool_id, GHashTable *seen_types ) {

    GList *read_information_list = read_type_information ( state, strings, pool_id, seen_types, buffer );
    GList *iterator;
    for ( iterator = read_information_list; iterator; iterator = iterator->next ) {
        validate_field_order ( state, (read_information*) iterator->data );
    }
    // Creating new instances needs to respect instances of sub-pools, thus it is only called on storage pools of base types.
    for ( iterator = read_information_list; iterator; iterator = iterator->next ) {
        if ( ( (read_information*) iterator->data )->super_type_name == 0 ) {
            create_new_instances ( state, (read_information*) iterator->data, seen_types );
        }
    }
    // Keep the last offset value in a variable, so that we can determine how many bytes to skip for unknown fields
    int64_t last_offset = 0;
    for ( iterator = read_information_list; iterator; iterator = iterator->next ) {
        read_field_data ( state, strings, (read_information*) iterator->data, &last_offset, buffer );
    }
}

// Reads the binary file at the given location and serializes all instances into this skill state.
void ${prefix}read_file ( ${prefix}skill_state state, char *filename ) {

    FILE *file;
    char *file_contents;
    char *read_head;
    int64_t file_length;

    file = fopen ( filename, "rb");
    if ( !file )
    {
        fprintf ( stderr, "Unable to open file %s\\n", filename );
        exit ( EXIT_FAILURE );
        // TODO
        return;
    }

    // Get file length
    fseek ( file, 0, SEEK_END );
    file_length = ftell ( file );
    fseek ( file, 0, SEEK_SET );

    file_contents = (char *) malloc ( file_length + 1 );
    if ( !file_contents )
    {
        fprintf ( stderr, "Memory error!\\n" );
        fclose ( file );
        exit ( EXIT_FAILURE );
        // TODO
        return;
    }

    // Read file contents
    fread ( file_contents, file_length, 1, file ) ;
    fclose ( file );
    read_head = file_contents;

    int64_t number_of_read_bytes = 0;
    int64_t pool_id = 0;

    // We need to store some information for the types that have already been read.
    // This maps type-name -> read_information and will contain entries for all seen types during the deserialization.
    GHashTable *seen_types = g_hash_table_new ( g_str_hash, g_str_equal );
    ${prefix}string_access strings = ${prefix}string_access_new ();

    while ( number_of_read_bytes < file_length ) {
        char *previous_position = read_head;
        read_string_block ( strings, &read_head );
        read_type_block ( state, strings, &read_head, &pool_id, seen_types );
        number_of_read_bytes += read_head - previous_position;
    }
    g_hash_table_destroy ( seen_types );

    state->filename = filename;
    state->strings = strings;
    free ( file_contents );
}
""")

    out.close()
  }
}
