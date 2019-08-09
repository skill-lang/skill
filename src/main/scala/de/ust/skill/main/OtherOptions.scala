/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.main

import de.ust.skill.BuildInfo
import de.ust.skill.generator.common.KnownGenerators

trait OtherOptions extends AbstractOptions {
  /**
   * handles requiresEscaping (including -L) and printCFM
   */
  case class OtherConfig(languages : Set[String] = Set(),
                         printCFM : Seq[String] = Seq(),
                         escapedIDs : Seq[String] = Seq()) extends WithProcess {
    def process {
      // do either of id escaping or ...
      if (!escapedIDs.isEmpty) {
        val langs = if (languages.isEmpty) allGeneratorNames else languages.toArray
        val ids = escapedIDs.toArray
        for (l ← langs) {
          println(checkEscaping(l, ids))
        }

      } else {
        val cfms = printCFM.map(_.toLowerCase).toSet

        // ... get known generator for languages
        for (
          g ← KnownGenerators.all.map(_.newInstance);
          if cfms.contains(g.getLanguageName)
        ) println(s"""Custom Field Manual (${g.getLanguageName}):
${g.customFieldManual}
""")
      }
    }
  }
  val otherParser = new scopt.OptionParser[OtherConfig]("skillc") {
    head("skillc", BuildInfo.version, "(without specification parameter)")

    opt[String]("printCFM").unbounded().action((s, c) ⇒ c.copy(printCFM = c.printCFM :+ s))

    // can only set a language once in this mode; otherwise the interface would be totally unclear
    opt[String]('L', "language").validate(
      lang ⇒
        if (allGeneratorNames.contains(lang.toLowerCase)) success
        else failure(s"Language $lang is not known and can therefore not be used!")
    ).action((lang, c) ⇒ lang match {
        case "all" ⇒ c.copy(languages = c.languages ++ allGeneratorNames)
        case lang  ⇒ c.copy(languages = c.languages + lang.toLowerCase)
      })

    opt[Seq[String]]("requiresEscaping").action((s, c) ⇒ c.copy(escapedIDs = c.escapedIDs ++ s)).text(
      """requires option "-L name"
                           checks the argument identifier list for necessity of
                           escaping. Will return a space separated list of
                           true/false. True, iff the identifier will be escaped
                           in a generated binding.
""").unbounded()

    opt[Unit]("show-generators").action {
      (_, c) ⇒ println(allGeneratorNames.mkString("\n")); c
    }

    help("help").text("prints this usage text")
    override def terminate(s : Either[String, Unit]) {
      s.fold(exit, identity)
    }

    note("""
for arguments related to skill sources/ir mode, pass a respective file as first argument""")
  }
}
