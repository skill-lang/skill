/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.model

import scala.collection.JavaConversions._
import java.io.PrintWriter
import de.ust.skill.generator.c.GeneralOutputMaker
import de.ust.skill.ir.UserType

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait StringAccessSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"model/${prefix}string_access.c")

    out.write(s"""
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <glib.h>

#include "${prefix}string_access.h"

${prefix}string_access ${prefix}string_access_new() {
    ${prefix}string_access result = malloc(sizeof(${prefix}string_access_struct));
    result->strings_by_id = g_hash_table_new(g_int64_hash, g_int64_equal);
    result->ids_by_string = g_hash_table_new(g_str_hash, g_str_equal);
    result->current_skill_id = 1;
    return result;
}

void ${prefix}string_access_destroy(${prefix}string_access this) {
    // Don't delete the strings, as they should still be references elsewhere
    // (from instances or from type_information/field_information).
    // The ids need to be deleted though.
    GList *ids = g_hash_table_get_keys(this->strings_by_id);
    GList *iterator;
    for(iterator = ids; iterator; iterator = iterator->next) {
        free((int64_t*) iterator->data);
    }
    g_list_free(ids);
    g_hash_table_destroy(this->strings_by_id);

    // For the other map, ids_by_string, the values are already deleted now.
    g_hash_table_destroy(this->ids_by_string);
    free(this);
}

int64_t ${prefix}string_access_get_id_by_string(${prefix}string_access this, char *string) {
    int64_t *result = (int64_t*) g_hash_table_lookup(this->ids_by_string, string);
    if(result == 0) {
        return 0;
    }
    return *result;
}

bool ${prefix}string_access_contains_string(${prefix}string_access this, char *string) {
    int64_t *result = (int64_t*) g_hash_table_lookup(this->ids_by_string, string);
    return(result != 0);
}

char *${prefix}string_access_get_string_by_id(${prefix}string_access this, int64_t skill_id) {
    char *result = (char*) g_hash_table_lookup(this->strings_by_id, &skill_id);
    return result;
}

int64_t ${prefix}string_access_add_string(${prefix}string_access this, char *string) {
    if(string == 0) {
        return 0;
    }

    int64_t *id = (int64_t*) g_hash_table_lookup(this->ids_by_string, string);
    if(id != 0) {
        // The string has already been added.
        return *id;
    }
    id = malloc(sizeof(int64_t));
    *id = this->current_skill_id;
    this->current_skill_id++;
    g_hash_table_insert(this->strings_by_id, id, string);
    g_hash_table_insert(this->ids_by_string, string, id);
    return *id;
}

GList *${prefix}string_access_get_all_strings(${prefix}string_access this) {
    return(g_hash_table_get_values(this->strings_by_id));
}

int64_t ${prefix}string_access_get_size(${prefix}string_access this) {
    int64_t size = g_hash_table_size(this->strings_by_id);
    if(g_hash_table_size(this->ids_by_string) != size) {
        printf("Error: wrong size of string_access.\\n");
        exit(EXIT_FAILURE);
    }
    return size;
}
""")

    out.close()
  }
}
