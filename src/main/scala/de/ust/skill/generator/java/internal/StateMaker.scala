/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import scala.collection.JavaConversions._
import de.ust.skill.generator.java.GeneralOutputMaker
import de.ust.skill.ir.Type
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.restriction.MonotoneRestriction
import de.ust.skill.ir.restriction.SingletonRestriction

trait StateMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/SkillState.java")

    out.write(s"""package ${packagePrefix}internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import de.ust.skill.common.java.api.Access;
import de.ust.skill.common.java.api.FieldDeclaration;
import de.ust.skill.common.java.api.SkillException;
import de.ust.skill.common.java.internal.BasePool;
import de.ust.skill.common.java.internal.SkillObject;
import de.ust.skill.common.java.internal.StoragePool;
import de.ust.skill.common.java.internal.StringPool;
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
public final class SkillState extends de.ust.skill.common.java.internal.SkillState implements SkillFile {

    // types in type order
    private ArrayList<StoragePool<?, ?>> types;

    // types by skill name
    private final HashMap<String, StoragePool<?, ?>> poolByName;

    @Override
    public HashMap<String, StoragePool<?, ?>> poolByName() {
        return poolByName;
    }

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
        assert f.exists() : "can only open files that already exist in genarel, because of java.nio restrictions";
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
    @SuppressWarnings("unused")
    public static SkillState open(Path path, Mode... mode) throws IOException, SkillException {
        ActualMode actualMode = new ActualMode(mode);
        switch (actualMode.open) {
        case Create:
            // initialization order of type information has to match file parser
            // and can not be done in place
            StringPool strings = new StringPool(null);
            ArrayList<StoragePool<?, ?>> types = new ArrayList<>(1);
            Annotation annotation = new Annotation(types);
            StringType stringType = new StringType(strings);

            // create type information${
      var i = -1
      (for (t ← IR)
        yield s"""
            ${name(t)}Access ${name(t)} = new ${name(t)}Access(${i += 1; i}${
        if (null == t.getSuperType) ""
        else { ", "+name(t.getSuperType) }
      });
            types.add(${name(t)});"""
      ).mkString("")
    }
            return new SkillState(strings, types, path, actualMode.close);

        case Read:
            return FileParser.read(FileInputStream.open(path), actualMode.close);

        default:
            throw new IllegalStateException("should never happen");
        }
    }

    public SkillState(StringPool strings, ArrayList<StoragePool<?, ?>> types, Path path, Mode mode) {
        super(path, mode);
        this.types = types;
        poolByName = new HashMap<>();
        for (StoragePool<?, ?> p : types)
            poolByName.put(p.name(), p);
${
      var i = -1
      (for (t ← IR)
        yield s"""
        ${name(t)}s = (${name(t)}Access) poolByName.get("${t.getSkillName}");
"""
      ).mkString("")
    }

        finalizePools();
    }

    public SkillState(HashMap<String, StoragePool<?, ?>> poolByName, StringPool strings,
            ArrayList<StoragePool<?, ?>> types, Path path, Mode mode) {
        super(path, mode);
        this.types = types;
        this.poolByName = poolByName;
${
      var i = -1
      (for (t ← IR)
        yield s"""
        ${name(t)}s = (${name(t)}Access) poolByName.get("${t.getSkillName}");"""
      ).mkString("")
    }

        finalizePools();
    }

    private void finalizePools() {
        StringType ts = new StringType((StringPool) Strings());
        Annotation as = new Annotation(types);
        for (StoragePool<?, ?> p : types) {
            // @note this loop must happen in type order!

            // set owners
            if (p instanceof BasePool<?>)
                ((BasePool<?>) p).setOwner(this);

            // add missing field declarations
            HashSet<String> fieldNames = new HashSet<>();
            for (FieldDeclaration<?, ?> f : p.fields())
                fieldNames.add(f.name());

            for (String n : p.knownFields) {
                if (!fieldNames.contains(n))
                    p.addKnownField(n, ts, as);
            }
        }
    }
${
      var i = -1
      (for (t ← IR)
        yield s"""
    private final ${name(t)}Access ${name(t)}s;

    @Override
    public ${name(t)}Access ${name(t)}s() {
        return ${name(t)}s;
    }
"""
      ).mkString("")
    }
    @Override
    public Iterable<? extends Access<? extends SkillObject>> allTypes() {
        return types;
    }
}
""")

    out.close()
  }
}
