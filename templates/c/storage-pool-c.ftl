#include <stdlib.h>
#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <inttypes.h>
#include "../model/${prefix}storage_pool.h"

${prefix}storage_pool ${prefix}storage_pool_new ( ${prefix}type_declaration declaration ) {
    ${prefix}storage_pool result = malloc ( sizeof ( ${prefix}storage_pool_struct ) );
    result->declaration = declaration;
    result->declared_in_file = false;
    result->fields = 0;
    
    result->base_pool = 0;
    result->super_pool = 0;
    result->sub_pools = 0;
    
    result->instances = g_array_new ( true, true, sizeof (${prefix}skill_type) );
    result->new_instances = g_array_new ( true, true, sizeof (${prefix}skill_type) );
    // TODO do we need to store the pool id here?
    result->id = -1; // 0 is a valid pool id, so -1 means, that this pool has no id assigned.
    return result;
}

${prefix}skill_type ${prefix}storage_pool_get_instance_by_id ( ${prefix}storage_pool this, int64_t id ) {
    if ( id == 0 ) {
        return 0;
    }
    ${prefix}storage_pool base_pool = this->base_pool;
    if ( id > base_pool->instances->len ) {
        printf ( "Error: wrong index into storage pool. Type %s, index %" PRId64 ".\n", base_pool->declaration->name, id );
        exit ( EXIT_FAILURE );
        return 0;
    }
    return g_array_index ( base_pool->instances, ${prefix}skill_type, id - 1 );
}

void ${prefix}storage_pool_destroy ( ${prefix}storage_pool this ) {
    // The type_info will be destroyed by the skill_state object.
    // The fields will be destroyed by their type_information.
    g_list_free ( this->fields );
    // The base pool and the sub pool will be deleted from the skill_state.
    g_list_free ( this->sub_pools );
    
    // Free the actual instances
    int64_t i;
    ${prefix}skill_type current_instance;
    for ( i = 0; i < this->instances->len; i++ ) {
        current_instance = g_array_index ( this->instances, ${prefix}skill_type, i );
        // delete only, if the isntance is of this exact type,
        // otherwise, it will be deleted from a sub-pool
        if ( current_instance->declaration == this->declaration ) {
            free ( current_instance );
        }
    }
    for ( i = 0; i < this->new_instances->len; i++ ) {
        current_instance = g_array_index ( this->new_instances, ${prefix}skill_type, i );
        // delete only, if the isntance is of this exact type,
        // otherwise, it will be deleted from a sub-pool
        if ( current_instance->declaration == this->declaration ) {
            free ( current_instance );
        }
    }
    
    g_array_free ( this->instances, true );
    g_array_free ( this->new_instances, true );
    
    free ( this );
}

GList *${prefix}storage_pool_get_instances ( ${prefix}storage_pool this ) {
    GList *result = 0;
    int64_t i;
    ${prefix}skill_type current_instance;
    // Use a reverse loop, as g_list_prepend is a lot faster than g_list_append
    // first, collect instances, which were loaded from a file
    
    // For this->instances->len == 0, the subtraction '-1' does not work, as its type is 'guint'.
    // Therefore the if-check is required.
    if ( this->instances->len > 0 ) {
        for ( i = this->instances->len - 1; i > -1; i-- ) {
            current_instance = g_array_index ( this->instances, ${prefix}skill_type, i );
            if ( current_instance->skill_id != 0 ) {
                result = g_list_prepend ( result, current_instance );
            }
        }
    }
    
    // collect instances, that are created by the user and not yet appended
    if ( this->new_instances->len > 0 ) {
        for ( i = this->new_instances->len - 1; i > -1; i-- ) {
            current_instance = g_array_index ( this->new_instances, ${prefix}skill_type, i );
            if ( current_instance->skill_id != 0 ) {
                result = g_list_prepend ( result, current_instance );
            }
        }
    }
    return result;
}

GList *${prefix}storage_pool_get_new_instances ( ${prefix}storage_pool this ) {
    GList *result = 0;
    int64_t i;
    ${prefix}skill_type current_instance;
    // Use a reverse loop, as g_list_prepend is a lot faster than g_list_append
    
    // collect instances, that are created by the user and not yet appended
    if ( this->new_instances->len > 0 ) {
        for ( i = this->new_instances->len - 1; i > -1; i-- ) {
            current_instance = g_array_index ( this->new_instances, ${prefix}skill_type, i );
            if ( current_instance->skill_id != 0 ) {
                result = g_list_prepend ( result, current_instance );
            }
        }
    }
    return result;
}

void ${prefix}storage_pool_add_instance ( ${prefix}storage_pool this, ${prefix}skill_type instance ) {
    g_array_append_val ( this->new_instances, instance );
    if ( this->super_pool ) {
        ${prefix}storage_pool_add_instance ( this->super_pool, instance );
    }
}

void ${prefix}storage_pool_remove_null_references ( ${prefix}storage_pool this ) {
    GList *iterator;
    GList *instances = ${prefix}storage_pool_get_instances ( this );
    for ( iterator = instances; iterator; iterator = iterator->next ) {
        this->declaration->remove_null_references ( (${prefix}skill_type) iterator->data );
    }
    g_list_free ( instances );
    instances = ${prefix}storage_pool_get_new_instances ( this );
    for ( iterator = instances; iterator; iterator = iterator->next ) {
        this->declaration->remove_null_references ( (${prefix}skill_type) iterator->data );
    }
    g_list_free ( instances );
}

static void remove_deleted_instances ( ${prefix}storage_pool this ) {
    int64_t i;
    ${prefix}skill_type current_instance;
    
    // Remove instances of the 'instances'-array
    int64_t number_of_undeleted_instances = 0;
    for ( i = 0; i < this->instances->len; i++ ) {
        if ( ( g_array_index ( this->instances, ${prefix}skill_type, i )->skill_id != 0 ) ) {
            number_of_undeleted_instances++;
        }
    }
    GArray *new_array = g_array_sized_new ( true, true, sizeof (${prefix}skill_type), number_of_undeleted_instances );
    g_array_set_size ( new_array, number_of_undeleted_instances );
    int64_t new_array_index = 0;
    for ( i = 0; i < this->instances->len; i++ ) {
        current_instance = g_array_index ( this->instances, ${prefix}skill_type, i );
        if ( current_instance->skill_id != 0 ) {
            g_array_index ( new_array, ${prefix}skill_type, new_array_index ) = current_instance;
            new_array_index++;
        } else {
            // Free the memory occupied by this instance.
            // We just free memory here, if this instance has the exact type of the pool (no sub-type)
            // So that no dangling pointers are created in sub-pools.
            if ( current_instance->declaration == this->declaration ) {
                free ( current_instance );
            }
        }
    }
    g_array_free ( this->instances, true );
    this->instances = new_array;
    
    // Remove instances of the 'new_instances'-array
    number_of_undeleted_instances = 0;
    for ( i = 0; i < this->new_instances->len; i++ ) {
        if ( ( g_array_index ( this->new_instances, ${prefix}skill_type, i )->skill_id != 0 ) ) {
            number_of_undeleted_instances++;
        }
    }
    new_array = g_array_sized_new ( true, true, sizeof (${prefix}skill_type), number_of_undeleted_instances );
    g_array_set_size ( new_array, number_of_undeleted_instances );
    new_array_index = 0;
    for ( i = 0; i < this->new_instances->len; i++ ) {
        current_instance = g_array_index ( this->new_instances, ${prefix}skill_type, i );
        if ( current_instance->skill_id != 0 ) {
            g_array_index ( new_array, ${prefix}skill_type, new_array_index ) = current_instance;
            new_array_index++;
        } else {
            // Free the memory occupied by this instance.
            // We just free memory here, if this instance has the exact type of the pool (no sub-type)
            // So that no dangling pointers are created in sub-pools.
            if ( current_instance->declaration == this->declaration ) {
                free ( current_instance );
            }
        }
    }
    g_array_free ( this->new_instances, true );
    this->new_instances = new_array;
    
    // Call this on sub-pools
    GList *iterator;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        remove_deleted_instances ( (${prefix}storage_pool) iterator->data );
    }
}

// For writing, it does not matter, which instances were loaded from a file, and which were created by the user.
// This puts old and new instances together.
static void squash_instances ( ${prefix}storage_pool this ) {
    int64_t i;
    int64_t old_length = this->instances->len;
    g_array_set_size ( this->instances, old_length + this->new_instances->len );
    
    for ( i = 0; i < this->new_instances->len; i++ ) {
        g_array_index ( this->instances, ${prefix}skill_type, old_length + i ) = g_array_index ( this->new_instances, ${prefix}skill_type, i );
    }
    g_array_free ( this->new_instances, true );
    this->new_instances = g_array_new ( true, true, sizeof (${prefix}skill_type) );
    
    // Call this on sub-pools
    GList *iterator;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        squash_instances ( (${prefix}storage_pool) iterator->data );
    }
}

// This sorts the instance array, so that instances of the same subtype are grouped together
static void reorder_instances ( ${prefix}storage_pool this ) {
    GList *iterator;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        reorder_instances ( (${prefix}storage_pool) iterator->data );
    }
    GArray *new_array = g_array_sized_new ( true, true, sizeof (${prefix}skill_type), this->instances->len );
    g_array_set_size ( new_array, this->instances->len );
    int64_t number_of_sub_instances = 0;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        number_of_sub_instances += ( (${prefix}storage_pool) iterator->data )->instances->len;
    }
    int64_t index = 0;
    int64_t i;
    ${prefix}skill_type current_instance;
    for ( i = 0; i < this->instances->len; i++ ) {
        current_instance = g_array_index ( this->instances, ${prefix}skill_type, i );
        if ( current_instance->declaration == this->declaration ) {
            g_array_index ( new_array, ${prefix}skill_type, index ) = current_instance;
            index++;
        }
    }
    ${prefix}storage_pool sub_pool;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        sub_pool = (${prefix}storage_pool) iterator->data;
        for ( i = 0; i < sub_pool->instances->len; i++ ) {
            g_array_index ( new_array, ${prefix}skill_type, index ) = g_array_index ( sub_pool->instances, ${prefix}skill_type, i );
            index++;
        }
    }
    g_array_free ( this->instances, true );
    this->instances = new_array;
}

static void reorder_new_instances ( ${prefix}storage_pool this ) {
    GList *iterator;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        reorder_new_instances ( (${prefix}storage_pool) iterator->data );
    }
    GArray *new_array = g_array_sized_new ( true, true, sizeof (${prefix}skill_type), this->new_instances->len );
    g_array_set_size ( new_array, this->new_instances->len );
    int64_t number_of_sub_instances = 0;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        number_of_sub_instances += ( (${prefix}storage_pool) iterator->data )->new_instances->len;
    }
    int64_t index = 0;
    int64_t i;
    ${prefix}skill_type current_instance;
    for ( i = 0; i < this->new_instances->len; i++ ) {
        current_instance = g_array_index ( this->new_instances, ${prefix}skill_type, i );
        if ( current_instance->declaration == this->declaration ) {
            g_array_index ( new_array, ${prefix}skill_type, index ) = current_instance;
            index++;
        }
    }
    ${prefix}storage_pool sub_pool;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        sub_pool = (${prefix}storage_pool) iterator->data;
        for ( i = 0; i < sub_pool->new_instances->len; i++ ) {
            g_array_index ( new_array, ${prefix}skill_type, index ) = g_array_index ( sub_pool->new_instances, ${prefix}skill_type, i );
            index++;
        }
    }
    g_array_free ( this->new_instances, true );
    this->new_instances = new_array;
}

static void calculate_lbpsi ( ${prefix}storage_pool this, int64_t *lbpsi ) {
    this->lbpsi = *lbpsi;
    int64_t number_of_sub_instances = 0;
    GList *iterator;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        number_of_sub_instances += ( (${prefix}storage_pool) iterator->data )->instances->len;
    }
    *lbpsi += this->instances->len - number_of_sub_instances;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        calculate_lbpsi ( (${prefix}storage_pool) iterator->data, lbpsi );
    }
}

static void calculate_new_lbpsi ( ${prefix}storage_pool this, int64_t *lbpsi ) {
    this->lbpsi = *lbpsi;
    int64_t number_of_sub_instances = 0;
    GList *iterator;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        number_of_sub_instances += ( (${prefix}storage_pool) iterator->data )->new_instances->len;
    }
    *lbpsi += this->new_instances->len - number_of_sub_instances;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        calculate_new_lbpsi ( (${prefix}storage_pool) iterator->data, lbpsi );
    }
}

// Assigns skill-ids staring from the given id and incrementing it.
static void assign_ids ( ${prefix}storage_pool this ) {
    int64_t i;
    int64_t id = 1;
    for ( i = 0; i < this->instances->len; i++ ) {
        g_array_index ( this->instances, ${prefix}skill_type, i )->skill_id = id;
        id++;
    }
}

// Assigns skill-ids staring from the given id and incrementing it.
static void assign_new_ids ( ${prefix}storage_pool this, int64_t start_id ) {
    int64_t i;
    int64_t id = start_id;
    for ( i = 0; i < this->new_instances->len; i++ ) {
        g_array_index ( this->new_instances, ${prefix}skill_type, i )->skill_id = id;
        id++;
    }
}

// This method does everything to prepare this pool to be written to a binary file:
//   -put old and new instances together in one array
//   -remove deleted instances
//   -calculate lbpsi's
//   -set ids of instances
// Call this method only on pools of base-types. It will take care of subtypes itself.
void ${prefix}storage_pool_prepare_for_writing ( ${prefix}storage_pool this ) {

    // add the 'new_instance'-array to the 'instance'-array
    squash_instances ( this );
    
    remove_deleted_instances ( this );
    reorder_instances ( this );
    int64_t lbpsi = 0;
    calculate_lbpsi ( this, &lbpsi );
    assign_ids ( this );
}

// This method does everything to prepare this pool to be appended to a binary file:
//   -check, that previously written instances have not been modified
//   -remove deleted instances, which are not yet written to a file
//   -calculate lbpsi's
//   -set ids of instances
// Call this method only on pools of base-types. It will take care of subtypes itself.
void ${prefix}storage_pool_prepare_for_appending ( ${prefix}storage_pool this ) {
    // TODO check, that old instances have not been modified
    
    remove_deleted_instances ( this );
    reorder_new_instances ( this );
    int64_t lbpsi = 0;
    calculate_new_lbpsi ( this, &lbpsi );
    int64_t start_id = this->instances->len + 1;
    assign_new_ids ( this, start_id );

}

GList *${prefix}storage_pool_get_sub_pools ( ${prefix}storage_pool this ) {
    GList *result = 0;
    GList *iterator;
    ${prefix}storage_pool pool;
    for ( iterator = this->sub_pools; iterator; iterator = iterator->next ) {
        pool = (${prefix}storage_pool) iterator->data;
        result = g_list_append ( result, pool );
        result = g_list_concat ( result, ${prefix}storage_pool_get_sub_pools ( pool ) );
    }
    return result;
}

void ${prefix}storage_pool_mark_instances_as_appended ( ${prefix}storage_pool this ) {
    squash_instances ( this );
}
