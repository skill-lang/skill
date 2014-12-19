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
trait BinaryReaderSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"io/${prefix}binary_reader.c")

    out.write(s"""
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "../model/${prefix}types.h"
#include "../io/${prefix}binary_reader.h"

static void swap_endian ( char source[], int size ) {
    int k;
    char result[size];
    for ( k = 0; k < size; k++ ) {
        result[k] = source[size - k - 1];
    }
    for ( k = 0; k < size; k++ ) {
        source[k] = result[k];
    }
}

static char read_char ( char ** buffer ) {
    char result = **buffer;
    (*buffer)++;
    return result;
}

bool ${prefix}read_bool ( char **buffer ) {
    if ( ${prefix}read_i8 ( buffer ) ) {
        return true;
    }
    return false;
}

int8_t ${prefix}read_i8 ( char **buffer ) {
    int8_t result = **buffer;
    (*buffer)++;
    return result;
}

int16_t ${prefix}read_i16 ( char **buffer ) {
    int16_t result;
    char data[2];
    data[0] = read_char ( buffer );
    data[1] = read_char ( buffer );
    swap_endian ( data, 2 );
    memcpy ( &result, data, 2 );
    return result;
}

int32_t ${prefix}read_i32 ( char **buffer ) {
    int32_t result;
    char data[4];
    data[0] = read_char ( buffer );
    data[1] = read_char ( buffer );
    data[2] = read_char ( buffer );
    data[3] = read_char ( buffer );
    swap_endian ( data, 4 );
    memcpy ( &result, data, 4 );
    return result;
}

int64_t ${prefix}read_i64 ( char **buffer ) {
    int64_t result;
    char data[8];
    data[0] = read_char ( buffer );
    data[1] = read_char ( buffer );
    data[2] = read_char ( buffer );
    data[3] = read_char ( buffer );
    data[4] = read_char ( buffer );
    data[5] = read_char ( buffer );
    data[6] = read_char ( buffer );
    data[7] = read_char ( buffer );
    swap_endian ( data, 8 );
    memcpy ( &result, data, 8 );
    return result;
}

int64_t ${prefix}read_v64 ( char **buffer ) {
    int64_t count = 0;
    int64_t rval = 0;
    int8_t r = ${prefix}read_i8 ( buffer );
    while (count < 8 && 0 != (r & 0x80)) {
        rval |= (r & 0x7f) << (7 * count);

        count += 1;
        r = ${prefix}read_i8 ( buffer );
    }
    rval = (rval | (8 == count ? r : (r & 0x7f)) << (7 * count));
    return rval;
}

float ${prefix}read_f32 ( char **buffer ) {
    char *result;
    result = malloc (sizeof ( char ) * 4 );
    int i;
    for ( i = 0; i < 4; i ++ ) {
        result[i] = ${prefix}read_i8 ( buffer );
    }
    swap_endian ( result, 4 );
    float float_result;
    memcpy ( &float_result, result, 4 );
    return float_result;
}

double ${prefix}read_f64 ( char **buffer ) {
    char *result;
    result = malloc (sizeof ( char ) * 8 );
    int i;
    for ( i = 0; i < 8; i ++ ) {
        result[i] = ${prefix}read_i8 ( buffer );
    }
    swap_endian ( result, 8 );
    double double_result;
    memcpy ( &double_result, result, 8 );
    return double_result;
}


char *${prefix}read_string ( char **buffer, int32_t size ) {
    char *result = (char*) malloc ( size + 1 );
    int i;
    for ( i = 0; i < size; i++ ) {
        result[i] = ${prefix}read_i8 ( buffer );
    }
    result[size] = 0;
    return result;
}
""")

    out.close()
  }
}
