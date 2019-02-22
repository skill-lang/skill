/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import de.ust.skill.io.PrintWriter

trait FileParserMaker extends GeneralOutputMaker {
  final def makeParser(out : PrintWriter) {

    //package & imports
    out.write(s"""
class Parser(FileParser):

    def __init__(self, inStream):
        super(Parser, self).__init__(inStream)

    @staticmethod
    def newPools(name: str, superPool, types: []):
        \"\"\"allocate correct pool type and add it to types\"\"\"
        try:${
          (for (t ← IR)
            yield if (null == t.getSuperType) s"""
            if name == "${t.getSkillName}":
                self.superPool = ${access(t)}(len(types))
                return superPool"""
          else s"""
            if name == "${t.getSkillName}":
                superPool = ${access(t)}(len(types), superPool)
                return superPool
    """).mkString("\n")
    }
            else:
                if superPool is None:
                    superPool = BasePool(len(types), name, StoragePool.noKnownFields, StoragePool.noAutoFields)
                else:
                    superPool = superPool.makeSubPool(len(types), name)
            return superPool
        finally:
            types.append(superPool)

    def newPool(self, name, superPool, restrictions):
        return self.newPools(name, superPool, self.types)
""")
  }
}
