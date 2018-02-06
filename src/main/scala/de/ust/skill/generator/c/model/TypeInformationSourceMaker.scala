/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.model

import de.ust.skill.generator.c.GeneralOutputMaker

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait TypeInformationSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"model/${prefix}type_information.c")

    out.write(s"""
#include <stdlib.h>
#include <glib.h>

#include "../model/${prefix}type_information.h"

${prefix}type_information ${prefix}type_information_new() {
    ${prefix}type_information result = malloc(sizeof(${prefix}type_information_struct));
    result->constant_value = 0;
    result->element_type = 0;
    result->array_length = 0;
    result->base_types = 0;
    return result;
}

void ${prefix}type_information_destroy(${prefix}type_information instance) {
    if(instance->element_type) {
        ${prefix}type_information_destroy(instance->element_type);
    }
    GList *iterator;
    for(iterator = instance->base_types; iterator; iterator = iterator->next) {
        ${prefix}type_information_destroy((${prefix}type_information) iterator->data);
    }
    g_list_free(instance->base_types);
    free(instance);
}
""")

    out.close()
  }
}
