/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.api.internal

import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._

trait InternalStringsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-internal_skill_names.ads""")

    out.write(s"""
with Skill.Types;

-- skill names used to represent types
-- ensures rather fast string comparisons
package ${PackagePrefix}.Internal_Skill_Names is
   pragma Preelaborate;
${
      val strings = IR.foldLeft(Set[String]()) { case (s, t) ⇒ s + t.getSkillName ++ t.getFields.map(_.getSkillName).toSet }
      (for (s ← strings.toArray.sorted)
        yield s"""
   ${s.capitalize}_Skill_Name : not null Skill.Types.String_Access :=
                           new String'("$s");
"""
      ).mkString
    }
end ${PackagePrefix}.Internal_Skill_Names;
""")

    out.close()
  }
}
