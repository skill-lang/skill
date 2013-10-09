/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.io.File
import java.io.PrintWriter

/**
 * Creates a new PrintWriter at the target path ensuring the files exist.
 * @author Timm Felden
 */
object Writer {
  private[scala] def apply(path: String) = {
    val f = new File(path)
    f.getParentFile.mkdirs
    f.createNewFile
    new PrintWriter(f)
  }
}
