/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

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
      (for (s ← allStrings._1; name = escaped(s)) yield s"""
        ::skill::api::String $name;""").mkString
    }${
      (for (s ← allStrings._2; name = escaped(s)) yield s"""
        ::skill::api::String $name;""").mkString
    }
    };
${packageParts.map(_ ⇒ "}").mkString}
""")
    out.close;
  }
}
