/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.io

import scala.collection.JavaConversions._
import java.io.PrintWriter
import de.ust.skill.generator.c.GeneralOutputMaker
import de.ust.skill.ir.UserType

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait BinaryReaderHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"io/${prefix}binary_reader.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#ifndef ${prefixCapital}BINARY_READER_H_
#define ${prefixCapital}BINARY_READER_H_

#include <stdio.h>
#include <stdbool.h>
#include <stdint.h>

#include "../model/${prefix}types.h"

bool ${prefix}read_bool ( char **buffer );

int8_t ${prefix}read_i8 ( char **buffer );

int16_t ${prefix}read_i16 ( char **buffer );

int32_t ${prefix}read_i32 ( char **buffer );

int64_t ${prefix}read_i64 ( char **buffer );

int64_t ${prefix}read_v64 ( char **buffer );

float ${prefix}read_f32 ( char **buffer );

double ${prefix}read_f64 ( char **buffer );

char *${prefix}read_string ( char **buffer, int32_t size );

#endif /* BINARY_READER_H_ */
""")

    out.close()
  }
}
