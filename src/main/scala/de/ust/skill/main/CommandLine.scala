/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.main

/**
 * command line parsing is split into three basic modes, depending on the extension of the first argument:
 * - for a .sir-file, the command line parser operates in IRConfig mode
 * - for a .skill-file, the command line parser operates in source mode
 * - else, the parser operates in other mode, i.e. the operations are independent of a skill specification.
 *
 *
 * @author Timm Felden
 * @todo add option to suppress implicit naming conventions
 * @todo add an option to tell the generators to make the equivalent of a library
 * (.jar, .so, ...) and use this option for generic testing
 */
object CommandLine extends SourceOptions with IROptions with OtherOptions {

  def main(args : Array[String]) {
    if (args.isEmpty) {
      error("you must provide an argument")
    }

    val first = args.head

    (if (first.endsWith(".skill") || "-".equals(first)) {
      sourceParser.parse(args.tail, SourceConfig(target = first))
    } else if (first.endsWith(".sir")) {
      irParser.parse(args.tail, IRConfig(target = first))
    } else {
      otherParser.parse(args, OtherConfig())
    }) match {
      case Some(c) ⇒ c.process
      case None    ⇒ error("")
    }
  }
}
