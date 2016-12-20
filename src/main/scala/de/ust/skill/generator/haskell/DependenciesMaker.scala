package de.ust.skill.generator.haskell

import java.nio.file.Files
import java.io.IOException
import java.io.File
import java.nio.file.StandardCopyOption

/**
 * copies common haskell code into the requested directory
 *
 * @author Timm Felden
 */
trait DependenciesMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (f ← deps) {
      this.getClass.synchronized({

        val out = new File(depsPath, f);
        out.getParentFile.mkdirs();

        Files.copy(new File("deps/haskell/" + f).toPath, out.toPath, StandardCopyOption.REPLACE_EXISTING)
      })
    }
  }

  private val deps = Seq("Controls.hs", "Deserialize.hs", "Methods.hs", "ReadFields.hs",
    "Serialize.hs", "ImpossibleImports.hs", "Memory.hs", "Serialize.hs", "WriteFields.hs")

}