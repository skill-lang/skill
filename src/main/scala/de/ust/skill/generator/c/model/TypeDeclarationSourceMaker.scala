/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.model

import de.ust.skill.generator.c.GeneralOutputMaker

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait TypeDeclarationSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"model/${prefix}type_declaration.c")

    out.write(s"""
#include <stdlib.h>
#include <glib.h>

#include "../model/${prefix}field_information.h"
#include "../model/${prefix}type_declaration.h"

${prefix}type_declaration ${prefix}type_declaration_new() {
    ${prefix}type_declaration result = malloc(sizeof(${prefix}type_declaration_struct));
    result->name = 0;
    result->super_type = 0;
    result->fields = g_hash_table_new(g_str_hash, g_str_equal);
    result->remove_null_references = 0;
    return result;
}

void ${prefix}type_declaration_destroy(${prefix}type_declaration instance) {
    // The super type is destroyed by the skill_state.
    if(instance->fields) {
        GList *iterator;
        GList *fields = g_hash_table_get_values(instance->fields);
        for(iterator = fields; iterator; iterator = iterator->next) {
            ${prefix}field_information_destroy((${prefix}field_information) iterator->data);
        }
        g_list_free(fields);
        g_hash_table_destroy(instance->fields);
    }
    free(instance);
}
""")

    out.close()
  }
}
