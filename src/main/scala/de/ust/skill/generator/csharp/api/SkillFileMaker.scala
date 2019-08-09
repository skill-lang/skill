/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp.api

import de.ust.skill.generator.csharp.GeneralOutputMaker

import scala.collection.JavaConversions.asScalaBuffer

trait SkillFileMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"api/SkillFile.cs")

    //package & imports
    out.write(s"""
using System.IO;
using System.Collections.Generic;

using de.ust.skill.common.csharp.api;
using de.ust.skill.common.csharp.@internal;
using de.ust.skill.common.csharp.@internal.fieldTypes;
using SkillState = ${this.packageName}.@internal.SkillState;

namespace ${this.packageName}
{
    namespace api
    {

        /// <summary>
        /// An abstract skill file that is hiding all the dirty implementation details from you.
        ///
        /// @author Simon Glaub, Timm Felden
        /// </summary>
        ${
              suppressWarnings
            }public abstract class SkillFile : de.ust.skill.common.csharp.@internal.SkillState, de.ust.skill.common.csharp.api.SkillFile {

            public SkillFile(StringPool strings, string path, Mode mode, List<AbstractStoragePool> types,
                Dictionary<string, AbstractStoragePool> poolByName, StringType stringType, Annotation annotationType)
                : base(strings, path, mode, types, poolByName, stringType, annotationType)
            { }

            /// <summary>
            /// Create a new skill file based on argument path and mode.
            /// </summary>
            public static SkillFile open(string path, params Mode[] mode) {
                FileInfo f = new FileInfo(path);
                return open(f, mode);
            }

            /// <summary>
            /// Create a new skill file based on argument path and mode.
            /// </summary>
            public static SkillFile open(FileInfo path, params Mode[] mode) {
                foreach (Mode m in mode) {
                    if (m == Mode.Create && !path.Exists)
                        path.Create().Close();
                }
                return SkillState.open(path.FullName, mode);
            }${
              (for (t ← IR) yield s"""

            /// <returns> an access for all ${name(t)}s in this state </returns>
            public abstract @internal.${access(t)} ${name(t)}s();""").mkString("")
            }${
              (for (t ← this.types.getInterfaces) yield s"""

            /// <returns> an access for all ${name(t)}s in this state </returns>
            public abstract ${interfacePool(t)} ${name(t)}s();""").mkString("")
            }
        }
    }
}
""")

    out.close()
  }
}
