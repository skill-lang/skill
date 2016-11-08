/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.main

import java.io.File

import de.ust.skill.BuildInfo
import de.ust.skill.generator.common.HeaderInfo
import de.ust.skill.generator.common.KnownGenerators
import scala.collection.mutable.HashMap
import de.ust.skill.ir.TypeContext
import de.ust.skill.parser.Parser
import de.ust.skill.generator.jforeign.Main

/**
 * new command line argument parsing
 *
 * command line parsing is split into three basic modes, depending on the extension of the first argument:
 *
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
object NewCommandLine {

  /**
   * configurable exit method called on error
   * @note the purpose of changing this is to replace it with a failure method in unit tests
   */
  var exit : String ⇒ Unit = { s ⇒ System.err.println(s); System.exit(0) }

  /**
   * print an error message and quit
   */
  def error(msg : String) : Nothing = {
    exit(msg)
    ???
  }

  trait WithProcess {
    def process;
  }

  case class IRConfig(target : String, header : HeaderInfo = new HeaderInfo()) extends WithProcess {
    def process {
    }
  }
  val irParser = new scopt.OptionParser[IRConfig]("skillc <file.sir>") {
    head("skillc", BuildInfo.version, "(skill intermediate representation mode)")

    help("help").text("prints this usage text")
  }

  case class SourceConfig(target : String,
                          outdir : File = new File("."),
                          header : HeaderInfo = new HeaderInfo(),
                          languages : Set[String] = Set(),
                          languageOptions : HashMap[String, HashMap[String, String]] = new HashMap(),
                          packageName : Seq[String] = Seq[String](),
                          keepSpecificationOrder : Boolean = false,
                          verbose : Boolean = false) extends WithProcess {
    def process {
      // get known generator for languages
      val known = KnownGenerators.all.map(_.newInstance).map { g ⇒ g.getLanguageName -> g }.toMap

      // skill TypeContext
      // this is either the result of parsing a skill file or an empty type context if "-" is specified
      val tc =
        if ("-".equals(target)) new TypeContext
        else Parser.process(new File(target), keepSpecificationOrder)

      if (verbose) {
        println(s"Parsed $target -- found ${tc.allTypeNames.size - (new TypeContext().allTypeNames.size)} types.")
        println(s"Generating sources into ${outdir.getAbsolutePath()}")
      }

      // if we process a single language only, the outdir is the target for the language. Otherwise, languages get their
      // own dirs in a subdirectory 
      if (languages.size == 1) {
        val lang = languages.head
        val gen = known(lang)
        gen.setTC(tc)
        gen.setPackage(packageName.toList)
        gen.headerInfo = header
        gen.outPath = outdir.getAbsolutePath

        if (verbose) print(s"run $lang: ")

        gen.make
        println("-done-")

      } else {
        val failures = HashMap[String, Exception]()
        for (
          lang ← if (languages.isEmpty) allGeneratorNames else languages.toArray;
          gen = known(lang)
        ) {
          gen.setTC(tc)
          gen.setPackage(packageName.toList)
          gen.headerInfo = header
          gen.outPath = outdir.getAbsolutePath + "/generated/" + lang

          if (verbose) print(s"run $lang: ")

          try {
            gen.make
            println("-done-")
          } catch {
            case e : IllegalStateException ⇒ println(s"-[FAILED: ${e.getMessage}]-");
            case e : Exception             ⇒ println("-FAILED-"); failures(lang) = e
          }
        }

        // report failures
        if (!failures.isEmpty)
          error((
            for ((lang, err) ← failures) yield {
              err.printStackTrace();
              s"$lang failed with message: ${err.getMessage}}"
            }
          ).mkString("\n"))
      }
    }
  }
  val sourceParser = new scopt.OptionParser[SourceConfig]("skillc <file.skill>") {
    head("skillc", BuildInfo.version, "(source specification mode)")

    opt[File]('o', "outdir").optional().action(
      (p, c) ⇒ c.copy(outdir = p)
    ).text("set the output directory")

    opt[String]('p', "package").required().action(
      (s, c) ⇒ c.copy(packageName = s.split('.'))
    ).text("set a package name used by all emitted code")

    opt[Boolean]("keepSpecOrder").optional().action(
      (v, c) ⇒ c.copy(keepSpecificationOrder = v)
    ).text("keep order from the specification where possible")

    opt[Boolean]("verbose").optional().action(
      (v, c) ⇒ c.copy(verbose = v)
    ).text("print some diagnostic information")

    opt[String]('L', "language").optional().unbounded().validate(
      lang ⇒
        if (allGeneratorNames.contains(lang.toLowerCase)) success
        else failure(s"Language $lang is not known and can therefore not be used!")
    ).action((lang, c) ⇒ lang match {
        case "all" ⇒ c.copy(languages = c.languages ++ allGeneratorNames)
        case lang  ⇒ c.copy(languages = c.languages + lang.toLowerCase)
      })

    help("help").text("prints this usage text")

    note("")

    opt[String]("header1").abbr("h1").optional().action {
      (s, c) ⇒ c.header.line1 = Some(s); c
    }.text("overrides the content of the respective header line")
    opt[String]("header2").abbr("h2").optional().action {
      (s, c) ⇒ c.header.line2 = Some(s); c
    }.text("overrides the content of the respective header line")
    opt[String]("header3").abbr("h3").optional().action {
      (s, c) ⇒ c.header.line3 = Some(s); c
    }.text("overrides the content of the respective header line")

    opt[String]('u', "user-name").optional().action {
      (s, c) ⇒ c.header.userName = Some(s); c
    }.text("set a user name")
    opt[String]("date").optional().action {
      (s, c) ⇒ c.header.date = Some(s); c
    }.text("set a custom date")
    opt[String]("license").optional().action {
      (s, c) ⇒ c.header.license = Some(s); c
    }.text("set a license text")

    opt[Unit]("debug-header").action {
      (s, c) ⇒
        c.header.userName = Some("<<some developer>>")
        c.header.line2 = Some("<<debug>>")
        c
    }.text("set debugging and diff friendly header content")

    note("")

    for (lang ← allGeneratorNames) {

      val helpText = knownHelpTexts(lang)
      if (!helpText.isEmpty()) {

        opt[(String, String)](s"set-$lang-option").abbr(s"O$lang").optional().unbounded().action {
          (p, c) ⇒ c.languageOptions.getOrElseUpdate(lang, new HashMap()).put(p._1, p._2); c
        }.text("key-value pairs are: " + helpText)
      }
    }

    note("")

  }

  /**
   * handles requiresEscaping (including -L) and printCFM
   */
  case class OtherConfig(languages : Set[String] = Set(),
                         printCFM : Seq[String] = Seq(),
                         escapedIDs : Seq[String] = Seq()) extends WithProcess {
    def process {
      // do either of id escaping and 
      if (!escapedIDs.isEmpty) {
        val langs = if (languages.isEmpty) allGeneratorNames else languages.toArray
        val ids = escapedIDs.toArray
        for (l ← langs) {
          println(checkEscaping(l, ids))
        }

      } else {
        val cfms = printCFM.map(_.toLowerCase).toSet

        // get known generator for languages
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

    opt[Seq[String]]("requiresEscaping").action((s, c) ⇒ c.copy(escapedIDs = c.escapedIDs ++ s)).text("""requires option "-L name"
                           checks the argument identifier list for necessity of
                           escaping. Will return a space separated list of
                           true/false. True, iff the identifier will be escaped
                           in a generated binding.
""").unbounded()

    opt[Unit]("show-generators").action {
      (_, c) ⇒ println(allGeneratorNames.mkString("\n")); c
    }

    help("help").text("prints this usage text")

    note("""
for arguments related to skill sources/ir mode, pass a respective file as first argument""")
  }

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

  def checkEscaping(language : String, args : Array[String]) : String = {
    val generator = KnownGenerators.all.map(_.newInstance).collect({
      case g if g.getLanguageName == language.toLowerCase ⇒ g
    }).head
    args.map { s ⇒ s != generator.escapedLonely(s) }.mkString(" ")
  }

  /**
   * returns an array containing all language names reported by known generators
   */
  lazy val allGeneratorNames : Array[String] = {
    KnownGenerators.all.map(_.newInstance.getLanguageName).to
  }

  /**
   * all known help texts
   */
  lazy val knownHelpTexts = KnownGenerators.all.map(_.newInstance).map { g ⇒ g.getLanguageName -> g.helpText }.toMap
}