package de.ust.skill.generator.csharp

import de.ust.skill.generator.csharp.internal.AccessMaker
import de.ust.skill.generator.csharp.internal.FileParserMaker
import de.ust.skill.generator.csharp.internal.FieldDeclarationMaker
import de.ust.skill.generator.csharp.internal.StateMaker

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

    val out = files.open(s"internal.cs")

    out.write(s"""
using System;
using System.Linq;
using System.Collections;
using System.Collections.Generic;

using SkillFile = ${this.packageName}.api.SkillFile;
using de.ust.skill.common.csharp.api;
using de.ust.skill.common.csharp.@internal;
using de.ust.skill.common.csharp.@internal.fieldDeclarations;
using de.ust.skill.common.csharp.@internal.exceptions;
using de.ust.skill.common.csharp.@internal.fieldTypes;
using de.ust.skill.common.csharp.@internal.parts;
using de.ust.skill.common.csharp.restrictions;
using de.ust.skill.common.csharp.streams;

namespace ${this.packageName}
{

    ${
      suppressWarnings
      }public sealed class @internal {
        private @internal() {}

""")

    makeState(out);
    makeParser(out);
    makePools(out);
    makeFields(out);

    out.write("""
    }
}
""");

    out.close
  }
}