/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import de.ust.skill.io.PrintWriter
import de.ust.skill.ir.{InterfaceType, Type, Typedef, UserType}

import scala.collection.JavaConversions.asScalaBuffer

trait StateMaker extends GeneralOutputMaker {
  final def makeState(out : PrintWriter) {

    out.write(s"""
class SkillState(State):
    \"\"\"
    Internal implementation of SkillFile.
    note: type access fields start with a capital letter to avoid collisions
    \"\"\"

    @staticmethod
    def open(path, mode: [], knownTypes: [], knownSubTypes: []):
        \"\"\"
        Create a new skill file based on argument path and mode.
        \"\"\"
        actualMode = ActualMode(mode)
        try:
            if actualMode.openMode == Mode.Create:
                strings = StringPool(None)
                types = []
                annotation = Annotation(types)
                return SkillState({}, strings, annotation, types,
                                    FileInputStream.open(path), actualMode.closeMode, knownTypes, knownSubTypes)
            elif actualMode.openMode == Mode.Read:
                p = Parser(FileInputStream.open(path), knownTypes, knownSubTypes)
                return p.read(SkillState, actualMode.closeMode, knownTypes, knownSubTypes)
            else:
                raise Exception("should never happen")
        except SkillException as e:
            raise e
        except Exception as e:
            raise SkillException(e)

    def __init__(self, poolByName, strings, annotationType, types, inStream, mode, knownTypes, knownSubTypes):
        super(SkillState, self).__init__(strings, inStream.path, mode, types, poolByName, annotationType)
        self._knownTypes = knownTypes
        self._knownSubTypes = knownSubTypes

        try:""")
      var i = 0
      for (t ← IR) {
          i = i + 1
          out.write(s"""
            p = poolByName.get("${t.getSkillName}")
            self.${name(t)} = p if (p is not None) else Parser.newPool("${t.getSkillName}", """)
          if (null == t.getSuperType) out.write("None")
          else out.write(s"self.${name(t.getSuperType)}")
          out.write(s""", types, self._knownTypes, self._knownSubTypes)""")
      }
      if (i == 0) {out.write(s"""p = None""")}
      out.write(s"""
        except Exception as e:
            raise ParseException(inStream, -1, e,
                                 "A super type does not match the specification; see cause for details.")
        for t in types:
            self._poolByName[t.name] = t

        self._finalizePools(inStream)
""")

  }

  private def collectRealizationNames(target : InterfaceType) : Seq[String] = {
    def reaches(t : Type) : Boolean = t match {
      case t : UserType      ⇒ t.getSuperInterfaces.contains(target) || t.getSuperInterfaces.exists(reaches)
      case t : Typedef       ⇒ reaches(t.getTarget)
      case _                 ⇒ false
    }

    types.getUsertypes.filter(reaches).map(name(_) + "s").toSeq
  }
}
