/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

trait SkillFileMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = files.open(s"api/SkillFile.py")

    //package & imports
    out.write(
      s"""
from ${packagePrefix()}internal import SkillState


class SkillFile(src.api.SkillFile):
    \"\"\"
    An abstract skill file that is hiding all the dirty implementation details
    from you.
    \"\"\"

    @staticmethod
    def open(path, *mode):
        \"\"\"
        Create a new skill file based on argument path and mode.
        \"\"\"
        return SkillState.open(path, mode)
      """)
    out.close()
  }
}
