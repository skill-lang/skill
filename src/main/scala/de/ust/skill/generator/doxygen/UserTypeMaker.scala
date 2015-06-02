/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.doxygen

import de.ust.skill.ir.UserType

import scala.collection.JavaConversions._
/**
 * Creates user type equivalents.
 *
 * @author Timm Felden
 */
trait UserTypeMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← tc.getUsertypes) {
      val out = open(s"""src/${t.getName.capital}.h""")

      out.write(s"""
// user type doxygen documentation
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
