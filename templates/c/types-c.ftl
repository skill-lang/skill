#include <stdlib.h>
#include "../model/${prefix}types.h"
#include "../model/${prefix}skill_state.h"

static void set_skill_type_fields ( ${prefix}skill_type instance, ${prefix}skill_state state, int64_t skill_id ) {
    instance->skill_id = skill_id;
    instance->state = state;
}

<#list declarations as declaration>
static void set_${declaration.name}_fields ( ${prefix}${declaration.name} instance, ${prefix}skill_state state, int64_t skill_id<#list declaration.all_fields as field>, ${field.c_type} <#if field.is_container_type>*</#if><#if field.is_string>*</#if>_${field.name}</#list> ) {
    set_${declaration.super_type}_fields ( &instance->_super_type, state, skill_id<#list declaration.super_fields as field>, _${field.name}</#list> );
    <#list declaration.fields as field>
    instance->_${field.name} = _${field.name};
    </#list>
}

</#list>

<#list declarations as declaration>
${prefix}${declaration.name} ${prefix}types_create_${declaration.name} ( ${prefix}skill_state state, int64_t skill_id<#list declaration.all_fields as field>, ${field.c_type} <#if field.is_container_type>*</#if><#if field.is_string>*</#if>_${field.name}</#list> ) {
    ${prefix}${declaration.name} result = malloc ( sizeof ( ${prefix}${declaration.name}_struct ) );
    set_${declaration.super_type}_fields ( &result->_super_type, state, skill_id<#list declaration.super_fields as field>, _${field.name}</#list> );
    <#list declaration.fields as field>
    result->_${field.name} = _${field.name};
    </#list>
    return result;
}

</#list>
