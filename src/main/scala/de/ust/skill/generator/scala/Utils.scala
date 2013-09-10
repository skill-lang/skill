package de.ust.skill.generator.scala

import java.io.File
import java.io.PrintWriter

/**
 * Creates a new PrintWriter at the target path ensuring the files exist.
 * @author Timm Felden
 */
object Writer {
  def apply(path: String) = {
    val f = new File(path)
    f.getParentFile.mkdirs
    f.createNewFile
    new PrintWriter(f)
  }
}