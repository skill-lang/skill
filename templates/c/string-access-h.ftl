#ifndef ${prefix_capital}STRING_ACCESS_H_
#define ${prefix_capital}STRING_ACCESS_H_

#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include <glib.h>

typedef struct ${prefix}string_access_struct *${prefix}string_access;

// Wee need a mapping id->string and a mapping string->id. Both lookups should be fast (NOT linear time in the number of strings.)
// Thus two maps are stored here.
typedef struct ${prefix}string_access_struct {
    GHashTable *strings_by_id; // Mapping <id -> string>
    GHashTable *ids_by_string; // Mapping <string -> id>
    int64_t current_skill_id;
} ${prefix}string_access_struct;

${prefix}string_access ${prefix}string_access_new ();

void ${prefix}string_access_destroy ( ${prefix}string_access this );

// Returns the string with the given id or null, if the id is invalid.
char *${prefix}string_access_get_string_by_id ( ${prefix}string_access this, int64_t skill_id );

// Returns the id, this string got assigned.
int64_t ${prefix}string_access_add_string ( ${prefix}string_access this, char *string );

int64_t ${prefix}string_access_get_id_by_string ( ${prefix}string_access this, char *string );

// Returns a list of *char. The list must be deallocated manually.
GList *${prefix}string_access_get_all_strings ( ${prefix}string_access this );

int64_t ${prefix}string_access_get_size ( ${prefix}string_access this );

#endif /* STRING_ACCESS_H_ */
