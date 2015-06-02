/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.api

import scala.collection.JavaConversions._
import java.io.PrintWriter
import de.ust.skill.generator.c.GeneralOutputMaker
import de.ust.skill.ir.UserType
import de.ust.skill.ir.Field

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait ApiSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"api/${prefix}api.c")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "../api/${prefix}api.h"

#include "../io/${prefix}reader.h"
#include "../io/${prefix}writer.h"

#include "../model/${prefix}types.h"
#include "../model/${prefix}skill_state.h"
#include "../model/${prefix}storage_pool.h"
#include "../model/${prefix}type_declaration.h"


${prefix}skill_state ${prefix}empty_skill_state() {
    return ${prefix}skill_state_new();
}

void ${prefix}delete_skill_state(${prefix}skill_state state) {
    ${prefix}skill_state_delete_internal(state);
}

// Reads the binary file at the given location and serializes all instances into this skill state.
${prefix}skill_state ${prefix}skill_state_from_file(char *file_path) {
    ${prefix}skill_state result = ${prefix}skill_state_new();
    ${prefix}read_file ( result, file_path );
    return result;
}

void ${prefix}write_to_file ( ${prefix}skill_state state, char *file_path ) {
    ${prefix}write ( state, file_path );
}

void ${prefix}append_to_file ( ${prefix}skill_state state ) {
    ${prefix}append ( state );
}

GList *${prefix}get_all_instances ( ${prefix}skill_state state ) {
    GList *result = 0;

    // only add instances of base-pools, so that instances are not added multiple times${
      (for (t ← IR; if t == t.getBaseType)
        yield s"""
    result = g_list_concat(result, ${prefix}storage_pool_get_instances(state->${t.getName.cStyle}));"""
      ).mkString
    }

    return result;
}

${
      (for (t ← IR)
        yield s"""
GList *${prefix}get_${t.getName.cStyle}_instances(${prefix}skill_state state) {
    return ${prefix}storage_pool_get_instances ( state->${t.getName.cStyle} );
}

${prefix}${t.getName.cStyle} ${prefix}create_${t.getName.cStyle}(${prefix}skill_state state${makeConstructorArguments(t)}) {
    ${prefix}${t.getName.cStyle} result = ${prefix}types_create_${t.getName.cStyle}(state, 0${t.getAllFields.filterNot(_.isConstant).map(f ⇒ s", _${name(f)}").fold("")(_ + _)});
    ((${prefix}skill_type)result)->declaration = state->${name(t)}->declaration;
    ((${prefix}skill_type)result)->skill_id = 1;
    ${prefix}storage_pool_add_instance ( state->${name(t)}, (${prefix}skill_type) result );
    return result;
}

bool ${prefix}instanceof_${t.getName.cStyle}(${prefix}skill_type instance) {
    ${prefix}type_declaration declaration = instance->state->${name(t)}->declaration;
    // We need to check all super-types here.
    ${prefix}type_declaration super_declaration = instance->declaration;
    while ( super_declaration ) {
        if ( super_declaration == declaration ) {
            return true;
        }
        super_declaration = super_declaration->super_type;
    }
    return false;
}
${
        (for (f ← t.getAllFields)
          yield s"""
${mapType(f.getType)} $prefix${t.getName.cStyle}_get_${f.getName.cStyle}(${prefix}${t.getName.cStyle} instance) {
${
          if (unsafe) ""
          else s"""    if(!${prefix}instanceof_${t.getName.cStyle}((${prefix}skill_type)instance)) {
        printf("Error: called method '${prefix}${t.getName.cStyle}_get_${f.getName.cStyle}()' on a struct, which is not an instance of that type.\\n");
        exit(EXIT_FAILURE);
    }
"""
        }    return ${access(f)};
}

void $prefix${t.getName.cStyle}_set_${f.getName.cStyle}(${prefix}${t.getName.cStyle} instance, ${mapType(f.getType)} _${escaped(f.getName.cStyle())}) {
${
          if (unsafe) ""
          else s"""    if(!${prefix}instanceof_${t.getName.cStyle}((${prefix}skill_type)instance)) {
        printf("Error: called method '${prefix}${t.getName.cStyle}_set_${f.getName.cStyle}()' on a struct, which is not an instance of that type.\\n");
        exit(EXIT_FAILURE);
    }
"""
        }    ${access(f)} = _${escaped(f.getName.cStyle())};
}
"""
        ).mkString
      }"""
      ).mkString
    }

void ${prefix}delete_instance ( ${prefix}skill_type instance ) {
    instance->skill_id = 0;
}
""")

    out.close()
  }
}
