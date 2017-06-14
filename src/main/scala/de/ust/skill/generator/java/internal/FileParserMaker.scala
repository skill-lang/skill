/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java.internal

import de.ust.skill.generator.java.GeneralOutputMaker
import de.ust.skill.io.PrintWriter

trait FileParserMaker extends GeneralOutputMaker {
  final def makeParser(out : PrintWriter) {

    //package & imports
    out.write(s"""
${
      suppressWarnings
    }public static final class Parser extends de.ust.skill.common.java.internal.FileParser {

    public Parser(FileInputStream in) {
        super(in, ${IR.size});
    }

    /**
     * allocate correct pool type and add it to types
     */
    static <T extends B, B extends SkillObject, P extends StoragePool<T, B>> P newPool(String name,
            StoragePool<?, ?> superPool, ArrayList<StoragePool<?, ?>> types) {
        try {
            switch (name) {${
      (for (t ← IR)
        yield if (null == t.getSuperType) s"""
        case "${t.getSkillName}":
            return (P) (superPool = new ${access(t)}(types.size()));
"""
      else s"""
        case "${t.getSkillName}": 
            return (P) (superPool = new ${access(t)}(types.size(), (${access(t.getSuperType)})superPool));
""").mkString("\n")
    }
            default:
                if (null == superPool)
                    return (P) (superPool = new BasePool<T>(types.size(), name, StoragePool.noKnownFields, noAutoFields()));
                else
                    return (P) (superPool = superPool.makeSubPool(types.size(), name));
            }
        } finally {
            types.add(superPool);
        }
    }

    @Override
    protected <T extends B, B extends SkillObject> StoragePool<T, B> newPool(String name,
            StoragePool<? super T, B> superPool, HashSet<TypeRestriction> restrictions) {
        return newPool(name, superPool, types);
    }
}
""")
  }
}
