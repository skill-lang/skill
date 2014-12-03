#ifndef ${prefix_capital}TYPE_INFORMATION_H_
#define ${prefix_capital}TYPE_INFORMATION_H_

#include <glib.h>
#include "../model/${prefix}types.h"
#include "../model/${prefix}type_enum.h"

struct ${prefix}type_information_struct;
typedef struct ${prefix}type_information_struct *${prefix}type_information;

// This stores information about a type of a user-type-field.
// This may be
//   - a simple type like integer, float, string, or boolean
//   - a container type like array, list, set, or map
//   - a pointer to a user-type
typedef struct ${prefix}type_information_struct {
    ${prefix}type_enum type;
    char *name;                             // If this is a pointer to a user type, this holds its name.
    int64_t constant_value;                 // If this is a constant, this holds the constant value.
    ${prefix}type_information element_type; // If this is an array, list, or set, this holds the type if its elements.
    int64_t array_length;                   // If this is a constant length array, this holds its length.
    GList *base_types;                      // If this is a map, it holds its types as list of type_information.
} ${prefix}type_information_struct;

${prefix}type_information ${prefix}type_information_new ();

void ${prefix}type_information_destroy ( ${prefix}type_information instance );

#endif /* TYPE_INFORMATION_H_ */
