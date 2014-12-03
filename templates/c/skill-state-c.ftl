#include <stdlib.h>
#include <glib.h>
#include "../model/${prefix}skill_state.h"
#include "../model/${prefix}type_information.h"
#include "../model/${prefix}field_information.h"
#include "../model/${prefix}storage_pool.h"

#include "../io/${prefix}binary_reader.h"
#include "../io/${prefix}binary_writer.h"

// Store read- and write-functions for each field.

<#-- Use additional methods for maps, that have more than 2 base types -->
<#-- need forward declaration here-->
<#list declarations as declaration>
<#list declaration.fields as field>
<#if field.is_map>
<#if ( field.base_types_length > 1 ) >
<#list field.base_types as base_type>
GHashTable *${declaration.name}_read_${field.name}_nested_${base_type.base_type_index} ( ${prefix}skill_state state, ${prefix}string_access strings, char **buffer );
</#list>
</#if>
</#if>
</#list>
</#list>

<#list declarations as declaration>
<#list declaration.fields as field>
<#if field.is_map>
<#if ( field.base_types_length > 1 ) >
<#list field.base_types as base_type>
<#if !base_type.has_nested_map >
<#-- This is just a simple key-value map, without further nested maps -->
GHashTable *${declaration.name}_read_${field.name}_nested_${base_type.base_type_index} ( ${prefix}skill_state state, ${prefix}string_access strings, char **buffer ) {
    <#if base_type.is_declaration>
    ${prefix}storage_pool source_pool = state->${base_type.declaration_type_name};
    GHashTable *result = g_hash_table_new ( g_direct_hash, g_direct_equal );
    <#elseif base_type.is_annotation>
    GHashTable *result = g_hash_table_new ( g_direct_hash, g_direct_equal );
    <#elseif base_type.is_string>
    GHashTable *result = g_hash_table_new ( g_str_hash, g_str_equal );
    <#else>
    GHashTable *result = g_hash_table_new ( g_int64_hash, g_int64_equal );
    </#if>
    <#if base_type.map_value_type.is_declaration>
    ${prefix}storage_pool target_pool = state->${base_type.map_value_type.declaration_type_name};
    </#if>
    <#if base_type.is_declaration>
    ${base_type.declaration_type_name}_struct *current_key;
    <#elseif base_type.is_annotation>
    ${prefix}skill_type current_key;
    <#else>
    ${base_type.c_type} *current_key;
    </#if>
    <#if base_type.map_value_type.is_declaration>
    ${base_type.map_value_type.declaration_type_name}_struct *current_value;
    <#elseif base_type.map_value_type.is_annotation>
    ${prefix}skill_type current_value;
    <#else>
    ${base_type.map_value_type.c_type} *current_value;
    </#if>
    int64_t length = ${prefix}read_v64 ( buffer );
    int64_t i;
    for ( i = 0; i < length; i++ ) {
        <#if base_type.is_declaration>
        current_key = (${base_type.declaration_type_name}_struct*) ${prefix}storage_pool_get_instance_by_id ( source_pool, ${prefix}read_v64 ( buffer ) );
        <#elseif base_type.is_annotation>
        int64_t type_name_id = ${prefix}read_v64 ( buffer );
        if ( type_name_id == 0 ) {
            ${prefix}read_v64 ( buffer );
        } else {
            // TODO error handling
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, ${prefix}string_access_get_string_by_id ( strings, type_name_id ) );
            int64_t reference_id = ${prefix}read_v64 ( buffer );
            current_key = ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
        }
        <#elseif base_type.is_string>
        current_key = ${prefix}string_access_get_string_by_id ( strings, ${prefix}read_v64 ( buffer ) );
        <#else>
        current_key = malloc ( sizeof (${base_type.c_type}) );
        *current_key = ${base_type.read_method} ( buffer );
        </#if>
        <#if base_type.map_value_type.is_declaration>
        current_value = (${base_type.map_value_type.declaration_type_name}_struct*) ${prefix}storage_pool_get_instance_by_id ( target_pool, ${prefix}read_v64 ( buffer ) );
        <#elseif base_type.map_value_type.is_annotation>
        int64_t value_type_name_id = ${prefix}read_v64 ( buffer );
        if ( value_type_name_id == 0 ) {
            ${prefix}read_v64 ( buffer );
        } else {
            // TODO error handling
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, ${prefix}string_access_get_string_by_id ( strings, value_type_name_id ) );
            int64_t value_reference_id = ${prefix}read_v64 ( buffer );
            current_value = ${prefix}storage_pool_get_instance_by_id ( target_pool, value_reference_id );
        }
        
        <#elseif base_type.map_value_type.is_string>
        current_value = ${prefix}string_access_get_string_by_id ( strings, ${prefix}read_v64 ( buffer ) );
        <#else>
        current_value = malloc ( sizeof (${base_type.map_value_type.c_type}) );
        *current_value = ${base_type.map_value_type.read_method} ( buffer );
        </#if>
        g_hash_table_insert ( result, current_key, current_value );
    }
    return result;
}
<#elseif base_type.has_nested_map >
<#-- This is a map with maps as values, so to read the values, call the matching map-read method -->
GHashTable *${declaration.name}_read_${field.name}_nested_${base_type.base_type_index} ( ${prefix}skill_state state, ${prefix}string_access strings, char **buffer ) {
    <#if base_type.is_declaration>
    ${prefix}storage_pool source_pool = state->${base_type.declaration_type_name};
    GHashTable *result = g_hash_table_new ( g_direct_hash, g_direct_equal );
    <#elseif base_type.is_annotation>
    GHashTable *result = g_hash_table_new ( g_direct_hash, g_direct_equal );
    <#elseif base_type.is_string>
    GHashTable *result = g_hash_table_new ( g_str_hash, g_str_equal );
    <#else>
    GHashTable *result = g_hash_table_new ( g_int64_hash, g_int64_equal );
    </#if>
    <#if base_type.is_declaration>
    ${base_type.declaration_type_name}_struct *current_key;
    <#elseif base_type.is_annotation>
    ${prefix}skill_type current_key;
    <#else>
    ${base_type.c_type} *current_key;
    </#if>
    int64_t length = ${prefix}read_v64 ( buffer );
    int64_t i;
    for ( i = 0; i < length; i++ ) {
        <#if base_type.is_declaration>
        current_key = (${base_type.declaration_type_name}_struct*) ${prefix}storage_pool_get_instance_by_id ( source_pool, ${prefix}read_v64 ( buffer ) );
        <#elseif base_type.is_annotation>
        int64_t type_name_id = ${prefix}read_v64 ( buffer );
        if ( type_name_id == 0 ) {
            ${prefix}read_v64 ( buffer );
        } else {
            // TODO error handling
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, ${prefix}string_access_get_string_by_id ( strings, type_name_id ) );
            int64_t reference_id = ${prefix}read_v64 ( buffer );
            current_key = ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
        }
        <#elseif base_type.is_string>
        current_key = ${prefix}string_access_get_string_by_id ( strings, ${prefix}read_v64 ( buffer ) );
        <#else>
        current_key = malloc ( sizeof (${base_type.c_type}) );
        *current_key = ${base_type.read_method} ( buffer );
        </#if>
        g_hash_table_insert ( result, current_key, ${declaration.name}_read_${field.name}_nested_${base_type.base_type_index + 1} ( state, strings, buffer ) );
    }
    return result;
}

</#if>
</#list>
</#if>
</#if>
</#list>
</#list>
<#list declarations as declaration>
<#list declaration.fields as field>
<#if !field.is_transient>
static void ${declaration.name}_read_${field.name} ( ${prefix}skill_state state, ${prefix}string_access strings, ${prefix}skill_type instance, char **buffer ) {
    <#if field.is_string>
    ( (${prefix}${declaration.name}) instance )->_${field.name} = ${prefix}string_access_get_string_by_id ( strings, ${prefix}read_v64 ( buffer ) );
    <#elseif field.is_annotation>
    int64_t type_name_id = ${prefix}read_v64 ( buffer );
    if ( type_name_id == 0 ) {
        ${prefix}read_v64 ( buffer );
    } else {
        // TODO error handling
        ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, ${prefix}string_access_get_string_by_id ( strings, type_name_id ) );
        int64_t reference_id = ${prefix}read_v64 ( buffer );
        ( (${prefix}${declaration.name}) instance )->_${field.name} = ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
    }
    <#elseif field.is_ground_type>
    ( (${prefix}${declaration.name}) instance )->_${field.name} = ${field.read_method} ( buffer );
    <#elseif field.is_declaration>
    int64_t reference_id = ${prefix}read_v64 ( buffer );
    if ( reference_id ) {
        ${prefix}storage_pool target_pool = state->${field.declaration_type_name};
        ( (${prefix}${declaration.name}) instance )->_${field.name} = (${prefix}${field.declaration_type_name}) ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
    }
    <#elseif field.is_constant_length_array>
    int64_t length = ${field.array_length};
    int64_t i;
    <#if field.base_type.is_declaration>
    GArray *array = g_array_new ( true, true, sizeof ( ${prefix}skill_type ) );
    g_array_set_size ( array, length );
    int64_t reference_id;
    ${prefix}storage_pool target_pool = state->${field.base_type.declaration_type_name};
    for ( i = 0; i < length; i++ ) {
        reference_id = ${prefix}read_v64 ( buffer );
        g_array_index ( array, ${prefix}skill_type, i ) = ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
    }
    <#elseif field.base_type.is_annotation>
    GArray *array = g_array_new ( true, true, sizeof ( ${prefix}skill_type ) );
    g_array_set_size ( array, length );
    int64_t reference_id;
    for ( i = 0; i < length; i++ ) {
        int64_t type_name_id = ${prefix}read_v64 ( buffer );
        if ( type_name_id == 0 ) {
            ${prefix}read_v64 ( buffer );
            g_array_index ( array, ${prefix}skill_type, i ) = 0;
        } else {
            // TODO error handling
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, ${prefix}string_access_get_string_by_id ( strings, type_name_id ) );
            reference_id = ${prefix}read_v64 ( buffer );
            g_array_index ( array, ${prefix}skill_type, i ) = ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
        }
    }
    <#elseif field.base_type.is_string>
    GArray *array = g_array_new ( true, true, sizeof ( char* ) );
    g_array_set_size ( array, length );
    for ( i = 0; i < length; i++ ) {
        g_array_index ( array, char*, i ) = ${prefix}string_access_get_string_by_id ( strings, ${prefix}read_v64 ( buffer ) );
    }
    <#else>
    GArray *array = g_array_new ( true, true, sizeof ( ${field.base_type.c_type} ) );
    g_array_set_size ( array, length );
    for ( i = 0; i < ${field.array_length}; i++ ) {
        g_array_index ( array, ${field.base_type.c_type}, i ) = ${field.base_type.read_method} ( buffer );
    }
    </#if>
    ( (${prefix}${declaration.name}) instance )->_${field.name} = array;
    <#elseif field.is_variable_length_array>
    int64_t length = ${prefix}read_v64 ( buffer );
    int64_t i;
    <#if field.base_type.is_declaration>
    GArray *array = g_array_new ( true, true, sizeof ( ${prefix}skill_type ) );
    g_array_set_size ( array, length );
    int64_t reference_id;
    ${prefix}skill_type current_instance;
    ${prefix}storage_pool target_pool = state->${field.base_type.declaration_type_name};
    for ( i = 0; i < length; i++ ) {
        reference_id = ${prefix}read_v64 ( buffer );
        if ( reference_id ) {
            current_instance = ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
            g_array_index ( array, ${prefix}skill_type, i ) = current_instance;
        }
    }
    <#elseif field.base_type.is_annotation>
    GArray *array = g_array_new ( true, true, sizeof ( ${prefix}skill_type ) );
    g_array_set_size ( array, length );
    int64_t reference_id;
    for ( i = 0; i < length; i++ ) {
        int64_t type_name_id = ${prefix}read_v64 ( buffer );
        if ( type_name_id == 0 ) {
            ${prefix}read_v64 ( buffer );
            g_array_index ( array, ${prefix}skill_type, i ) = 0;
        } else {
            // TODO error handling
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, ${prefix}string_access_get_string_by_id ( strings, type_name_id ) );
            reference_id = ${prefix}read_v64 ( buffer );
            g_array_index ( array, ${prefix}skill_type, i ) = ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
        }
    }
    <#elseif field.base_type.is_string>
    GArray *array = g_array_new ( true, true, sizeof ( char* ) );
    g_array_set_size ( array, length );
    for ( i = 0; i < length; i++ ) {
        g_array_index ( array, char*, i ) = ${prefix}string_access_get_string_by_id ( strings, ${prefix}read_v64 ( buffer ) );
    }
    <#else>
    // For arrays, int and float types will be inserted by value.
    GArray *array = g_array_new ( true, true, sizeof ( ${field.base_type.c_type} ) );
    g_array_set_size ( array, length );
    for ( i = 0; i < length; i++ ) {
        g_array_index ( array, ${field.base_type.c_type}, ${field.base_type.read_method} ( buffer ) );
    }
    </#if>
    ( (${prefix}${declaration.name}) instance )->_${field.name} = array;
    <#elseif field.is_list_type>
    int64_t length = ${prefix}read_v64 ( buffer );
    int64_t i;
    GList *list = 0;
    <#if field.base_type.is_declaration>
    int64_t reference_id;
    ${prefix}storage_pool target_pool = state->${field.base_type.declaration_type_name};
    for ( i = 0; i < length; i++ ) {
        reference_id = ${prefix}read_v64 ( buffer );
        list = g_list_append ( list, ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id ) );
    }
    <#elseif field.base_type.is_annotation>
    int64_t reference_id;
    for ( i = 0; i < length; i++ ) {
        int64_t type_name_id = ${prefix}read_v64 ( buffer );
        if ( type_name_id == 0 ) {
            ${prefix}read_v64 ( buffer );
            list = g_list_append ( list, 0 );
        } else {
            // TODO error handling
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, ${prefix}string_access_get_string_by_id ( strings, type_name_id ) );
            reference_id = ${prefix}read_v64 ( buffer );
            list = g_list_append ( list, ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id ) );
        }
    }
    <#elseif field.base_type.is_string>
    for ( i = 0; i < length; i++ ) {
        list = g_list_append ( list, ${prefix}string_access_get_string_by_id ( strings, ${prefix}read_v64 ( buffer ) ) );
    }
    <#else>
    ${field.base_type.c_type} *current_value;
    for ( i = 0; i < length; i++ ) {
        current_value = malloc ( sizeof ( ${field.base_type.c_type} ) );
        *current_value = ${field.base_type.read_method} ( buffer );
        list = g_list_append ( list, current_value );
    }
    </#if>
    ( (${prefix}${declaration.name}) instance )->_${field.name} = list;
    <#elseif field.is_set_type>
    int64_t size = ${prefix}read_v64 ( buffer );
    int64_t i;
    <#if field.base_type.is_declaration>
    GHashTable *set = g_hash_table_new ( g_direct_hash, g_direct_equal );
    ${prefix}storage_pool target_pool = state->${field.base_type.declaration_type_name};
    int64_t reference_id;
    ${prefix}skill_type current_instance;
    for ( i = 0; i < size; i++ ) {
        reference_id = ${prefix}read_v64 ( buffer );
        if ( reference_id ) {
            current_instance = ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
            if ( g_hash_table_lookup ( set, current_instance ) != 0 ) {
                printf ( "Error: found element twice in a set: type %s, field %s.\n", "${declaration.name}", "${field.name}" );
                exit ( EXIT_FAILURE );
            } else {
                g_hash_table_insert ( set, current_instance, current_instance );
            }
        }
    }
    
    <#elseif field.base_type.is_annotation>
    GHashTable *set = g_hash_table_new ( g_direct_hash, g_direct_equal );
    int64_t reference_id;
    ${prefix}skill_type current_instance;
    for ( i = 0; i < size; i++ ) {
        int64_t type_name_id = ${prefix}read_v64 ( buffer );
        if ( type_name_id == 0 ) {
            ${prefix}read_v64 ( buffer );
        } else {
            // TODO error handling
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, ${prefix}string_access_get_string_by_id ( strings, type_name_id ) );
            reference_id = ${prefix}read_v64 ( buffer );
            current_instance = ${prefix}storage_pool_get_instance_by_id ( target_pool, reference_id );
            if ( g_hash_table_lookup ( set, current_instance ) != 0 ) {
                printf ( "Error: found element twice in a set: type %s, field %s.\n", "${declaration.name}", "${field.name}" );
                exit ( EXIT_FAILURE );
            } else {
                g_hash_table_insert ( set, current_instance, current_instance );
            }
        }
    }
    <#elseif field.base_type.is_string>
    char *current_string;
    GHashTable *set = g_hash_table_new ( g_str_hash, g_str_equal );
    for ( i = 0; i < size; i++ ) {
        current_string = ${prefix}string_access_get_string_by_id ( strings, ${prefix}read_v64 ( buffer ) );
        if ( g_hash_table_lookup ( set, current_string ) != 0 ) {
            // TODO
            printf ( "Error: found element twice in a set: type %s, field %s.\n", "${declaration.name}", "${field.name}" );
            exit ( EXIT_FAILURE );
        } else {
            g_hash_table_insert ( set, current_string, current_string );
        }
    }
    <#else>
    GHashTable *set = g_hash_table_new ( g_int64_hash, g_int64_equal );
    ${field.base_type.c_type} *current_value;
    for ( i = 0; i < size; i++ ) {
        current_value = malloc ( sizeof ( ${field.base_type.c_type} ) );
        *current_value = ${field.base_type.read_method} ( buffer );
        if ( g_hash_table_lookup ( set, current_value ) != 0 ) {
            // TODO
            printf ( "Error: found element twice in a set: type %s, field %s.\n", "${declaration.name}", "${field.name}" );
            exit ( EXIT_FAILURE );
        } else {
            g_hash_table_insert ( set, current_value, current_value );
        }
    }
    </#if>
    ( (${prefix}${declaration.name}) instance )->_${field.name} = set;
    <#elseif field.is_map>
    ( (${prefix}${declaration.name}) instance )->_${field.name} = ${declaration.name}_read_${field.name}_nested_0 ( state, strings, buffer );
    </#if>
}
</#if>

</#list>
</#list>
<#-- Use additional methods for maps, that have more than 2 base types -->
<#-- need forward declaration here-->
<#list declarations as declaration>
<#list declaration.fields as field>
<#if field.is_map>
<#if ( field.base_types_length > 1 ) >
<#list field.base_types as base_type>
int64_t ${declaration.name}_write_${field.name}_nested_${base_type.base_type_index} ( GHashTable *map, ${prefix}skill_state state, ${prefix}string_access strings, ${prefix}binary_writer out );
</#list>
</#if>
</#if>
</#list>
</#list>

<#list declarations as declaration>
<#list declaration.fields as field>
<#if field.is_map>
<#if ( field.base_types_length > 1 )>
<#list field.base_types as base_type>
<#if !base_type.has_nested_map >
<#-- This is just a simple key-value map, without further nested maps -->
int64_t ${declaration.name}_write_${field.name}_nested_${base_type.base_type_index} ( GHashTable *map, ${prefix}skill_state state, ${prefix}string_access strings, ${prefix}binary_writer out ) {
    int64_t bytes_written = 0;
    <#if base_type.is_declaration || base_type.is_annotation>
    ${prefix}skill_type current_key;
    <#else>
    ${base_type.c_type} *current_key;
    </#if>
    int64_t length = g_hash_table_size ( map );
    bytes_written += ${prefix}write_v64 ( out, length );
    GList *key_list = g_hash_table_get_keys ( map );
    GList *iterator;
    for ( iterator = key_list; iterator; iterator = iterator->next ) {
        <#if base_type.is_declaration>
        current_key = (${prefix}skill_type) iterator->data;
        if ( current_key == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            bytes_written += ${prefix}write_v64 ( out, current_key->skill_id );
        }
        <#elseif base_type.is_annotation>
        current_key = (${prefix}skill_type) iterator->data;
        if ( current_key == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, current_key->declaration->name );
            char *base_type_name = target_pool->base_pool->declaration->name;
            bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, base_type_name ) );
            bytes_written += ${prefix}write_v64 ( out, current_key->skill_id );
        }
        <#elseif base_type.is_string>
        current_key = (char*) iterator->data;
        bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, current_key ) );
        <#else>
        current_key = (${base_type.c_type}*) iterator->data;
        bytes_written += ${base_type.write_method} ( out, *current_key );
        </#if>
        <#if base_type.map_value_type.is_declaration>
        ${prefix}skill_type target = (${prefix}skill_type) g_hash_table_lookup ( map, current_key );
        if ( target->skill_id == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            bytes_written += ${prefix}write_v64 ( out, target->skill_id );
        }
        <#elseif base_type.map_value_type.is_annotation>
        ${prefix}skill_type target = (${prefix}skill_type) g_hash_table_lookup ( map, current_key );
        if ( target->skill_id == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, target->declaration->name );
            char *base_type_name = target_pool->base_pool->declaration->name;
            bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, base_type_name ) );
            bytes_written += ${prefix}write_v64 ( out, target->skill_id );
        }
        <#elseif base_type.map_value_type.is_string>
        bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, g_hash_table_lookup ( map, current_key ) ) );
        <#else>
        bytes_written += ${base_type.map_value_type.write_method} ( out, *(${base_type.map_value_type.c_type}*) g_hash_table_lookup ( map, current_key ) );
        </#if>
    }
    g_list_free ( key_list );
    return bytes_written;
}
<#elseif base_type.has_nested_map >
<#-- This is a map with maps as values, so to write the values, call the matching map-write method -->
int64_t ${declaration.name}_write_${field.name}_nested_${base_type.base_type_index} ( GHashTable *map, ${prefix}skill_state state, ${prefix}string_access strings, ${prefix}binary_writer out ) {
    int64_t bytes_written = 0;
    <#if base_type.is_declaration || base_type.is_annotation>
    ${prefix}skill_type current_key;
    <#else>
    ${base_type.c_type} *current_key;
    </#if>
    int64_t length = g_hash_table_size ( map );
    bytes_written += ${prefix}write_v64 ( out, length );
    GList *key_list = g_hash_table_get_keys ( map );
    GList *iterator;
    for ( iterator = key_list; iterator; iterator = iterator->next ) {
        <#if base_type.is_declaration>
        current_key = (${prefix}skill_type) iterator->data;
        if ( current_key == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            bytes_written += ${prefix}write_v64 ( out, current_key->skill_id );
        }
        <#elseif base_type.is_annotation>
        current_key = (${prefix}skill_type) iterator->data;
        if ( current_key == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, current_key->declaration->name );
            char *base_type_name = target_pool->base_pool->declaration->name;
            bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, base_type_name ) );
            bytes_written += ${prefix}write_v64 ( out, current_key->skill_id );
        }
        <#elseif base_type.is_string>
        current_key = (char*) iterator->data;
        bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, current_key ) );
        <#else>
        current_key = (${base_type.c_type}*) iterator->data;
        bytes_written += ${base_type.write_method} ( out, *current_key );
        </#if>
        bytes_written += ${declaration.name}_write_${field.name}_nested_${base_type.base_type_index + 1} ( (GHashTable*) g_hash_table_lookup ( map, current_key ), state, strings, out );
    }
    g_list_free ( key_list );
    return bytes_written;
}
</#if>
</#list>
</#if>
</#if>
</#list>
</#list>

// store write_methods for each field 
<#list declarations as declaration>
<#list declaration.fields as field>
<#if !field.is_transient>
int64_t ${declaration.name}_write_${field.name} ( ${prefix}skill_state state, ${prefix}string_access strings, ${prefix}skill_type instance, ${prefix}binary_writer out ) {
    <#if field.is_declaration>
    ${prefix}skill_type target = (${prefix}skill_type) ( (${prefix}${declaration.name}) instance )->_${field.name};
    if ( target->skill_id == 0 ) {
        return ${prefix}write_v64 ( out, 0 );
    }
    return ${prefix}write_v64 ( out, target->skill_id );
    <#elseif field.is_annotation>
    int64_t bytes_written = 0;
    ${prefix}skill_type annotation = ( (${prefix}${declaration.name}) instance )->_${field.name};
    if ( annotation == 0 ) {
        bytes_written += ${prefix}write_v64 ( out, 0 );
        bytes_written += ${prefix}write_v64 ( out, 0 );
    } else {
        ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, annotation->declaration->name );
        char *base_type_name = target_pool->base_pool->declaration->name;
        bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, base_type_name ) );
        bytes_written += ${prefix}write_v64 ( out, annotation->skill_id );
    }
    return bytes_written;
    <#elseif field.is_string>
    return ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, ( (${prefix}${declaration.name}) instance )->_${field.name} ) );
    <#elseif field.is_constant_length_array>
    int64_t bytes_written = 0;
    int64_t i;
    GArray *array = ( (${prefix}${declaration.name}) instance )->_${field.name};
    <#if field.base_type.is_declaration>
    for ( i = 0; i < array->len; i++ ) {
        ${prefix}skill_type target = g_array_index ( array, ${prefix}skill_type, i );
        if ( target->skill_id == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            bytes_written += ${prefix}write_v64 ( out, target->skill_id );
        }
    }
    <#elseif field.base_type.is_annotation>
    for ( i = 0; i < array->len; i++ ) {
        ${prefix}skill_type target = g_array_index ( array, ${prefix}skill_type, i );
        if ( target->skill_id == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, target->declaration->name );
            char *base_type_name = target_pool->base_pool->declaration->name;
            bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, base_type_name ) );
            bytes_written += ${prefix}write_v64 ( out, target->skill_id );
        }
    }
    <#elseif field.base_type.is_string>
    for ( i = 0; i < array->len; i++ ) {
        bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, g_array_index ( array, char*, i ) ) );
    }
    <#else>
    for ( i = 0; i < array->len; i++ ) {
        bytes_written += ${field.base_type.write_method} ( out, g_array_index ( array, ${field.base_type.c_type}, i ) );
    }
    </#if>
    return bytes_written;
    <#elseif field.is_variable_length_array>
    int64_t bytes_written = 0;
    int64_t i;
    GArray *array = ( (${prefix}${declaration.name}) instance )->_${field.name};
    bytes_written += ${prefix}write_v64 ( out, array->len );
    <#if field.base_type.is_declaration>
    for ( i = 0; i < array->len; i++ ) {
        ${prefix}skill_type target = g_array_index ( array, ${prefix}skill_type, i );
        if ( target->skill_id == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            bytes_written += ${prefix}write_v64 ( out, target->skill_id );
        }
    }
    <#elseif field.base_type.is_annotation>
    for ( i = 0; i < array->len; i++ ) {
        ${prefix}skill_type target = g_array_index ( array, ${prefix}skill_type, i );
        if ( target->skill_id == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, target->declaration->name );
            char *base_type_name = target_pool->base_pool->declaration->name;
            bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, base_type_name ) );
            bytes_written += ${prefix}write_v64 ( out, target->skill_id );
        }
    }
    <#elseif field.base_type.is_string>
    for ( i = 0; i < array->len; i++ ) {
        bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, g_array_index ( array, char*, i ) ) );
    }
    <#else>
    for ( i = 0; i < array->len; i++ ) {
        bytes_written += ${field.base_type.write_method} ( out, g_array_index ( array, ${field.base_type.c_type}, i ));
    }
    </#if>
    return bytes_written;
    <#elseif field.is_list_type>
    int64_t bytes_written = 0;
    GList *iterator;
    GList *list = ( (${prefix}${declaration.name}) instance )->_${field.name};
    bytes_written += ${prefix}write_v64 ( out, g_list_length ( list ) );
    
    <#if field.base_type.is_declaration>
    for ( iterator = list; iterator; iterator = iterator->next ) {
        ${prefix}skill_type target = (${prefix}skill_type) iterator->data;
        if ( target->skill_id == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            bytes_written += ${prefix}write_v64 ( out, target->skill_id );
        }
    }
    <#elseif field.base_type.is_annotation>
    for ( iterator = list; iterator; iterator = iterator->next ) {
        ${prefix}skill_type target = (${prefix}skill_type) iterator->data;
        if ( target->skill_id == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, target->declaration->name );
            char *base_type_name = target_pool->base_pool->declaration->name;
            bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, base_type_name ) );
            bytes_written += ${prefix}write_v64 ( out, target->skill_id );
        }
    }
    <#elseif field.base_type.is_string>
    for ( iterator = list; iterator; iterator = iterator->next ) {
        bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, (char*) iterator->data ) );
    }
    <#else>
    for ( iterator = list; iterator; iterator = iterator->next ) {
        bytes_written += ${field.base_type.write_method} ( out, *( (${field.base_type.c_type}*) iterator->data ) );
    }
    </#if>
    return bytes_written;
    <#elseif field.is_set_type>
    int64_t bytes_written = 0;
    GList *iterator;
    GList *values = g_hash_table_get_values ( ( (${prefix}${declaration.name}) instance )->_${field.name} );
    bytes_written += ${prefix}write_v64 ( out, g_list_length ( values ) );
    <#if field.base_type.is_declaration>
    for ( iterator = values; iterator; iterator = iterator->next ) {
        ${prefix}skill_type target = (${prefix}skill_type) iterator->data;
        if ( target->skill_id == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            bytes_written += ${prefix}write_v64 ( out, target->skill_id );
        }
    }
    <#elseif field.base_type.is_annotation>
    for ( iterator = values; iterator; iterator = iterator->next ) {
        ${prefix}skill_type target = (${prefix}skill_type) iterator->data;
        if ( target->skill_id == 0 ) {
            bytes_written += ${prefix}write_v64 ( out, 0 );
            bytes_written += ${prefix}write_v64 ( out, 0 );
        } else {
            ${prefix}storage_pool target_pool = (${prefix}storage_pool) g_hash_table_lookup ( state->pools, target->declaration->name );
            char *base_type_name = target_pool->base_pool->declaration->name;
            bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, base_type_name ) );
            bytes_written += ${prefix}write_v64 ( out, target->skill_id );
        }
    }
    <#elseif field.base_type.is_string>
    for ( iterator = values; iterator; iterator = iterator->next ) {
        bytes_written += ${prefix}write_v64 ( out, ${prefix}string_access_get_id_by_string ( strings, (char*) iterator->data ) );
    }
    <#else>
    for ( iterator = values; iterator; iterator = iterator->next ) {
        bytes_written += ${field.base_type.write_method} ( out, *( (${field.base_type.c_type}*) iterator->data ) );
    }
    </#if>
    g_list_free ( values );
    return bytes_written;
    <#elseif field.is_map>
    return ${declaration.name}_write_${field.name}_nested_0 ( ( (${prefix}${declaration.name}) instance )->_${field.name}, state, strings, out );
    <#elseif field.is_ground_type>
    return ${field.write_method} ( out, ( (${prefix}${declaration.name}) instance )->_${field.name} );
    </#if>
}
</#if>

</#list>

</#list>





<#-- Use additional methods for maps, that have more than 2 base types -->
<#-- need forward declaration here-->
<#list declarations as declaration>
<#list declaration.fields as field>
<#if field.is_map>
<#if ( field.base_types_length > 2 ) >
<#list field.base_types as base_type>
void ${declaration.name}_remove_${field.name}_null_references_nested_${base_type.base_type_index} ( GHashTable *map );
</#list>
</#if>
</#if>
</#list>
</#list>

<#list declarations as declaration>
<#list declaration.fields as field>
<#if field.is_map>
<#if ( field.base_types_length > 1 )>
<#list field.base_types as base_type>
<#if !base_type.has_nested_map >
<#-- This is just a simple key-value map, without further nested maps -->
void ${declaration.name}_remove_${field.name}_null_references_nested_${base_type.base_type_index} ( GHashTable *map ) {
    ${prefix}skill_type current_key;
    int64_t length;
    GList *key_list;
    GList *iterator;
    <#if base_type.is_declaration || base_type.is_annotation>
    length = g_hash_table_size ( map );
    key_list = g_hash_table_get_keys ( map );
    for ( iterator = key_list; iterator; iterator = iterator->next ) {
        current_key = (${prefix}skill_type) iterator->data;
        if ( current_key != 0 && current_key->skill_id == 0 ) {
            g_hash_table_remove ( map, current_key );
        }
    }
    g_list_free ( key_list );
    </#if>
    
    <#if base_type.map_value_type.is_declaration || base_type.map_value_type.is_annotation>
    ${prefix}skill_type current_value;
    length = g_hash_table_size ( map );
    key_list = g_hash_table_get_keys ( map );
    for ( iterator = key_list; iterator; iterator = iterator->next ) {
        current_key = (${prefix}skill_type) iterator->data;
        current_value = ( ${prefix}skill_type ) g_hash_table_lookup ( map, current_key );
        if ( current_value != 0 && current_value->skill_id == 0 ) {
            g_hash_table_remove ( map, current_key );
        }
    }
    g_list_free ( key_list );
    </#if>
}
<#elseif base_type.has_nested_map >
<#-- This is a map with maps as values -->
void ${declaration.name}_remove_${field.name}_null_references_nested_${base_type.base_type_index} ( GHashTable *map ) {
    ${prefix}skill_type current_key;
    int64_t length;
    GList *key_list;
    GList *iterator;
    <#if base_type.is_declaration || base_type.is_annotation>
    length = g_hash_table_size ( map );
    key_list = g_hash_table_get_keys ( map );
    for ( iterator = key_list; iterator; iterator = iterator->next ) {
        current_key = (${prefix}skill_type) iterator->data;
        if ( current_key != 0 && current_key->skill_id == 0 ) {
            g_hash_table_remove ( map, current_key );
        }
    }
    g_list_free ( key_list );
    
    GList *values = g_hash_table_get_values ( map );
    for ( iterator = values; iterator; iterator = iterator->next ) {
        ${declaration.name}_remove_${field.name}_null_references_nested_${base_type.base_type_index + 1} ( (GHashTable*) iterator->data );
    }
    g_list_free ( values );
    </#if>
}
</#if>
</#list>
</#if>
</#if>
</#list>
</#list>

// Store cleanup functions for each declaration.
// They set references to deleted user types to 0 for instances of this type.
<#list declarations as declaration>
void remove_${declaration.name}_null_references ( ${prefix}skill_type instance ) {
    // Probably unused variables to keep code generation not too complicated.
    ${prefix}skill_type target;
    GList *iterator;
    GList *list;
    GList *elements_to_delete;
    GList *values;
    GArray *array;
    int64_t i;
    <#list declaration.fields as field>
    <#if field.is_declaration || field.is_annotation>
    target = (${prefix}skill_type) ( (${prefix}${declaration.name}) instance )->_${field.name};
    if ( target != 0 && target->skill_id == 0 ) {
        ( (${prefix}${declaration.name}) instance )->_${field.name} = 0;
    }
    <#elseif field.is_constant_length_array>
    array = ( (${prefix}${declaration.name}) instance )->_${field.name};
    <#if field.base_type.is_declaration || field.base_type.is_annotation>
    for ( i = 0; i < array->len; i++ ) {
        target = g_array_index ( array, ${prefix}skill_type, i );
        if ( target != 0 && target->skill_id == 0 ) {
            g_array_index ( array, ${prefix}skill_type, i ) = 0;
        }
    }
    </#if>
    <#elseif field.is_variable_length_array>
    array = ( (${prefix}${declaration.name}) instance )->_${field.name};
    <#if field.base_type.is_declaration || field.base_type.is_annotation>
    for ( i = 0; i < array->len; i++ ) {
        ${prefix}skill_type target = g_array_index ( array, ${prefix}skill_type, i );
        if ( target != 0 && target->skill_id == 0 ) {
            g_array_index ( array, ${prefix}skill_type, i ) = 0;
        }
    }
    </#if>
    <#elseif field.is_list_type>
    <#if field.base_type.is_declaration || field.base_type.is_annotation>
    list = ( (${prefix}${declaration.name}) instance )->_${field.name};
    elements_to_delete = 0;
    for ( iterator = list; iterator; iterator = iterator->next ) {
        target = (${prefix}skill_type) iterator->data;
        if ( target != 0 && target->skill_id == 0 ) {
            elements_to_delete = g_list_append ( elements_to_delete, (${prefix}skill_type) iterator->data );
        }
    }
    for ( iterator = elements_to_delete; iterator; iterator = iterator->next ) {
        list = g_list_remove ( list, (${prefix}skill_type) iterator->data );
    }
    g_list_free ( elements_to_delete );
    ( (${prefix}${declaration.name}) instance )->_${field.name} = list;
    </#if>
    <#elseif field.is_set_type>
    values = g_hash_table_get_values ( ( (${prefix}${declaration.name}) instance )->_${field.name} );
    <#if field.base_type.is_declaration || field.base_type.is_annotation>
    for ( iterator = values; iterator; iterator = iterator->next ) {
        target = (${prefix}skill_type) iterator->data;
        if ( target != 0 && target->skill_id == 0 ) {
            g_hash_table_remove ( ( (${prefix}${declaration.name}) instance )->_${field.name}, target );
        }
    }
    </#if>
    g_list_free ( values );
    <#elseif field.is_map>
    ${declaration.name}_remove_${field.name}_null_references_nested_0 ( ( (${prefix}${declaration.name}) instance )->_${field.name} );
    </#if>
    
</#list>
}
</#list>

${prefix}skill_state ${prefix}skill_state_new () {
    ${prefix}skill_state result = malloc ( sizeof ( ${prefix}skill_state_struct ) );
    result->filename = 0;
    result->pools = g_hash_table_new ( g_str_hash, g_str_equal );
    
    ${prefix}storage_pool pool;
    ${prefix}type_declaration declaration;
    ${prefix}field_information current_field;
    <#list declarations as declaration>
    declaration = ${prefix}type_declaration_new ();
    declaration->name = "${declaration.name}";
    declaration->remove_null_references = remove_${declaration.name}_null_references;
    declaration->size = sizeof ( ${prefix}${declaration.name}_struct );
    
    <#list declaration.fields_including_constants as field>
    <#if !field.is_transient>
    current_field = ${prefix}field_information_new ( "${field.name}" );
    current_field->name = "${field.name}";
    <#if !field.is_constant>
    current_field->read = ${declaration.name}_read_${field.name};
    current_field->write = ${declaration.name}_write_${field.name};
    </#if>
    current_field->type_info = ${prefix}type_information_new ();
    <#if field.is_constant>
    current_field->type_info->constant_value = ${field.constant_value};
    </#if>
    <#if field.is_declaration>
    current_field->type_info->type = ${prefix}USER_TYPE;
    current_field->type_info->name = "${field.declaration_type_name}";
    <#else>
    current_field->type_info->type = ${field.enum_type};
    <#if field.is_container_type>
    <#if field.is_map>
    ${prefix}type_information current_type_info;
    <#list field.base_types as base_type>
    current_type_info = ${prefix}type_information_new ();
    <#if base_type.is_declaration>
    current_type_info->type = ${prefix}USER_TYPE;
    current_type_info->name = "${base_type.declaration_type_name}";
    <#else>
    current_type_info->type = ${base_type.enum_type};
    </#if>
    current_field->type_info->base_types = g_list_append ( current_field->type_info->base_types, current_type_info );
    </#list>
    
    <#else>
    current_field->type_info->element_type = ${prefix}type_information_new ();
    <#if field.base_type.is_declaration>
    current_field->type_info->element_type->type = ${prefix}USER_TYPE;
    current_field->type_info->element_type->name = "${field.base_type.declaration_type_name}";
    <#else>
    current_field->type_info->element_type->type = ${field.base_type.enum_type};
    </#if>
    <#if field.is_constant_length_array>
    current_field->type_info->array_length = ${field.array_length};
    </#if>
    </#if>
    </#if>
    </#if>
    
    g_hash_table_insert ( declaration->fields, "${field.name}", current_field );
    
    </#if>
    </#list>
    pool = ${prefix}storage_pool_new ( declaration );
    result->${declaration.name} = pool;
    g_hash_table_insert ( result->pools, "${declaration.name}", pool );
    result->${declaration.name} = pool;
    
    </#list>
    
    // set super-pool and sub-pool references
    ${prefix}storage_pool super_pool;
    <#list declarations as declaration>
    <#if declaration.super_type != "skill_type">
    super_pool = g_hash_table_lookup ( result->pools, "${declaration.super_type}" );
    pool = g_hash_table_lookup ( result->pools, "${declaration.name}" );
    pool->super_pool = super_pool;
    super_pool->sub_pools = g_list_append ( super_pool->sub_pools, pool );
    
    </#if>
    </#list>
    
    // set super-declaration references
    ${prefix}type_declaration super_declaration;
    <#list declarations as declaration>
    <#if declaration.super_type != "skill_type">
    result->${declaration.name}->declaration->super_type = result->${declaration.super_type}->declaration;
    </#if>
    </#list>
    
    // set base-pool references. Those are references to the root of the inheritance tree.
    <#list declarations as declaration>
    pool = result->${declaration.name};
    super_pool = pool;
    while ( super_pool->super_pool != 0 ) {
        super_pool = super_pool->super_pool;
    }
    pool->base_pool = super_pool;
    
    </#list>
    result->strings = ${prefix}string_access_new ();
    return result;
}

void ${prefix}skill_state_delete_internal ( ${prefix}skill_state this ) {
    <#list declarations as declaration>
    ${prefix}type_declaration_destroy ( this->${declaration.name}->declaration );
    ${prefix}storage_pool_destroy ( this->${declaration.name} );
    </#list>
    
    g_hash_table_destroy ( this->pools );
    ${prefix}string_access_destroy ( this->strings );
    free ( this );
}
