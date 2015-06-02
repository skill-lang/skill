/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c.io

import scala.collection.JavaConversions._
import java.io.PrintWriter
import de.ust.skill.generator.c.GeneralOutputMaker
import de.ust.skill.ir.UserType

/**
 * @author Fabian Harth, Timm Felden
 * @todo rename skill state to skill file
 * @todo ensure 80 characters margin
 */
trait WriterHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"io/${prefix}writer.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#ifndef ${prefixCapital}WRITER_H_
#define ${prefixCapital}WRITER_H_

#include "../model/${prefix}skill_state.h"

void ${prefix}write ( ${prefix}skill_state state, char *filename );

void ${prefix}append ( ${prefix}skill_state state );

#endif
""")

    out.close()
  }
}
