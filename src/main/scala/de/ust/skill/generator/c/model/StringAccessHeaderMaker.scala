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
trait StringAccessHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"model/${prefix}string_access.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""#ifndef ${prefixCapital}STRING_ACCESS_H_
#define ${prefixCapital}STRING_ACCESS_H_

#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include <glib.h>

typedef struct ${prefix}string_access_struct *${prefix}string_access;

/**
 * Wee need a mapping id->string and a mapping string->id. Both lookups should
 * be fast (NOT linear time in the number of strings). Thus, two maps are
 * stored here.
 */
typedef struct ${prefix}string_access_struct {
    //! Mapping <id -> string>
    GHashTable *strings_by_id;

    //! Mapping <string -> id>
    GHashTable *ids_by_string;

    int64_t current_skill_id;
} ${prefix}string_access_struct;

${prefix}string_access ${prefix}string_access_new();

void ${prefix}string_access_destroy(${prefix}string_access this);

//! Returns the string with the given id or null, if the id is invalid.
char *${prefix}string_access_get_string_by_id(${prefix}string_access this, int64_t skill_id);

//! Returns the id, this string got assigned.
int64_t ${prefix}string_access_add_string(${prefix}string_access this, char *string);

int64_t ${prefix}string_access_get_id_by_string(${prefix}string_access this, char *string);

//! Returns a list of *char. The list must be deallocated manually.
GList *${prefix}string_access_get_all_strings(${prefix}string_access this);

int64_t ${prefix}string_access_get_size(${prefix}string_access this);

#endif /* STRING_ACCESS_H_ */
""")

    out.close()
  }
}
