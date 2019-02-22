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
    def open(path, *mode):
        \"\"\"
        Create a new skill file based on argument path and mode.
        \"\"\"
        actualMode = ActualMode(mode)
        try:
            if actualMode.openMode == State.Mode.Create:
                strings = StringPool(None)
                types = []
                annotation = Annotation(types)
                return SkillState({}, strings, annotation, types,
                                    FileInputStream.open(path, False), actualMode.closeMode)
            elif actualMode.openMode == State.Mode.Read:
                p = Parser(FileInputStream.open(path, (actualMode.closeMode == State.Mode.ReadOnly)))
                return p.read(SkillState, actualMode.closeMode)
            else:
                raise Exception("should never happen")
        except SkillException as e:
            raise e
        except Exception as e:
            raise SkillException(e)

    def __init__(self, poolByName, strings, annotationType, types, inStream, mode):
        super(SkillState, self).__init__(strings, inStream.path, mode, types, poolByName, annotationType)

        try:
            ${
      (for (t ← IR)
        yield s"""
            p = poolByName.get("${t.getSkillName}")\n
            self.${name(t)}s = p if (p is not None) else Parser.newPools("${t.getSkillName}", ${
        if (null == t.getSuperType) "None"
        else s"${name(t.getSuperType)}s"
      }, types)""").mkString("")
    }
        except Exception as e:
            raise ParseException(inStream, -1, e,
                    "A super type does not match the specification; see cause for details.")
        for t in types:
            self.poolByName.put(t.name, t)

        self.finalizePools(inStream)
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
