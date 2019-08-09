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
trait TypeEnumSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"model/${prefix}type_enum.c")

    out.write(s"""
#include <stdlib.h>
#include <stdio.h>
#include "../model/${prefix}type_enum.h"

int8_t ${prefix}type_enum_to_int ( ${prefix}type_enum type ) {
    switch ( type ) {
    case ${prefix}CONSTANT_I8:
        return 0;
    case ${prefix}CONSTANT_I16:
        return 1;
    case ${prefix}CONSTANT_I32:
        return 2;
    case ${prefix}CONSTANT_I64:
        return 3;
    case ${prefix}CONSTANT_V64:
        return 4;
    case ${prefix}ANNOTATION:
        return 5;
    case ${prefix}BOOL:
        return 6;
    case ${prefix}I8:
        return 7;
    case ${prefix}I16:
        return 8;
    case ${prefix}I32:
        return 9;
    case ${prefix}I64:
        return 10;
    case ${prefix}V64:
        return 11;
    case ${prefix}F32:
        return 12;
    case ${prefix}F64:
        return 13;
    case ${prefix}STRING:
        return 14;
    case ${prefix}CONSTANT_LENGTH_ARRAY:
        return 15;
    case ${prefix}VARIABLE_LENGTH_ARRAY:
        return 17;
    case ${prefix}LIST:
        return 18;
    case ${prefix}SET:
        return 19;
    case ${prefix}MAP:
        return 20;
    case ${prefix}USER_TYPE:
        return 32;
    }
    return -1;
}

${prefix}type_enum ${prefix}type_enum_from_int ( int8_t type_id ) {
    switch ( type_id ) {
    case 0:
        return ${prefix}CONSTANT_I8;
    case 1:
        return ${prefix}CONSTANT_I16;
    case 2:
        return ${prefix}CONSTANT_I32;
    case 3:
        return ${prefix}CONSTANT_I64;
    case 4:
        return ${prefix}CONSTANT_V64;
    case 5:
        return ${prefix}ANNOTATION;
    case 6:
        return ${prefix}BOOL;
    case 7:
        return ${prefix}I8;
    case 8:
        return ${prefix}I16;
    case 9:
        return ${prefix}I32;
    case 10:
        return ${prefix}I64;
    case 11:
        return ${prefix}V64;
    case 12:
        return ${prefix}F32;
    case 13:
        return ${prefix}F64;
    case 14:
        return ${prefix}STRING;
    case 15:
        return ${prefix}CONSTANT_LENGTH_ARRAY;
    case 16:
        // TODO
        printf ( "Error: type id 16 not specified!" );
        exit ( EXIT_FAILURE );
        return 0;
    case 17:
        return ${prefix}VARIABLE_LENGTH_ARRAY;
    case 18:
        return ${prefix}LIST;
    case 19:
        return ${prefix}SET;
    case 20:
        return ${prefix}MAP;
    default:
        if ( type_id < 0 ) {
            // TODO
            printf ( "Error: type id < 0 not allowed!" );
            exit ( EXIT_FAILURE );
            return 0;
        } else if ( type_id > 20 && type_id < 32 ) {
            // TODO
            printf ( "Error: type-ids 21 - 31 are not defined." );
            exit ( EXIT_FAILURE );
            return 0;
        } else {
            return ${prefix}USER_TYPE;
        }
    }
}

bool ${prefix}type_enum_is_constant ( ${prefix}type_enum type ) {
    if ( type == ${prefix}CONSTANT_I8 || type == ${prefix}CONSTANT_I16 || type == ${prefix}CONSTANT_I32 || type == ${prefix}CONSTANT_I64 || type == ${prefix}CONSTANT_V64 ) {
        return true;
    }
    return false;
}

bool ${prefix}type_enum_is_container_type ( ${prefix}type_enum type ) {
    if ( type == ${prefix}CONSTANT_LENGTH_ARRAY || type == ${prefix}VARIABLE_LENGTH_ARRAY || type == ${prefix}LIST || type == ${prefix}SET || type == ${prefix}MAP ) {
        return true;
    }
    return false;
}
""")

    out.close()
  }
}
