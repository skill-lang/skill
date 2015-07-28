/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada.internal

import de.ust.skill.ir._
import de.ust.skill.generator.ada.GeneralOutputMaker
import scala.collection.JavaConversions._

trait KnownFieldsMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (
      t ← IR.par;
      f ← t.getFields.par
    ) {
      makeSpec(t, f)
      makeBody(t, f)
    }
  }

  private final def makeSpec(t : UserType, f : Field) {

    val out = open(s"""${packagePrefix}-known_field_${t.getName.cStyle}_${f.getName.cStyle}.ads""")
    
    val fn = s"${t.getName.ada}_${f.getName.ada}"

    out.write(s"""
with Skill.Files;

${comment(f)}
package ${PackagePrefix}.Known_Field_$fn is

   type Known_Field_${fn}_T is tagged private;
   type Known_Field_$fn is access Known_Field_${fn}_T'Class;

private

   type Known_Field_${fn}_T is tagged null record;

end ${PackagePrefix}.Known_Field_$fn;
""")

    out.close()
  }

  private final def makeBody(t : UserType, f : Field) {

  }
}
