/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
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
trait FieldInformationHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"model/${prefix}field_information.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""#ifndef ${prefixCapital}FIELD_INFORMATION_H_
#define ${prefixCapital}FIELD_INFORMATION_H_

#include "../model/${prefix}type_information.h"
#include "../model/${prefix}skill_state.h"
#include "../model/${prefix}string_access.h"
#include "../model/${prefix}types.h"
#include "../io/${prefix}binary_writer.h"

// ---------------------------------------------------------
// "field-information"-instances will contain a read function and a write
// function, so that they know how to read and write field data.
// The functions need the skill-state to look up references to user types, and
// the string-access for string fields. Write functions return the number of
// bytes written.
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

#endif
""")

    out.close()
  }
}
