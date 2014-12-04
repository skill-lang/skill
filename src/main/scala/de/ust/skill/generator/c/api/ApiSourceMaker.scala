/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
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
trait ApiSourceMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/api.c")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "../api/api.h"

#include "../io/reader.h"
#include "../io/writer.h"

#include "../model/types.h"
#include "../model/skill_state.h"
#include "../model/storage_pool.h"
#include "../model/type_declaration.h"
""")

    out.close()
  }
}
