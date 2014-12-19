/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
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
trait TypesSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"model/${prefix}types.c")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#include <stdlib.h>
#include "../model/${prefix}types.h"
#include "../model/${prefix}skill_state.h"

static void set_skill_type_fields(${prefix}skill_type instance, ${prefix}skill_state state, int64_t skill_id) {
    instance->skill_id = skill_id;
    instance->state = state;
}

""")
    for (t ← IR)
      out.write(s"""
static void set_${name(t)}_fields(
    ${prefix}${name(t)} instance,
    ${prefix}skill_state state,
    int64_t skill_id${
        makeConstructorArguments(t)
      }) {
    ${
        // invoke super "constructor"
        if (null != t.getSuperType) s"""set_${name(t.getSuperType)}_fields(
        ${cast(t.getSuperType)}instance,
        state,
        skill_id${
          (for (f ← t.getSuperType.getAllFields; if !f.isConstant()) yield s""",
        _${name(f)}""").mkString
        });"""
        else s"set_skill_type_fields(${cast()}instance, state, skill_id);"
      }
${
        // assign fields
        (for (f ← t.getFields; if !f.isConstant()) yield s"""
    instance->${name(f)} = _${name(f)};""").mkString
      }
}

${prefix}${name(t)} ${prefix}types_create_${name(t)} (
    ${prefix}skill_state state,
    int64_t skill_id${
        makeConstructorArguments(t)
      }) {
    ${prefix}${name(t)} result = malloc(sizeof(${prefix}${name(t)}_struct));
    set_${name(t)}_fields(
        result,
        state,
        skill_id${
        (for (f ← t.getAllFields; if !f.isConstant()) yield s""",
        _${name(f)}""").mkString
      });
    return result;
}
""")

    out.close()
  }
}
