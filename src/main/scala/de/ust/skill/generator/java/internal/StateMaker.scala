/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.generator.java.GeneralOutputMaker
import de.ust.skill.ir.InterfaceType
import de.ust.skill.ir.Typedef
import de.ust.skill.ir.Type
import de.ust.skill.ir.UserType

trait StateMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"internal/SkillState.java")

    out.write(s"""package ${packagePrefix}internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import de.ust.skill.common.java.api.SkillException;
import de.ust.skill.common.java.internal.StoragePool;
import de.ust.skill.common.java.internal.StringPool;
import de.ust.skill.common.java.internal.exceptions.ParseException;
import de.ust.skill.common.java.internal.fieldTypes.Annotation;
import de.ust.skill.common.java.internal.fieldTypes.StringType;
import de.ust.skill.common.jvm.streams.FileInputStream;

import ${packagePrefix}api.SkillFile;

/**
 * Internal implementation of SkillFile.
 *
 * @author Timm Felden
 * @note type access fields start with a capital letter to avoid collisions
 */
${
      suppressWarnings
    }public final class SkillState extends de.ust.skill.common.java.internal.SkillState implements SkillFile {

    /**
     * Create a new skill file based on argument path and mode.
     *
     * @throws IOException
     *             on IO and mode related errors
     * @throws SkillException
     *             on file or specification consistency errors
     */
    public static SkillState open(String path, Mode... mode) throws IOException, SkillException {
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
    public static SkillState open(File path, Mode... mode) throws IOException, SkillException {
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
     * @note suppress unused warnings, because sometimes type declarations are
     *       created, although nobody is using them
     */
    public static SkillState open(Path path, Mode... mode) throws IOException, SkillException {
        return de.ust.skill.common.java.internal.SkillState.open(SkillState.class, FileParser.class, ${IR.size}, path, mode);
    }

    public SkillState(HashMap<String, StoragePool<?, ?>> poolByName, StringPool strings, StringType stringType,
            Annotation annotationType, ArrayList<StoragePool<?, ?>> types, FileInputStream in, Mode mode) {
        super(strings, in.path(), mode, types, poolByName, stringType, annotationType);

        try {
            StoragePool<?, ?> p;${
      (for (t ← IR)
        yield s"""
            ${name(t)}s = (null == (p = poolByName.get("${t.getSkillName}"))) ? FileParser.newPool("${t.getSkillName}", ${
        if (null == t.getSuperType) "null"
        else s"${name(t.getSuperType)}s"
      }, types) : (${access(t)}) p;"""
      ).mkString("")
    }${
      (for (t ← types.getInterfaces)
        yield s"""
            ${name(t)}s = new ${interfacePool(t)}("${t.getSkillName}", ${
        if (t.getSuperType.getSkillName.equals("annotation")) "annotationType"
        else name(t.getSuperType) + "s";
      }${
        collectRealizationNames(t).mkString(",", ",", "")
      });"""
      ).mkString("")
    }
        } catch (ClassCastException e) {
            throw new ParseException(in, -1, e,
                    "A super type does not match the specification; see cause for details.");
        }
        for (StoragePool<?, ?> t : types)
            poolByName.put(t.name(), t);

        finalizePools(in);
    }
${
      (for (t ← IR)
        yield s"""
    private final ${access(t)} ${name(t)}s;

    @Override
    public ${access(t)} ${name(t)}s() {
        return ${name(t)}s;
    }
"""
      ).mkString("")
    }${
      (for (t ← types.getInterfaces)
        yield s"""
    private final ${interfacePool(t)} ${name(t)}s;

    @Override
    public ${interfacePool(t)} ${name(t)}s() {
        return ${name(t)}s;
    }
"""
      ).mkString("")
    }}
""")

    out.close()
  }

  private def collectRealizationNames(target : InterfaceType) : Seq[String] = {
    def reaches(t : Type) : Boolean = t match {
      case t : UserType      ⇒ t.getSuperInterfaces.contains(target) || t.getSuperInterfaces.exists(reaches)
      case t : InterfaceType ⇒ t.getSuperInterfaces.contains(target) || t.getSuperInterfaces.exists(reaches)
      case t : Typedef       ⇒ reaches(t.getTarget)
      case _                 ⇒ false
    }

    types.getUsertypes.filter(reaches).map(name(_) + "s").toSeq
  }
}
