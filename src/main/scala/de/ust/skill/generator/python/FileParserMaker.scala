/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import de.ust.skill.io.PrintWriter

trait FileParserMaker extends GeneralOutputMaker {
  final def makeParser(out : PrintWriter) {
    var tab = "    "
    //package & imports
    out.write(s"""
class Parser(FileParser):

    def __init__(self, inStream, knownTypes):
        super(Parser, self).__init__(inStream, knownTypes)

    @staticmethod
    def newPool(name: str, superPool, types: [], cls):
        \"\"\"allocate correct pool type and add it to types\"\"\"
        try:""")
        var i = 0
        for (t ← IR) {
            if (null == t.getSuperType) {
                if (t == IR.head) out.write(
                    s"""
            if name == "${t.getSkillName}":
                superPool = ${access(t)}(len(types), cls)
                return superPool""")
                else out.write(
                    s"""
            elif name == "${t.getSkillName}":
                superPool = ${access(t)}(len(types), cls)
                return superPool""")
            } else {
                if (t == IR.head) out.write(
                    s"""
            if name == "${t.getSkillName}":
                superPool = ${access(t)}(len(types), superPool, cls)
                return superPool""")
                else
                    out.write(
                        s"""
            elif name == "${t.getSkillName}":
                superPool = ${access(t)}(len(types), superPool, cls)
                return superPool
                 """)
            }
            i = i + 1
        }
        if(IR.isEmpty){
    out.write(
        s"""
            if superPool is None:
                superPool = BasePool(len(types), name, StoragePool.noKnownFields, StoragePool.noAutoFields, cls)
            else:
                superPool = superPool.makeSubPool(len(types), name, cls)
            return superPool
        finally:
            types.append(superPool)
         """.stripMargin)
        } else {
    out.write(s"""
            else:
                if superPool is None:
                    superPool = BasePool(len(types), name, StoragePool.noKnownFields, StoragePool.noAutoFields, cls)
                else:
                    superPool = superPool.makeSubPool(len(types), name, cls)
            return superPool
        finally:
            types.append(superPool)
""")}
  }
}
