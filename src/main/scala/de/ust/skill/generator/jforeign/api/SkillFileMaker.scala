/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.jforeign.api

import de.ust.skill.generator.jforeign.GeneralOutputMaker

trait SkillFileMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/SkillFile.java")

    //package & imports
    out.write(s"""package ${packagePrefix}api;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import ${packagePrefix}internal.SkillState;
import de.ust.skill.common.jforeign.api.SkillException;

/**
 * An abstract skill file that is hiding all the dirty implementation details
 * from you.
 *
 * @author Timm Felden
 */
${
      suppressWarnings
    }public interface SkillFile extends de.ust.skill.common.jforeign.api.SkillFile {

    /**
     * Create a new skill file based on argument path and mode.
     *
     * @throws IOException
     *             on IO and mode related errors
     * @throws SkillException
     *             on file or specification consistency errors
     */
    public static SkillFile open(String path, Mode... mode) throws IOException, SkillException {
        return SkillState.open(path, mode);
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
        return SkillState.open(path, mode);
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
    public ${packagePrefix}internal.${name(t)}Access ${name(t)}s();""").mkString("")
    }
}
""")

    out.close()
  }
}
