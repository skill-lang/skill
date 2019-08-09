/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.nio.file.NoSuchFileException

/**
 * creates copies of required jars in $outPath
 * @author Simon Glaub, Timm Felden
 */
trait DependenciesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    // safe unnecessary overwrites that cause race conditions on parallel builds anyway
    if (!skipDependencies)
      for (dll ← dlls) {
        this.getClass.synchronized({

          val out = new File(depsPath, dll);
          out.getParentFile.mkdirs();

          if (try {
            !out.exists() || !commonDllSum(dll).equals(sha256(out.toPath()))
          } catch {
            case e : IOException ⇒
              false // just continue
          }) {
            Files.deleteIfExists(out.toPath)
            try {
              Files.copy(new File("deps/" + dll).toPath, out.toPath)
            } catch {
              case e : NoSuchFileException ⇒
                throw new IllegalStateException("deps directory apparently inexistent.\nWas looking for " + new File("deps/" + dll).getAbsolutePath, e)
            }
          }
        })
      }
  }

  val dlls = Seq("skill.csharp.common.dll")
  lazy val commonDllSum = dlls.map { s ⇒ (s -> sha256("deps/" + s)) }.toMap

  final def sha256(name : String) : String = sha256(new File(name).toPath)
  @inline final def sha256(path : Path) : String = {
    val bytes = Files.readAllBytes(path)
    MessageDigest.getInstance("SHA-256").digest(bytes).map("%02X".format(_)).mkString
  }
}
