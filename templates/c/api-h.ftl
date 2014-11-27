#ifndef ${prefix_capital}API_H_
#define ${prefix_capital}API_H_

#include <glib.h>
#include <stdbool.h>

/*
    Skill State
--------------------
*/
struct ${prefix}skill_state_struct;
typedef struct ${prefix}skill_state_struct *${prefix}skill_state;

// Creates an empty skill_state
${prefix}skill_state ${prefix}empty_skill_state ();

// Frees all memory allocated to the skill_state.
// User-type lists returned by the api are not owned by the state and have to be freed manually.
void ${prefix}delete_skill_state ( ${prefix}skill_state state );

// Creates a skill_state from the given binary file loading all instances from the file.
${prefix}skill_state ${prefix}skill_state_from_file ( char *file_path );

// Serializes all information contained in the given state to the given binary file.
void ${prefix}write_to_file ( ${prefix}skill_state state, char *file_path );

// Updates the file, the given state was created from, i.e. removing deleted
// instances, adding created instances or fields, and updating indices.
void ${prefix}append_to_file ( ${prefix}skill_state state );

//-------------------------------------------------------------------------------------------------

/*
    User Types
------------------

These types represent all user types contained in the specification file this binding was created from.
Typecasting is supported. Use the instanceof<typename> functions to determine the actual type.
Casting to a super-type is always safe.
<#if safe>
IMPORTANT: If you access an instance cast to the wrong type, the program execution will terminate.
<#else>
IMPORTANT: If you access an instance cast to the wrong type, the behavior is undefined.
</#if>
*/

// This is used as super type for all user types.
typedef struct ${prefix}skill_type_struct *${prefix}skill_type;

<#list declarations as declaration>
struct ${prefix}${declaration.name}_struct;
typedef struct ${prefix}${declaration.name}_struct *${prefix}${declaration.name};
</#list>

// These methods return lists containing all instances of a certain type, including all subtypes.
// The returned lists are not owned by the state and have to be freed manually.

GList *${prefix}get_all_instances ( ${prefix}skill_state state );
<#list declarations as declaration>
GList *${prefix}get_${declaration.name}_instances ( ${prefix}skill_state state );
</#list>

<#list declarations as declaration>
${prefix}${declaration.name} ${prefix}create_${declaration.name} ( ${prefix}skill_state state<#list declaration.all_fields as field>, ${field.c_type} <#if field.is_container_type>*</#if><#if field.is_string>*</#if>_${field.name}</#list> );
</#list>

void ${prefix}delete_instance ( ${prefix}skill_type instance );

<#list declarations as declaration>
<#list declaration.all_fields as field>
${field.c_type} <#if field.is_container_type>*</#if><#if field.is_string>*</#if>${prefix}${declaration.name}_get_${field.name} ( ${prefix}${declaration.name} instance );
</#list>
<#list declaration.all_fields as field>
void ${prefix}${declaration.name}_set_${field.name} ( ${prefix}${declaration.name} instance, ${field.c_type} <#if field.is_container_type>*</#if><#if field.is_string>*</#if>_${field.name} );
</#list>

</#list>
<#list declarations as declaration>
bool ${prefix}instanceof_${declaration.name} ( ${prefix}skill_type instance );
</#list>

#endif
