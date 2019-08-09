/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.model

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.c.GeneralOutputMaker

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait TypesHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"model/${prefix}types.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#ifndef ${prefixCapital}TYPES_H_
#define ${prefixCapital}TYPES_H_

#include <glib.h>
#include <stdbool.h>

#include "../api/${prefix}api.h"

struct ${prefix}type_declaration_struct;

typedef struct ${prefix}skill_type_struct {
    int64_t skill_id;
    struct ${prefix}type_declaration_struct *declaration;
    ${prefix}skill_state state;
} ${prefix}skill_type_struct;
""")
    for (t ← IR)
      out.write(s"""
typedef struct ${prefix}${name(t)}_struct {
    ${prefix}${name(t.getSuperType)}_struct _super_type;${
        (for (f ← t.getFields) yield s"""
    ${mapType(f.getType)} ${name(f)};""").mkString
      }
} ${prefix}${name(t)}_struct;

${prefix}${name(t)} ${prefix}types_create_${name(t)} (
    ${prefix}skill_state state,
    int64_t skill_id${
        makeConstructorArguments(t)
      });
""")

    out.write("\n#endif\n")
    out.close()
  }
}
