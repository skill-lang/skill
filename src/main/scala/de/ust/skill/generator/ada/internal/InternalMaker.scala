/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import de.ust.skill.generator.ada.GeneralOutputMaker

trait InternalMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    makeSpec
    makeBody
  }

  private final def makeSpec {

    val out = open(s"""${packagePrefix}-internal.ads""")

    out.write(s"""
with Skill.Files;

-- parametrization of internal file and state management
package ${PackagePrefix}.Internal is
  -- nothing atm

end ${PackagePrefix}.Internal;
""")

    out.close()
  }

  private final def makeBody {

  }
}
