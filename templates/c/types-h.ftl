#ifndef ${prefix_capital}TYPES_H_
#define ${prefix_capital}TYPES_H_

#include <glib.h>
#include <stdbool.h>
#include "../api/${prefix}api.h"

struct ${prefix}type_declaration_struct;

typedef struct ${prefix}skill_type_struct {
    int64_t skill_id;
    struct ${prefix}type_declaration_struct *declaration;
    ${prefix}skill_state state;
} ${prefix}skill_type_struct;

<#list declarations as declaration>
typedef struct ${prefix}${declaration.name}_struct {
    ${prefix}${declaration.super_type}_struct _super_type;
    <#list declaration.fields as field>
    <#if field.is_declaration>
    ${prefix}${field.declaration_type_name} _${field.name};
    <#else>
    ${field.c_type} <#if field.is_container_type>*</#if><#if field.is_string>*</#if>_${field.name};
    </#if>
    </#list>
} ${prefix}${declaration.name}_struct;

</#list>

<#list declarations as declaration>
${prefix}${declaration.name} ${prefix}types_create_${declaration.name} ( ${prefix}skill_state state, int64_t skill_id<#list declaration.all_fields as field>, ${field.c_type} <#if field.is_container_type>*</#if><#if field.is_string>*</#if>_${field.name}</#list> );

</#list>
#endif
