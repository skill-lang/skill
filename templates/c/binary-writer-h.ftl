#ifndef ${prefix_capital}BINARY_WRITER_H_
#define ${prefix_capital}BINARY_WRITER_H_

#include <stdio.h>
#include <stdbool.h>
#include <stdint.h>
#include "../model/${prefix}types.h"

typedef struct ${prefix}data_struct *${prefix}data;

typedef struct ${prefix}binary_writer_struct *${prefix}binary_writer;

${prefix}binary_writer ${prefix}binary_writer_new ();

void ${prefix}binary_writer_destroy ( ${prefix}binary_writer this );

int8_t ${prefix}write_bool ( ${prefix}binary_writer this, bool value );

int8_t ${prefix}write_i8 ( ${prefix}binary_writer this, int8_t i8 );

int8_t ${prefix}write_i16 ( ${prefix}binary_writer this, int16_t i16 );

int8_t ${prefix}write_i32 ( ${prefix}binary_writer this, int32_t i32 );

int8_t ${prefix}write_i64 ( ${prefix}binary_writer this, int64_t i64 );

int8_t ${prefix}write_v64 ( ${prefix}binary_writer this, int64_t v64 );

int8_t ${prefix}write_f32 ( ${prefix}binary_writer this, float f32 );

int8_t ${prefix}write_f64 ( ${prefix}binary_writer this, double f64 );

int32_t ${prefix}write_string ( ${prefix}binary_writer this, char *string );

${prefix}data ${prefix}binary_writer_reserve_data ( ${prefix}binary_writer this );

int8_t ${prefix}write_delayed_v64 ( ${prefix}binary_writer this, ${prefix}data position, int64_t v64 );

void ${prefix}binary_writer_write_to_file ( ${prefix}binary_writer this, char *filename );

void ${prefix}binary_writer_append_to_file ( ${prefix}binary_writer this, char *filename );

#endif /* BINARY_WRITER_H_ */
