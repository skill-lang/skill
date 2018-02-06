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
trait FieldInformationSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"model/${prefix}field_information.c")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
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
""")

    out.close()
  }
}
