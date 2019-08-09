/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp.internal

import de.ust.skill.generator.csharp.GeneralOutputMaker
import de.ust.skill.io.PrintWriter
import scala.collection.JavaConversions._

trait FileParserMaker extends GeneralOutputMaker {
  final def makeParser(out : PrintWriter) {

    //package & imports
    out.write(s"""
        public sealed class Parser : FileParser {

            public Parser(FileInputStream @in) : base(@in, 1) {
            }

            /// <summary>
            /// allocate correct pool type and add it to types
            /// </summary>
            internal static AbstractStoragePool newPool (string name, AbstractStoragePool superPool, List<AbstractStoragePool> types)
            {
                try {
                    switch (name) {${
              (for (t ← IR)
                yield if (null == t.getSuperType) s"""
                        case "${t.getSkillName}":
                            return (superPool = new ${access(t)}(types.Count));
        """
              else s"""
                        case "${t.getSkillName}":
                            return (superPool = new ${access(t)}(types.Count, (${access(t.getSuperType)})superPool));
""").mkString("\n")
    }
                    default:
                        if (null == superPool)
                            return (superPool = new BasePool<SkillObject>(types.Count, name, AbstractStoragePool.noKnownFields, AbstractStoragePool.NoAutoFields));
                        else
                            return (superPool = superPool.makeSubPool(types.Count, name));
                    }
                } finally {
                    types.Add(superPool);
                }
            }

            protected override AbstractStoragePool newPool(string name, AbstractStoragePool superPool, HashSet<TypeRestriction> restrictions) {
                return newPool(name, superPool, types);
            }
        }
""")
  }
}
