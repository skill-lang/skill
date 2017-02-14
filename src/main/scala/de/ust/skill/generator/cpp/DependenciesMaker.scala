/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * creates a copy of skill.jvm.common.jar in $outPath
 */
trait DependenciesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    if (!skipDependencies)
      println("C++ dependency creation is postponed; clone skill.cpp.common yourself!")
  }
}
