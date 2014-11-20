package de.ust.skill.generator.scala

import java.nio.file.Files
import java.io.File

/**
 * creates a copy of skill.jvm.common.jar in $outPath
 */
trait StreamsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    val out = new File(s"$outPath/lib/skill.jvm.common.jar");
    out.getParentFile.mkdirs();
    Files.deleteIfExists(out.toPath)
    Files.copy(new File("skill.jvm.common.jar").toPath, out.toPath)
  }
}