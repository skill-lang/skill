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
trait ReaderHeaderMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"io/${prefix}reader.h")

    val prefixCapital = packagePrefix.toUpperCase

    out.write(s"""
#ifndef ${prefixCapital}READER_H_
#define ${prefixCapital}READER_H_

#include "../model/${prefix}skill_state.h"

void ${prefix}read_file(${prefix}skill_state state, char *filename);

#endif
""")

    out.close()
  }
}
