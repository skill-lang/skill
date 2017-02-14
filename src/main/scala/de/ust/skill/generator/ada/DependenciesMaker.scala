/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * creates copies of required ada common instance in $outPath
 * @author Timm Felden
 */
trait DependenciesMaker extends GeneralOutputMaker {
  // update files only once in a shared instance (is safe and makes tests faster)
  var updated = false;

  abstract override def make {
    super.make

    if (!skipDependencies)
      println("ada dependency creation is postponed; clone ada_common yourself!")
  }
}
