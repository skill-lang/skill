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
trait TypeInformationHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"model/${prefix}type_information.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#ifndef ${prefixCapital}TYPE_INFORMATION_H_
#define ${prefixCapital}TYPE_INFORMATION_H_

#include <glib.h>

#include "../model/${prefix}types.h"
#include "../model/${prefix}type_enum.h"

struct ${prefix}type_information_struct;
typedef struct ${prefix}type_information_struct *${prefix}type_information;

//! This stores information about a type of a user-type-field.
//! This may be
//!   - a simple type like integer, float, string, or boolean
//!   - a container type like array, list, set, or map
//!   - a pointer to a user-type
typedef struct ${prefix}type_information_struct {
    ${prefix}type_enum type;

    //! If this is a pointer to a user type, this holds its name.
    char *name;

    //! If this is a constant, this holds the constant value.
    int64_t constant_value;

    //! If this is an array, list, or set, this holds the type if its elements.
    ${prefix}type_information element_type;

    //! If this is a constant length array, this holds its length.
    int64_t array_length;

    //! If this is a map, it holds its types as list of type_information.
    GList *base_types;
} ${prefix}type_information_struct;

${prefix}type_information ${prefix}type_information_new();

void ${prefix}type_information_destroy(${prefix}type_information instance);

#endif /* TYPE_INFORMATION_H_ */
""")

    out.close()
  }
}
