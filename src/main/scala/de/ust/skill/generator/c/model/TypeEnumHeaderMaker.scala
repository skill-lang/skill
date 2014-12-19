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
trait TypeEnumHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"model/${prefix}type_enum.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#ifndef ${prefixCapital}TYPE_ENUM_H_
#define ${prefixCapital}TYPE_ENUM_H_

#include <stdbool.h>
#include <stdint.h>

//! @todo abuse this as type ID?
typedef enum ${prefix}type_enum {
    ${prefix}CONSTANT_I8 = 0,
    ${prefix}CONSTANT_I16,
    ${prefix}CONSTANT_I32,
    ${prefix}CONSTANT_I64,
    ${prefix}CONSTANT_V64,
    ${prefix}I8,
    ${prefix}I16,
    ${prefix}I32,
    ${prefix}I64,
    ${prefix}V64,
    ${prefix}ANNOTATION,
    ${prefix}BOOL,
    ${prefix}F32,
    ${prefix}F64,
    ${prefix}STRING,
    ${prefix}CONSTANT_LENGTH_ARRAY,
    ${prefix}VARIABLE_LENGTH_ARRAY,
    ${prefix}LIST,
    ${prefix}SET,
    ${prefix}MAP,
    ${prefix}USER_TYPE
} ${prefix}type_enum;

static inline const char *${prefix}type_enum_to_string(${prefix}type_enum type) {
    static const char *strings[] = {
    "constant i8",
    "constant i16",
    "constant i32",
    "constant i64",
    "constant v64",
    "i8",
    "i16",
    "i32",
    "i64",
    "v64",
    "annotation",
    "bool",
    "f32",
    "f64",
    "string",
    "constant length array",
    "variable length array",
    "list",
    "set",
    "map",
    "user defined type" };

    return strings[type];
}

//! @todo remove this shit, as enums are ints
int8_t ${prefix}type_enum_to_int(${prefix}type_enum type);

${prefix}type_enum ${prefix}type_enum_from_int(int8_t type_id);

bool ${prefix}type_enum_is_constant(${prefix}type_enum type);

bool ${prefix}type_enum_is_container_type(${prefix}type_enum type);

#endif /* TYPE_ENUM_H_ */
""")

    out.close()
  }
}
