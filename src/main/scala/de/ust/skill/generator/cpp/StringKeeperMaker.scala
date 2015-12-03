/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import java.io.PrintWriter

import scala.collection.JavaConversions._

import de.ust.skill.ir._
import de.ust.skill.ir.restriction._

/**
 * a string keeper takes care of all known strings
 *
 * @author Timm Felden
 */
trait StringKeeperMaker extends GeneralOutputMaker {

  abstract override def make {
    super.make

    val out = open(s"StringKeeper.h")


    //includes package
    out.write(s"""#include <skill/api/String.h>

${packageParts.mkString("namespace ", " {\nnamespace", " {")}

    /**
     * holds instances of all strings
     */
    struct StringKeeper : public ::skill::internal::AbstractStringKeeper {${
      (for (s ← allStrings; name = escaped(s)) yield s"""
        ::skill::api::String $name;""").mkString
    }
    };
${packageParts.map(_ ⇒ "}").mkString}
""")
    out.close;
  }
}
