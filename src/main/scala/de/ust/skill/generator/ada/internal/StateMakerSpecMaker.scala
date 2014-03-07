/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import java.io.PrintWriter
import de.ust.skill.generator.ada.GeneralOutputMaker

trait StateMakerSpecMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal-state_maker.ads""")

    out.write(s"""
package ${packagePrefix.capitalize}.Internal.State_Maker is

   procedure Create (State : access Skill_State);

end ${packagePrefix.capitalize}.Internal.State_Maker;
""")

    out.close()
  }
}
