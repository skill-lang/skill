/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.api

import scala.collection.JavaConversions._
import java.io.PrintWriter
import de.ust.skill.generator.c.GeneralOutputMaker
import de.ust.skill.ir.UserType

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait ApiHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"api/${prefix}api.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""#ifndef ${prefixCapital}API_H_
#define ${prefixCapital}API_H_

#include <glib.h>
#include <stdbool.h>

/*
    Skill State
--------------------
*/
struct ${prefix}skill_state_struct;
typedef struct ${prefix}skill_state_struct *${prefix}skill_state;

//! Creates an empty skill_state
${prefix}skill_state ${prefix}empty_skill_state ();

//! Frees all memory allocated to the skill_state.
//! User-type lists returned by the api are not owned by the state and have to be freed manually.
void ${prefix}delete_skill_state ( ${prefix}skill_state state );

//! Creates a skill_state from the given binary file loading all instances from the file.
${prefix}skill_state ${prefix}skill_state_from_file ( char *file_path );

//! Serializes all information contained in the given state to the given binary file.
void ${prefix}write_to_file ( ${prefix}skill_state state, char *file_path );

//! Updates the file, the given state was created from, i.e. removing deleted
//! instances, adding created instances or fields, and updating indices.
void ${prefix}append_to_file ( ${prefix}skill_state state );

//-------------------------------------------------------------------------------------------------

/*
    User Types
------------------

These types represent all user types contained in the specification file this binding was created from.
Typecasting is supported. Use the instanceof<typename> functions to determine the actual type.
Casting to a super-type is always safe.
${
      if (unsafe) "IMPORTANT: If you access an instance cast to the wrong type, the behavior is undefined."
      else "IMPORTANT: If you access an instance cast to the wrong type, the program execution will terminate."
    }
*/

// This is used as super type for all user types.
typedef struct ${prefix}skill_type_struct *${prefix}skill_type;
${
      (for (t ← IR) yield s"""
struct ${prefix}${t.getName.cStyle}_struct;
${comment(t)}typedef struct ${prefix}${t.getName.cStyle}_struct *${prefix}${t.getName.cStyle};
""").mkString
    }
// These methods return lists containing all instances of a certain type, including all subtypes.
// The returned lists are not owned by the state and have to be freed manually.

GList *${prefix}get_all_instances ( ${prefix}skill_state state );${
      // access method
      (for (t ← IR) yield s"""
GList *${prefix}get_${t.getName.cStyle}_instances(${prefix}skill_state state);""").mkString
    }
${
      // instance constructor
      (for (t ← IR) yield s"""
${prefix}${t.getName.cStyle} ${prefix}create_${t.getName.cStyle}(${prefix}skill_state state${makeConstructorArguments(t)});""").mkString
    }

void ${prefix}delete_instance ( ${prefix}skill_type instance );
${
      // get and set methods
      // @todo should instance be of type void*? (declared as "typedef void* sf_any")
      (for (t ← IR; f ← t.getAllFields) yield s"""
${comment(f)}${mapType(f.getType)} $prefix${t.getName.cStyle}_get_${f.getName.cStyle}(${prefix}${t.getName.cStyle} instance);
${comment(f)}void $prefix${t.getName.cStyle}_set_${f.getName.cStyle}(${prefix}${t.getName.cStyle}, ${mapType(f.getType)});
""").mkString
    }
${
      // instance-of operator
      (for (t ← IR) yield s"""
bool ${prefix}instanceof_${t.getName.cStyle}(${prefix}skill_type instance);""").mkString
    }

#endif
""")

    out.close()
  }
}
