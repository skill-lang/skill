#ifndef ${prefix_capital}FIELD_INFORMATION_H_
#define ${prefix_capital}FIELD_INFORMATION_H_

#include "../model/${prefix}type_information.h"
#include "../model/${prefix}skill_state.h"
#include "../model/${prefix}string_access.h"
#include "../model/${prefix}types.h"
#include "../io/${prefix}binary_writer.h"

// ---------------------------------------------------------
// "field-information"-instances will contain a read-function and a write-function, so that they
// know how to read and write field data.
// The functions need the skill-state to look up references to user types, and the string-access
// for string fields. Write functions return the number of bytes written.
// ---------------------------------------------------------
typedef void ${prefix}read_function ( ${prefix}skill_state, ${prefix}string_access, ${prefix}skill_type, char** );
typedef int64_t ${prefix}write_function (${prefix}skill_state, ${prefix}string_access, ${prefix}skill_type, ${prefix}binary_writer);

typedef struct ${prefix}field_information_struct *${prefix}field_information;

typedef struct ${prefix}field_information_struct {
    char *name;
    ${prefix}type_information type_info;
    ${prefix}read_function *read;
    ${prefix}write_function *write;
} ${prefix}field_information_struct;

${prefix}field_information ${prefix}field_information_new ();

void ${prefix}field_information_destroy ( ${prefix}field_information this );

#endif /* FIELD_INFORMATION_H_ */
