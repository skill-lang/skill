/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.model

import scala.collection.JavaConversions._
import java.io.PrintWriter
import de.ust.skill.generator.c.GeneralOutputMaker
import de.ust.skill.ir.UserType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.Field
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.ReferenceType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.ContainerType
import de.ust.skill.ir.Type

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait SkillStateHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"model/${prefix}skill_state.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#ifndef ${prefix.toUpperCase}SKILL_STATE_H_
#define ${prefix.toUpperCase}SKILL_STATE_H_

#include <glib.h>
#include "../model/${prefix}string_access.h"
#include "../model/${prefix}storage_pool.h"
#include "../model/${prefix}types.h"

typedef struct ${prefix}skill_state_struct{${
      // create access
      (for (t ← IR) yield s"""
    ${prefix}storage_pool ${name(t)};""").mkString
}
    //! <type_name> -> storage_pool
    GHashTable *pools;
    ${prefix}string_access strings;

    //! stores the name of the file from which this state was created.
    //! This is NULL, if not created from a file.
    char *filename;
} ${prefix}skill_state_struct;

${prefix}skill_state ${prefix}skill_state_new();

void ${prefix}skill_state_delete_internal(${prefix}skill_state this);

#endif /* SKILL_STATE_H_ */
""")

    out.close()
  }
}
