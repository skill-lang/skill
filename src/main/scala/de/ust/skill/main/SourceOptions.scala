/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.main

import java.io.File

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import de.ust.skill.BuildInfo
import de.ust.skill.generator.common.HeaderInfo
import de.ust.skill.generator.common.KnownGenerators
import de.ust.skill.ir.TypeContext
import de.ust.skill.parser.Parser

trait SourceOptions extends AbstractOptions {

  case class SourceConfig(target : String,
                          outdir : File = new File("."),
                          var depsdir : File = null,
                          clean : Boolean = false,
                          header : HeaderInfo = new HeaderInfo(),
                          var languages : Set[String] = Set(),
                          languageOptions : HashMap[String, ArrayBuffer[(String, String)]] = new HashMap(),
                          packageName : Seq[String] = Seq[String](),
                          keepSpecificationOrder : Boolean = false,
                          verbose : Boolean = false) extends WithProcess {
    def process {
      // get known generator for languages
      val known = KnownGenerators.all.map(_.newInstance).map { g ⇒ g.getLanguageName -> g }.toMap

      // depsdir defaults to outdir
      if (null == depsdir) depsdir = outdir

      // select all languages, if none was selected
      if (languages.isEmpty) {
        languages ++= allGeneratorNames
      }

      // skill TypeContext
      // this is either the result of parsing a skill file or an empty type context if "-" is specified
      val tc =
        if ("-".equals(target)) new TypeContext
        else Parser.process(new File(target), keepSpecificationOrder)

      if (verbose) {
        println(s"Parsed $target -- found ${tc.allTypeNames.size - (new TypeContext().allTypeNames.size)} types.")
        println(s"Generating sources into ${outdir.getAbsolutePath()}")
      }

      val failures = HashMap[String, Exception]()
      for (
        lang ← languages;
        gen = known(lang)
      ) {
        val pathPostfix =
          // if we process a single language only, the outdir is the target for the language. Otherwise, languages get
          // their own dirs in a subdirectory
          if (1 == languages.size) ""
          else "/generated/" + lang

        // set options
        for ((k, v) ← languageOptions.getOrElse(lang, new HashMap())) {
          gen.setOption(k.toLowerCase, v)
        }

        gen.setTC(tc)
        gen.setPackage(packageName.toList)
        gen.headerInfo = header
        gen.outPath = outdir.getAbsolutePath + pathPostfix
        gen.depsPath = depsdir.getAbsolutePath + pathPostfix

        if (verbose) print(s"run $lang: ")

        if (clean) gen.clean

        try {
          gen.make
          println("-done-")
        } catch {
          case e : IllegalStateException ⇒ println(s"-[FAILED: ${e.getMessage}]-");
          case e : Exception             ⇒ println("-FAILED-"); failures(lang) = e
        }
      }

      // report failures
      if (!failures.isEmpty) {
        if (1 == failures.size) {
          //rethrow
          throw failures.head._2
        } else {
          error((
            for ((lang, err) ← failures) yield {
              err.printStackTrace();
              s"$lang failed with message: ${err.getMessage}}"
            }
          ).mkString("\n"))
        }
      }

    }
  }
  val sourceParser = new scopt.OptionParser[SourceConfig]("skillc <file.skill>") {
    head("skillc", BuildInfo.version, "(source specification mode)")

    opt[File]('o', "outdir").optional().action(
      (p, c) ⇒ c.copy(outdir = p)
    ).text("set the output directory")

    opt[Unit]('c', "clean").optional().action(
      (p, c) ⇒ c.copy(clean = true)
    ).text("clean output directory before creating source files")

    opt[File]('d', "depsdir").optional().action(
      (p, c) ⇒ c.copy(depsdir = p)
    ).text("set the dependency directory (libs, common sources)")

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
    override def terminate(s : Either[String, Unit]) {
      s.fold(exit, identity)
    }

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
          (p, c) ⇒ c.languageOptions.getOrElseUpdate(lang, new ArrayBuffer()).append(p); c
        }.text("key-value pairs are: " + helpText)
      }
    }

    note("")

  }
}
