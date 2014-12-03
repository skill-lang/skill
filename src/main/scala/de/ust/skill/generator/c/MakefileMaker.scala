/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c

import scala.collection.JavaConversions._

trait MakefileMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""makefile""")

    out.write(s"""
penis
""")

    out.close()
  }
}
