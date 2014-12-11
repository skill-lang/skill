package de.ust.skill.generator.scala

import java.nio.file.Files
import java.io.File
import java.security.MessageDigest
import java.nio.file.Path

/**
 * creates a copy of skill.jvm.common.jar in $outPath
 */
trait StreamsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    val out = new File(s"$outPath/lib/skill.jvm.common.jar");
    out.getParentFile.mkdirs();

    // safe unnecessary overwrites that cause race conditions on parallel builds anyway
    if (out.exists() && sha256(out.getAbsolutePath) == commonJarSum)
      return

    Files.deleteIfExists(out.toPath)
    Files.copy(new File(commonJar).toPath, out.toPath)
  }

  val commonJar = "skill.jvm.common.jar"
  lazy val commonJarSum = sha256(commonJar)

  final def sha256(name : String) : String = sha256(new File("src/test/resources/"+name).toPath)
  @inline final def sha256(path : Path) : String = {
    val bytes = Files.readAllBytes(path)
    MessageDigest.getInstance("SHA-256").digest(bytes).map("%02X".format(_)).mkString
  }
}