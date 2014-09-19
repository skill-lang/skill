/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.doxygen

import de.ust.skill.ir.UserType
import scala.collection.JavaConversions._
import de.ust.skill.ir.InterfaceType
/**
 * Creates user type equivalents.
 *
 * @author Timm Felden
 */
trait InterfaceTypeMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← IR.collect { case t : InterfaceType ⇒ t }) {
      val out = open(s"""src/${t.getName.capital}.h""")

      out.write(s"""
// interface type doxygen documentation
#include <string>
#include <list>
#include <set>
#include <map>
#include <stdint.h>

${
        comment(t)
      }class ${t.getName.capital} ${
        val ts = t.getAllSuperTypes()
        if (ts.isEmpty) ""
        else ts.map(_.getName().capital).mkString(": virtual protected ", ", virtual protected ", "")
      }{
  public:${
        (
          for (f ← t.getFields())
            yield s"""

${
            comment(f)
          }    ${mapType(f.getType())} ${f.getName.camel};"""
        ).mkString
      }
};
""")

      out.close()
    }
  }
}
