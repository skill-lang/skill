#ifndef ${prefix_capital}BINARY_READER_H_
#define ${prefix_capital}BINARY_READER_H_

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
