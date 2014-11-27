#ifndef ${prefix_capital}SKILL_STATE_H_
#define ${prefix_capital}SKILL_STATE_H_

#include <glib.h>
#include "../model/${prefix}string_access.h"
#include "../model/${prefix}storage_pool.h"
#include "../model/${prefix}types.h"

typedef struct ${prefix}skill_state_struct{
    <#list declarations as declaration>
    ${prefix}storage_pool ${declaration.name};
    </#list>
    GHashTable *pools; // <type_name> -> storage_pool
    ${prefix}string_access strings;
    
    // stores the name of the file from which this state was created.
    // This is NULL, if not created from a file.
    char *filename;
} ${prefix}skill_state_struct;

${prefix}skill_state ${prefix}skill_state_new ();

void ${prefix}skill_state_delete_internal ( ${prefix}skill_state this );

#endif /* SKILL_STATE_H_ */
