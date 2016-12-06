/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
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
trait BinaryWriterSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"io/${prefix}binary_writer.c")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "../io/${prefix}binary_writer.h"

typedef struct ${prefix}binary_writer_struct {
    ${prefix}data head;
    ${prefix}data tail;
    int64_t size;
} ${prefix}binary_writer_struct;

typedef struct ${prefix}data_struct {
    struct ${prefix}data_struct *next;
    char data[8 * 1024];
    int used;
} ${prefix}data_struct;

/*
To store the data to be written out to a file, 'Data' structs are used.
Those build up a list, which can be extended at runtime, when more bytes are required.
*/

static void swapEndian ( char source[], int size ) {
    int k;
    char result[size];
    for ( k = 0; k < size; k++ ) {
        result[k] = source[size - k - 1];
    }
    for ( k = 0; k < size; k++ ) {
        source[k] = result[k];
    }
}

static void data_destroy ( ${prefix}data this ) {
    free ( this );
}

static ${prefix}data data_new () {
    ${prefix}data result = malloc ( sizeof ( ${prefix}data_struct ) );
    result->next = 0;
    result->used = 0;
    return result;
}

static ${prefix}data data_new_append ( ${prefix}data tail ) {
    ${prefix}data result = malloc ( sizeof ( ${prefix}data_struct ) );
    result->next = 0;
    result->used = 0;
    tail->next = result;
    return result;
}

${prefix}binary_writer ${prefix}binary_writer_new () {
    ${prefix}binary_writer result = malloc ( sizeof ( ${prefix}binary_writer_struct ) );
    result->head = data_new ();
    result->size = 1;
    result->tail = result->head;
    return result;
}

void ${prefix}binary_writer_destroy ( ${prefix}binary_writer this ) {
    ${prefix}data iterator = this->head;
    ${prefix}data previous_data = this->head;
    while ( iterator != this->tail ) {
        previous_data = iterator;
        iterator = iterator->next;
        data_destroy ( previous_data );
    }
    free ( this );
}

static void put_char ( ${prefix}binary_writer this, char data ) {
    if ( sizeof ( this->tail->data ) - this->tail->used < 1 ) {
        this->tail = data_new_append ( this->tail );
    }
    this->tail->data[this->tail->used] = data;
    this->tail->used++;
    this->size++;
}

static void put_char_array ( ${prefix}binary_writer this, char data[], size_t number_of_bytes ) {
    this->tail = data_new_append ( this->tail );
    memcpy ( this->tail->data, data, number_of_bytes );
    this->tail->used += number_of_bytes;
    this->size += sizeof ( data );
}

int8_t ${prefix}write_bool ( ${prefix}binary_writer this, bool value ) {
    int8_t value_as_int = 0;
    if ( value ) {
        value_as_int = -1;
    }
    put_char ( this, value_as_int );
    return 1;
}

int8_t ${prefix}write_i8 ( ${prefix}binary_writer this, int8_t i8 ) {
    put_char ( this, i8 );
    return 1;
}

int8_t ${prefix}write_i16 ( ${prefix}binary_writer this, int16_t i16 ) {
    int i;
    char result[2];
    for ( i = 0; i < 2; i++ ) {
        result[i] = ( i16 >> ( 8 * i ) ) & 0xff; // This writes the byte at position i into the char array.
    }
    swapEndian ( result, 2 );
    put_char_array ( this, result, 2 );
    return 2;
}

int8_t ${prefix}write_i32 ( ${prefix}binary_writer this, int32_t i32 ) {
    int i;
    char result[4];
    for ( i = 0; i < 4; i++ ) {
        result[i] = ( i32 >> ( 8 * i ) ) & 0xff; // This writes the byte at position i into the char array.
    }
    swapEndian ( result, 4 );
    put_char_array ( this, result, 4 );
    return 4;
}

int8_t ${prefix}write_i64 ( ${prefix}binary_writer this, int64_t i64 ) {
    int i;
    char result[8];
    for ( i = 0; i < 8; i++ ) {
        result[i] = ( i64 >> ( 8 * i ) ) & 0xff; // This writes the byte at position i into the char array.
    }
    swapEndian ( result, 8 );
    put_char_array ( this, result, 8 );
    return 8;
}

// Writes the given v64 to the given position and returns the number of bytes written.
// The given position must have at least 9 free bytes left.
static int8_t write_v64_internal ( ${prefix}data position, int64_t v64 ) {
    char *data = position->data;
    int off = position->used;
    if (0L == ( v64 & 0xFFFFFFFFFFFFFF80L ) ) {
        data[off++] = v64;
    } else if (0L == ( v64 & 0xFFFFFFFFFFFFC000L ) ) {
        data[off++] = ( 0x80L | v64 );
        data[off++] = ( v64 >> 7 );
    } else if (0L == ( v64 & 0xFFFFFFFFFFE00000L ) ) {
        data[off++] = ( 0x80L | v64 );
        data[off++] = ( 0x80L | v64 >> 7 );
        data[off++] = ( v64 >> 14 );
    } else if (0L == ( v64 & 0xFFFFFFFFF0000000L ) ) {
        data[off++] = ( 0x80L | v64 );
        data[off++] = ( 0x80L | v64 >> 7 );
        data[off++] = ( 0x80L | v64 >> 14 );
        data[off++] = ( v64 >> 21 );
    } else if ( 0L == ( v64 & 0xFFFFFFF800000000L ) ) {
        data[off++] = ( 0x80L | v64 );
        data[off++] = ( 0x80L | v64 >> 7 );
        data[off++] = ( 0x80L | v64 >> 14 );
        data[off++] = ( 0x80L | v64 >> 21 );
        data[off++] = ( v64 >> 28);
    } else if (0L == ( v64 & 0xFFFFFC0000000000L ) ) {
        data[off++] = ( 0x80L | v64 );
        data[off++] = ( 0x80L | v64 >> 7 );
        data[off++] = ( 0x80L | v64 >> 14 );
        data[off++] = ( 0x80L | v64 >> 21 );
        data[off++] = ( 0x80L | v64 >> 28 );
        data[off++] = ( v64 >> 35 );
    } else if ( 0L == ( v64 & 0xFFFE000000000000L ) ) {
        data[off++] = ( 0x80L | v64 );
        data[off++] = ( 0x80L | v64 >> 7 );
        data[off++] = ( 0x80L | v64 >> 14 );
        data[off++] = ( 0x80L | v64 >> 21 );
        data[off++] = ( 0x80L | v64 >> 28 );
        data[off++] = ( 0x80L | v64 >> 35 );
        data[off++] = ( v64 >> 42);
    } else if ( 0L == ( v64 & 0xFF00000000000000L ) ) {
        data[off++] = ( 0x80L | v64 );
        data[off++] = ( 0x80L | v64 >> 7 );
        data[off++] = ( 0x80L | v64 >> 14 );
        data[off++] = ( 0x80L | v64 >> 21 );
        data[off++] = ( 0x80L | v64 >> 28 );
        data[off++] = ( 0x80L | v64 >> 35 );
        data[off++] = ( 0x80L | v64 >> 42 );
        data[off++] = ( v64 >> 49 );
    } else {
        data[off++] = ( 0x80L | v64 );
        data[off++] = ( 0x80L | v64 >> 7 );
        data[off++] = ( 0x80L | v64 >> 14 );
        data[off++] = ( 0x80L | v64 >> 21 );
        data[off++] = ( 0x80L | v64 >> 28 );
        data[off++] = ( 0x80L | v64 >> 35 );
        data[off++] = ( 0x80L | v64 >> 42 );
        data[off++] = ( 0x80L | v64 >> 49 );
        data[off++] = ( v64 >> 56 );
    }
    int result = off - position->used;
    position->used = off;
    return result;
}

int8_t ${prefix}write_v64 ( ${prefix}binary_writer this, int64_t v64 ) {
    if ( ( sizeof ( this->tail->data ) ) - this->tail->used < 9 ) {
        this->tail = data_new_append ( this->tail );
    }
    int result = write_v64_internal ( this->tail, v64 );
    this->size += result;
    return result;
}

int8_t ${prefix}write_f32 ( ${prefix}binary_writer this, float f32 ) {
    char result[4];
    memcpy ( result, &f32, 4 );
    swapEndian ( result, 4 );
    put_char_array ( this, result, 4 );
    return 4;
}

int8_t ${prefix}write_f64 ( ${prefix}binary_writer this, double f64 ) {
    char result[8];
    memcpy ( result, &f64, 8 );
    swapEndian ( result, 8 );
    put_char_array ( this, result, 8 );
    return 8;
}

int32_t ${prefix}write_string ( ${prefix}binary_writer this, char *string ) {
    put_char_array ( this, string, strlen ( string ) );
    return strlen ( string );
}

${prefix}data ${prefix}binary_writer_reserve_data ( ${prefix}binary_writer this ) {
    this->tail = data_new_append ( this->tail );
    ${prefix}data result = this->tail;
    this->tail = data_new_append ( this->tail );
    return result;
}

int8_t ${prefix}write_delayed_v64 ( ${prefix}binary_writer this, ${prefix}data position, int64_t v64 ) {
    int8_t result = write_v64_internal ( position, v64 );
    this->size += result;
    return result;
}

void ${prefix}binary_writer_write_to_file ( ${prefix}binary_writer this, char *filename ) {
    // First, determine the number of bytes, that will be written.
    int number_of_bytes = 0;
    ${prefix}data iterator = this->head;
    number_of_bytes += iterator->used;
    while (iterator->next) {
        iterator = iterator->next;
        number_of_bytes += iterator->used;
    }

    // open the file to be written to and write the bytes from each Data el_structement
    FILE *output_file = fopen ( filename, "w");
    if ( output_file == NULL ) {
        // TODO
    }
    iterator = this->head;
    fwrite ( iterator->data, 1, iterator->used, output_file );
    while ( iterator->next ) {
        iterator = iterator->next;
        fwrite ( iterator->data, 1, iterator->used, output_file );
    }
    fclose ( output_file );
}

void ${prefix}binary_writer_append_to_file ( ${prefix}binary_writer this, char *filename ) {
    // First, determine the number of bytes, that will be written.
    int number_of_bytes = 0;
    ${prefix}data iterator = this->head;
    number_of_bytes += iterator->used;
    while (iterator->next) {
        iterator = iterator->next;
        number_of_bytes += iterator->used;
    }

    // open the file to be appended to, and write the bytes from each Data element
    FILE *output_file = fopen ( filename, "a");
    if ( output_file == NULL ) {
        // TODO
    }
    iterator = this->head;
    fwrite ( iterator->data, 1, iterator->used, output_file );
    while ( iterator->next ) {
        iterator = iterator->next;
        fwrite ( iterator->data, 1, iterator->used, output_file );
    }
    fclose ( output_file );
}
""")

    out.close()
  }
}
