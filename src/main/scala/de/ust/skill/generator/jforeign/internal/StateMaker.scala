/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.jforeign.internal

import scala.collection.JavaConversions._
import de.ust.skill.generator.jforeign.GeneralOutputMaker
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
import de.ust.skill.ir.TypeContext.PointerType
import de.ust.skill.ir.UserType
import de.ust.skill.ir.TypeContext.PointerType
import de.ust.skill.ir.TypeContext
import scala.collection.mutable.HashSet
import de.ust.skill.ir.SingleBaseTypeContainer

trait StateMaker extends GeneralOutputMaker {
    def writeAddAllForMaps(baseTypes : List[Type]): String = baseTypes match {
      case head :: Nil ⇒ head match {
          case ut: UserType ⇒ s"v1.selfAdd(this);"
          case gt: GroundType ⇒ gt.getSkillName.toLowerCase() match {
            case "string" ⇒ s"Strings().add(v1);"
            case _ ⇒ "// no need to add ground types"
          }
        }

      case head :: rest ⇒ s""".forEach( (k${rest.size}, v${rest.size}) -> {
        ${head match {
          case ut: UserType ⇒ s"k${rest.size}.selfAdd(this);"
          case gt: GroundType ⇒ gt.getSkillName.toLowerCase() match {
            case "string" ⇒ s"Strings().add(k${rest.size});"
            case _ ⇒ "// no need to add ground types"
          }
        }}
        ${if (rest.size > 1) s"v${rest.size}" else ""}${writeAddAllForMaps(rest)}
    });"""
      case Nil ⇒ throw new RuntimeException("Map needs at least two types");
    }

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

import de.ust.skill.common.jforeign.api.Access;
import de.ust.skill.common.jforeign.api.FieldDeclaration;
import de.ust.skill.common.jforeign.api.SkillException;
import de.ust.skill.common.jforeign.internal.BasePool;
import de.ust.skill.common.jforeign.internal.SkillObject;
import de.ust.skill.common.jforeign.internal.StoragePool;
import de.ust.skill.common.jforeign.internal.StringPool;
import de.ust.skill.common.jforeign.internal.fieldTypes.Annotation;
import de.ust.skill.common.jforeign.internal.fieldTypes.StringType;
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
}public final class SkillState extends de.ust.skill.common.jforeign.internal.SkillState implements SkillFile {

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
            StringType stringType = new StringType(strings);
            Annotation annotation = new Annotation(types);

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
            return new SkillState(strings, types, stringType, annotation, path, actualMode.close);

        case Read:
            return FileParser.read(FileInputStream.open(path, actualMode.close == Mode.ReadOnly), actualMode.close);

        default:
            throw new IllegalStateException("should never happen");
        }
    }

    public SkillState(StringPool strings, ArrayList<StoragePool<?, ?>> types, StringType stringType,
            Annotation annotationType, Path path, Mode mode) {
        super(strings, path, mode, types, stringType, annotationType);
        poolByName = new HashMap<>();
        for (StoragePool<?, ?> p : types)
            poolByName.put(p.name(), p);
${
      var i = -1
      (for (t ← IR)
        yield s"""
        ${name(t)}s = (${name(t)}Access) poolByName.get("${t.getSkillName.toLowerCase()}");
"""
      ).mkString("")
    }

        finalizePools();
    }

    public SkillState(HashMap<String, StoragePool<?, ?>> poolByName, StringPool strings, StringType stringType,
            Annotation annotationType,
            ArrayList<StoragePool<?, ?>> types, Path path, Mode mode) {
        super(strings, path, mode, types, stringType, annotationType);
        this.poolByName = poolByName;
${
      var i = -1
      (for (t ← IR)
        yield s"""
        ${name(t)}s = (${name(t)}Access) poolByName.get("${t.getSkillName.toLowerCase()}");"""
      ).mkString("")
    }

        finalizePools();
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

${
      (for (t ← IR) yield s"""
    public void addAll(${mapType(t, false)} x) {
        if (x.getSkillID() == -9) {
            return;
        }
        x.setSkillID(-9);
        ${name(t)}s().add(x);
${
      t.getAllFields.map { f ⇒
        f.getType match {
          case ut: UserType ⇒ s"""
        if (x.${getterOrFieldAccess(t, f)} != null) {
            x.${getterOrFieldAccess(t, f)}.selfAdd(this);
        }"""
          case gt: GroundType ⇒ if (gt.getSkillName.toLowerCase().equals("string")) s"""
        Strings().add(x.${getterOrFieldAccess(t, f)});"""
          else ""
          case lt: SingleBaseTypeContainer ⇒ lt.getBaseType match {
            case gt: GroundType ⇒ if (gt.getSkillName.toLowerCase().equals("string")) s"""
        if (x.${getterOrFieldAccess(t, f)} != null) {
            x.${getterOrFieldAccess(t, f)}.forEach(e -> Strings().add(e));
        }"""
            else ""
            case ut: UserType ⇒ s"""
        if (x.${getterOrFieldAccess(t, f)} != null) {
            x.${getterOrFieldAccess(t, f)}.forEach(e -> e.selfAdd(this));
        }"""
          }
          case mt: MapType ⇒ s"""
        if (x.${getterOrFieldAccess(t, f)} != null) {
            x.${getterOrFieldAccess(t, f)}${writeAddAllForMaps(mt.getBaseTypes.toList)}
        }"""
          case x: Type ⇒ s"""
        // cannot addAll a ${x} because I don't know this type"""
        }
      }.mkString("")
}
    }

""").mkString("")
}
}
""")

    out.close()
  }
}
