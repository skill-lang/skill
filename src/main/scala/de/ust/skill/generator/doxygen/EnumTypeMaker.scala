/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.doxygen

import scala.collection.JavaConversions.asScalaBuffer
/**
 * Creates user type equivalents.
 *
 * @author Timm Felden
 */
trait EnumTypeMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make

    for (t ← tc.getEnums) {
      val out = files.open(s"""src/${t.getName.capital}.h""")

      out.write(s"""
// user type doxygen documentation
#include <string>
#include <list>
#include <set>
#include <map>
#include <stdint.h>

${
        comment(t)
      }enum ${t.getName.capital} {
${t.getInstances.map(_.camel).mkString(", ")}
};
""")

      out.close()
    }
  }
}
