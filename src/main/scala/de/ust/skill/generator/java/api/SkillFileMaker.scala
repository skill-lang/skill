/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.api

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.java.GeneralOutputMaker

trait SkillFileMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"api/SkillFile.java")

    //package & imports
    out.write(s"""package ${packagePrefix}api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import ${packagePrefix}internal.SkillState;
import de.ust.skill.common.java.api.SkillException;

/**
 * An abstract skill file that is hiding all the dirty implementation details
 * from you.
 *
 * @author Timm Felden
 */
${
      suppressWarnings
    }public interface SkillFile extends de.ust.skill.common.java.api.SkillFile {

    /**
     * Create a new skill file based on argument path and mode.
     *
     * @throws IOException
     *             on IO and mode related errors
     * @throws SkillException
     *             on file or specification consistency errors
     */
    public static SkillFile open(String path, Mode... mode) throws IOException, SkillException {
        File f = new File(path);
        return open(f, mode);
    }

    /**
     * Create a new skill file based on argument path and mode.
     *
     * @throws IOException
     *             on IO and mode related errors
     * @throws SkillException
     *             on file or specification consistency errors
     */
    public static SkillFile open(File path, Mode... mode) throws IOException, SkillException {
        for (Mode m : mode) {
            if (m == Mode.Create && !path.exists())
                path.createNewFile();
        }
        assert path.exists() : "can only open files that already exist in genarel, because of java.nio restrictions";
        return open(path.toPath(), mode);
    }

    /**
     * Create a new skill file based on argument path and mode.
     *
     * @throws IOException
     *             on IO and mode related errors
     * @throws SkillException
     *             on file or specification consistency errors
     */
    public static SkillFile open(Path path, Mode... mode) throws IOException, SkillException {
        return SkillState.open(path, mode);
    }${
      (for (t ← IR) yield s"""

    /**
     * @return an access for all ${name(t)}s in this state
     */
    public ${packagePrefix}internal.${access(t)} ${name(t)}s();""").mkString("")
    }${
      (for (t ← this.types.getInterfaces) yield s"""

    /**
     * @return an access for all ${name(t)}s in this state
     */
    public ${interfacePool(t)} ${name(t)}s();""").mkString("")
    }
}
""")

    out.close()
  }
}
