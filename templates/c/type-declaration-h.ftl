#ifndef ${prefix_capital}TYPE_DECLARATION_H_
#define ${prefix_capital}TYPE_DECLARATION_H_

#include <glib.h>
#include "../model/${prefix}types.h"

struct ${prefix}type_declaration_struct;
typedef struct ${prefix}type_declaration_struct *${prefix}type_declaration;

// Instances are not deleted directly, but just get their skill-id set to 0.
// Therefore we need a cleanup function to set references deleted instances to null. 
typedef void ${prefix}cleanup_function ( ${prefix}skill_type instance );

// This stores information about a user-defined type.
// There is no order defined on the fields here, as the order may vary between different binary files.
// Also, a binary file may add additional fields, which are not known by the binding.
typedef struct ${prefix}type_declaration_struct {
    char *name;
    ${prefix}type_declaration super_type; // The super-type, if it has one.
    GHashTable *fields; // This is a mapping field-name -> field_information for all its fields.
    int64_t size; // The number of bytes of one instance of this type in memory.
    ${prefix}cleanup_function *remove_null_references;
} ${prefix}type_declaration_struct;

${prefix}type_declaration ${prefix}type_declaration_new ();

void ${prefix}type_declaration_destroy ( ${prefix}type_declaration instance );

#endif /* TYPE_DECLARATION_H */
