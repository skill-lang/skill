package de.ust.skill.generator.java

import de.ust.skill.generator.java.internal.AccessMaker
import de.ust.skill.generator.java.internal.FileParserMaker
import de.ust.skill.generator.java.internal.FieldDeclarationMaker
import de.ust.skill.generator.java.internal.StateMaker

/**
 * Create an internal class instead of a package. That way, the fucked-up Java
 * visibility model can be exploited to access fields directly.
 * As a side-effect, types that resided in that package must be inner classes of
 * internal.
 */
trait InternalMaker extends GeneralOutputMaker
    with AccessMaker
    with FieldDeclarationMaker
    with FileParserMaker
    with StateMaker {

  abstract override def make {
    super.make

    val out = files.open(s"internal.java")

    out.write(s"""package ${this.packageName};

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import ${packagePrefix}api.SkillFile;
import de.ust.skill.common.java.api.SkillException;
import de.ust.skill.common.java.internal.BasePool;
import de.ust.skill.common.java.internal.FieldDeclaration;
import de.ust.skill.common.java.internal.FieldType;
import de.ust.skill.common.java.internal.InterfacePool;
import de.ust.skill.common.java.internal.KnownDataField;
import de.ust.skill.common.java.internal.SkillObject;
import de.ust.skill.common.java.internal.StoragePool;
import de.ust.skill.common.java.internal.StringPool;
import de.ust.skill.common.java.internal.UnrootedInterfacePool;
import de.ust.skill.common.java.internal.fieldDeclarations.AutoField;
import de.ust.skill.common.java.internal.exceptions.ParseException;
import de.ust.skill.common.java.internal.fieldTypes.*;
import de.ust.skill.common.java.internal.parts.Block;
import de.ust.skill.common.java.restrictions.TypeRestriction;
import de.ust.skill.common.jvm.streams.FileInputStream;
import de.ust.skill.common.jvm.streams.MappedInStream;
import de.ust.skill.common.jvm.streams.MappedOutStream;

${
      suppressWarnings
    }public final class internal {
    private internal() {}

""")

    makeState(out);
    makeParser(out);
    makePools(out);
    makeFields(out);

    out.write("""
}
""");

    out.close
  }
}