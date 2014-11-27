#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "../api/${prefix}api.h"

#include "../io/${prefix}reader.h"
#include "../io/${prefix}writer.h"

#include "../model/${prefix}types.h"
#include "../model/${prefix}skill_state.h"
#include "../model/${prefix}storage_pool.h"
#include "../model/${prefix}type_declaration.h"

<#list declarations as declaration>
<#list declaration.all_fields as field>
${field.c_type} <#if field.is_container_type>*</#if><#if field.is_string>*</#if>${prefix}${declaration.name}_get_${field.name} ( ${prefix}${declaration.name} instance ) {
    <#if safe>
    if ( !${prefix}instanceof_${declaration.name} ( (${prefix}skill_type) instance ) ) {
        printf ( "Error: called method '%s' on a struct, which is not an instance of that type.\n", "${prefix}${declaration.name}_get_${field.name}()" );
        exit ( EXIT_FAILURE );
    }
    </#if>
    return ( (${prefix}${field.declaration_name}) instance )->_${field.name};
}

</#list>
<#list declaration.all_fields as field>
void ${prefix}${declaration.name}_set_${field.name} ( ${prefix}${declaration.name} instance, ${field.c_type} <#if field.is_container_type>*</#if><#if field.is_string>*</#if>_${field.name} ) {
    <#if safe>
    if ( !${prefix}instanceof_${declaration.name} ( (${prefix}skill_type) instance ) ) {
        printf ( "Error: called method '%s' on a struct, which is not an instance of that type.\n", "${prefix}${declaration.name}_set_${field.name}()" );
        exit ( EXIT_FAILURE );
    }
    </#if>
    ( (${prefix}${field.declaration_name}) instance )->_${field.name} = _${field.name};
}

</#list>

</#list>

${prefix}skill_state ${prefix}empty_skill_state () {
    return ${prefix}skill_state_new ();
}

void ${prefix}delete_skill_state ( ${prefix}skill_state state ) {
    ${prefix}skill_state_delete_internal ( state );
}

// Reads the binary file at the given location and serializes all instances into this skill state.
${prefix}skill_state ${prefix}skill_state_from_file ( char *file_path ) {
    ${prefix}skill_state result = ${prefix}skill_state_new ();
    ${prefix}read_file ( result, file_path );
    return result;
}

void ${prefix}write_to_file ( ${prefix}skill_state state, char *file_path ) {
    ${prefix}write ( state, file_path );
}

void ${prefix}append_to_file ( ${prefix}skill_state state ) {
    ${prefix}append ( state );
}

GList *${prefix}get_all_instances ( ${prefix}skill_state state ) {
    GList *result = 0;
    <#list declarations as declaration>
    // only add instances of base-pools, so that instances are not added multiple times
    if ( state->${declaration.name}->super_pool == 0 ) {
        result = g_list_concat ( result, ${prefix}storage_pool_get_instances ( state->${declaration.name} ) );
    }
    </#list>
    return result;
}

<#list declarations as declaration>
GList *${prefix}get_${declaration.name}_instances ( ${prefix}skill_state state ) {
    return ${prefix}storage_pool_get_instances ( state->${declaration.name} );
}

</#list>

<#list declarations as declaration>
${prefix}${declaration.name} ${prefix}create_${declaration.name} ( ${prefix}skill_state state<#list declaration.all_fields as field>, ${field.c_type} <#if field.is_container_type>*</#if><#if field.is_string>*</#if>_${field.name}</#list> ) {
    ${prefix}${declaration.name} result = ${prefix}types_create_${declaration.name} ( state, 0<#list declaration.all_fields as field>, _${field.name}</#list> );
    ( (${prefix}skill_type) result )->declaration = state->${declaration.name}->declaration;
    ( (${prefix}skill_type) result )->skill_id = 1;
    ${prefix}storage_pool_add_instance ( state->${declaration.name}, (${prefix}skill_type) result );
    return result;
}
</#list>

void ${prefix}delete_instance ( ${prefix}skill_type instance ) {
    instance->skill_id = 0;
}

<#list declarations as declaration>
bool ${prefix}instanceof_${declaration.name} ( ${prefix}skill_type instance ) {
    ${prefix}type_declaration declaration = instance->state->${declaration.name}->declaration;
    // We need to check all super-types here.
    ${prefix}type_declaration super_declaration = instance->declaration;
    while ( super_declaration ) {
        if ( super_declaration == declaration ) {
            return true;
        }
        super_declaration = super_declaration->super_type;
    }
    return false;
}
</#list>
