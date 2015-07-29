/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
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

    println("ada dependency creation is postponed; clone ada_common yourself!")
    return

    // architecture to be copied to the target directory
    val architecture = s"ada.common.$buildMode.$buildOS.$buildARCH"

    // safe unnecessary overwrites that cause race conditions on parallel builds anyway
    this.getClass.synchronized(
      if (!updated) {
        val baseSource = new File(s"deps/$architecture")
        if (!baseSource.exists)
          System.err.println(s"architecture $architecture could not be found, please copy it yourself!")
        else {

          val baseDest = new File(s"$outPath/lib/$architecture");
          if (baseDest.exists) {
            // delete with children
            baseDest.listFiles().foreach { _.delete() };
            baseDest.delete();
          }
          baseDest.mkdirs();
          for (f ← baseSource.listFiles()) {
            Files.copy(f.toPath, new File(baseDest, f.getName).toPath)
          }
        }
        updated = true;
      })
  }
}
