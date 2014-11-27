#include <stdlib.h>
#include "../model/${prefix}field_information.h"

${prefix}field_information ${prefix}field_information_new () {
    ${prefix}field_information result = malloc ( sizeof ( ${prefix}field_information_struct ) );
    result->name = 0;
    result->type_info = 0;
    result->read = 0;
    result->write = 0;
    return result;
}

void ${prefix}field_information_destroy ( ${prefix}field_information this ) {
    if ( this->type_info ) {
        ${prefix}type_information_destroy ( this->type_info );
    }
    free ( this );
}
