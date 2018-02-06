/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
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
trait StoragePoolHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"model/${prefix}storage_pool.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#ifndef ${prefixCapital}STORAGE_POOL_H_
#define ${prefixCapital}STORAGE_POOL_H_

#include <glib.h>
#include <stdbool.h>

#include "../model/${prefix}type_declaration.h"
#include "../model/${prefix}types.h"
#include "../api/${prefix}api.h"

struct ${prefix}storage_pool_struct;

typedef struct ${prefix}storage_pool_struct *${prefix}storage_pool;

/**
 * This stores information about instances contained in one type block.
 * Instances which are created by the user, but are not yet serialized, do not
 * belong to a type block.
 */
typedef struct ${prefix}storage_pool_struct {

    /**
     * this is the id, this pool has in the binary file (defined by the order,
     * in which the types appear in the file). If this pool has not yet been
     * written to file, this is set to -1.
     */
    int64_t id;

    //! This is set to true, if this pool has already been written to a binary file.
    bool declared_in_file;

    ${prefix}type_declaration declaration;

    /**
     * list of field_information. This only contains fields from this pool's
     * declaration. This is empty, unless this pool has already been written to
     * file. Unknown fields are not stored at the moment and they will not be
     * written when appending.
     */
    GList *fields;

    //! This is the root of the inheritance tree. May point to itself.
    ${prefix}storage_pool base_pool;

    //! This is the direct super-type
    ${prefix}storage_pool super_pool;

    //! GList of ${prefix}storage_pool. These are only the direct sub-types.
    GList *sub_pools;

    /**
     * If this is a subtype, this field will be set before writing/appending.
     * This is the index of the first instance of this type in the instance array
     * of the base type.
     */
    int64_t lbpsi;

    /**
     * Array of skill_type*. This only includes instances of this exact type.
     * Instances of Sub-Types will be hold by the sub-pools. This contains instances,
     * that are already contained in a binary file.
     */
    GArray *instances;

    /**
     * Array of skill_type*. This only includes instances of this exact type.
     * Instances of Sub-Types will be hold by the sub-pools. This contains only
     * instances, that are user-created and not contained in a binary file.
     */
    GArray *new_instances;

} ${prefix}storage_pool_struct;

${prefix}storage_pool ${prefix}storage_pool_new(${prefix}type_declaration declaration);

void ${prefix}storage_pool_destroy(${prefix}storage_pool this);

${prefix}skill_type ${prefix}storage_pool_get_instance_by_id(${prefix}storage_pool this, int64_t skill_id);

//! Returns all instances of this type (including sub-types).
//! The list must be deallocated manually.
GList *${prefix}storage_pool_get_instances(${prefix}storage_pool this);

/**
 * Returns instances of this type (including sub-types), that are created by the user,
 * and not yet appended to the binary file. The list must be deallocated manually.
 */
GList *${prefix}storage_pool_get_new_instances(${prefix}storage_pool this);

//! This adds a user-created instance to the pool.
void ${prefix}storage_pool_add_instance(${prefix}storage_pool this, ${prefix}skill_type instance);

//! Deleted instances are not deleted immediately, but get their id set to null.
//! This function sets all references to deleted instances to null for instances of this pool.
void ${prefix}storage_pool_remove_null_references(${prefix}storage_pool this);

//! This method does everything to prepare this pool to be written to a binary file:
//!   -put old and new instances together in one array
//!   -remove deleted instances
//!   -calculate lbpsi's
//!   -set ids of instances
//! Call this method only on pools of base-types. It will take care of subtypes itself.
void ${prefix}storage_pool_prepare_for_writing(${prefix}storage_pool this);

//! This method does everything to prepare this pool to be appended to a binary file:
//!   -check, that previously written instances have not been modified
//!   -remove deleted instances, which are not yet written to a file
//!   -calculate lbpsi's
//!   -set ids of instances
//! Call this method only on pools of base-types. It will take care of subtypes itself.
void ${prefix}storage_pool_prepare_for_appending(${prefix}storage_pool this);

//! For writing/appending, we need pools ordered, so that sub-pools come after their super-pools.
//! This returns all sub-pools of this pool ordered that way, including all subtypes, not only direct subtypes.
GList *${prefix}storage_pool_get_sub_pools(${prefix}storage_pool this);

//! This merges new instances into the old instances after appending
void ${prefix}storage_pool_mark_instances_as_appended(${prefix}storage_pool this);

#endif /* STORAGE_POOL_H_ */
""")

    out.close()
  }
}
