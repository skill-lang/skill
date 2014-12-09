/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
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
trait TypeDeclarationHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("model/type_declaration.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""#ifndef ${prefixCapital}TYPE_DECLARATION_H_
#define ${prefixCapital}TYPE_DECLARATION_H_

#include <glib.h>
#include "../model/${prefix}types.h"

struct ${prefix}type_declaration_struct;
typedef struct ${prefix}type_declaration_struct *${prefix}type_declaration;

/**
 * Instances are not deleted directly, but just get their skill-id set to 0.
 * Therefore, we need a cleanup function to set references deleted instances to
 * null.
 */
typedef void ${prefix}cleanup_function(${prefix}skill_type instance);

/**
 * This stores information about a user-defined type. There is no order defined
 * on the fields here, as the order may vary between different binary files.
 * Also, a binary file may add additional fields, which are not known by the
 * binding.
 */
typedef struct ${prefix}type_declaration_struct {
    char *name;
    ${prefix}cleanup_function *remove_null_references;

    //! The super-type, if it has one.
    ${prefix}type_declaration super_type;

    //! This is a mapping field-name -> field_information for all its fields.
    GHashTable *fields;

    //! The number of bytes of one instance of this type in memory.
    int64_t size;
} ${prefix}type_declaration_struct;

${prefix}type_declaration ${prefix}type_declaration_new();

void ${prefix}type_declaration_destroy(${prefix}type_declaration instance);

#endif /* TYPE_DECLARATION_H */
""")

    out.close()
  }
}
