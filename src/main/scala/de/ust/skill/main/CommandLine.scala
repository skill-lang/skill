/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.main

import de.ust.skill.generator.common.KnownGenerators
import de.ust.skill.generator.common.Generator
import de.ust.skill.parser.Parser
import java.io.File
import de.ust.skill.generator.common.HeaderInfo
import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._
import de.ust.skill.ir.TypeContext
import scala.collection.mutable.ListBuffer

/**
 * Command line interface to the skill compilers
 * @author Timm Felden
 * @todo add option to suppress implicit naming conventions
 * @todo add an option to tell the generators to make the equivalent of a library (jar, whatever) and use this option
 * for generic testing
 */
object CommandLine {
  /**
   * configurable exit method called on error
   * @note the purpose of changing this is to replace it with a failure method in unit tests
   */
  var exit : String ⇒ Unit = { s ⇒ System.err.println(s); System.exit(0) }

  // this exception is used to quit normally
  class DoneException extends Exception;

  private def printHelp(gens : Iterable[Generator]) : Unit = {
    println("""
usage:
  [options] skillPath outPath

Opitions:
  -p packageName         set a package name used by all emitted code.

  -h1|h2|h3 content      overrides the content of the respective header line
  -u userName            set a user name
  -date date             set a custom date
  -license text          set a license text

  -L name|all            request a language to be built; default: all
  -O@<lang>:<opt>=<val>  set for a language an option to a value

  --requiresEscaping [identifier]+
                         requires option "-L name"
                         checks the argument identifier list for necessity of
                         escaping. Will return a space separated list of
                         true/false. True, iff the identifier will be escaped
                         in a generated binding.

  --printCFM language    print custom field manual for the argument language
""")
    gens.foreach(_.printHelp)
  }

  /**
   * print an error message and quit
   */
  def error(msg : String) : Nothing = {
    exit(msg)
    ???
  }

  def main(args : Array[String]) {

    // get known generator for languages
    val known = KnownGenerators.all.map(_.newInstance).map { g ⇒ g.getLanguageName -> g }.toMap

    // process options
    if (2 > args.length) {
      printHelp(known.values)
      return
    }
    val skillPath : String = args(args.length - 2)
    var outPath : String = args(args.length - 1)

    try {
      if (args.contains("--requiresEscaping") || args.contains("--printCFM"))
        parseOptions(args.to, known)

      val (header, packageName, languages, foreignSources, optionalOpts) = parseOptions(args.view(0, args.length - 2).to, known)

      assert(!packageName.isEmpty, "A package name must be specified. Generators rely on it!")

      // skill TypeContext
      // this is either the result of parsing a skill file or an empty type context if "-" is specified
      val tc = if (skillPath != "-") Parser.process(new File(skillPath)) else new TypeContext

      // process mapping
      val (jforeignTc, reflectionMap) =
      if (languages contains "javaforeign") {
        if (!optionalOpts.contains("mappingFile")) error("-M option is missing for javaForeign")
          JavaForeign.run(optionalOpts("mappingFile"), tc, foreignSources)
      } else (null, null);

      // invoke generators
      println(s"Parsed $skillPath -- found ${tc.allTypeNames.size - (new TypeContext().allTypeNames.size)} types.")
      println(s"Generating sources into ${new File(outPath).getAbsolutePath()}")

      val failures = HashMap[String, Exception]()
      for ((n, m) ← languages) {
        if (m.isInstanceOf[de.ust.skill.generator.javaforeign.GeneralOutputMaker])
          m.asInstanceOf[de.ust.skill.generator.javaforeign.GeneralOutputMaker].setReflectionMap(reflectionMap)
        m.setTC(if (n == "javaforeign") jforeignTc else tc)
        m.setPackage(packageName)
        m.headerInfo = header
        m.outPath = outPath + "/" + n

        print(s"run $n: ")
        try {
          m.make
          println("-done-")
        } catch { case e : Exception ⇒ println("-FAILED-"); failures(n) = e }
      }

      // report failures
      if (!failures.isEmpty)
        error((
          for ((lang, err) ← failures) yield {
            err.printStackTrace();
            s"$lang failed with message: ${err.getMessage}}"
          }
        ).mkString("\n"))

    } catch {
      case e : DoneException ⇒ return ;
    }
  }

  def parseOptions(args : Iterator[String], known : Map[String, Generator]) = {

    var packageName = List[String]()
    val header = new HeaderInfo()
    val selectedLanguages = new HashMap[String, Generator]()
    val foreignSources = new ListBuffer[String]()
    val optionalOpts = new HashMap[String, String]

    while (args.hasNext) args.next match {

      case "-p"       ⇒ packageName = args.next.split('.').toList

      case "-h1"      ⇒ header.line1 = Some(args.next)
      case "-h2"      ⇒ header.line2 = Some(args.next)
      case "-h3"      ⇒ header.line3 = Some(args.next)

      case "-u"       ⇒ header.userName = Some(args.next)

      case "-date"    ⇒ header.date = Some(args.next)

      case "-license" ⇒ header.license = Some(args.next)

      case "-L" ⇒ args.next match {
        case "all" ⇒ selectedLanguages ++= known
        case lang ⇒ selectedLanguages(lang.toLowerCase) = known.get(lang.toLowerCase).getOrElse(
          error(s"Language $lang is not known and can therefore not be used!")
        )
      }

      // @note language and options are case insensitive for all generators
      case option if option.matches("""-O@?\w+:\w+=.+""") ⇒ locally {
        implicit class Regex(sc : StringContext) {
          def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ ⇒ "x") : _*)
        }
        val r"""-O@?(\w+)${ lang }:(\w+)${ opt }=(.+)${ value }""" = option
        known.get(lang.toLowerCase).getOrElse(
          error(s"Language $lang is not known and can therefore not provided with options!")
        ).setOption(opt.toLowerCase, value)

      }

      case "--requiresEscaping" ⇒
        if (selectedLanguages.size != 1)
          error("Exactly one language has to be specified using the -L option before asking for escapings.")
        else {
          val words : Array[String] = args.toArray
          println(checkEscaping(selectedLanguages.keySet.head, words))
          throw new DoneException;
        }
      case "-M" ⇒ optionalOpts += ("mappingFile" → args.next)

      case "-F" => foreignSources += args.next()

      case "--printCFM" ⇒
        if (!args.hasNext)
          error("Exactly one language has to be specified when using --printCFM.")

        val language = args.next
        if (!known.contains(language.toLowerCase))
          error(s"Unknown language: $language.")
        else {
          println(s"Custom Field Manual for language $language:")
          println(known(language.toLowerCase).customFieldManual)
          throw new DoneException;
        }

      case unknown ⇒ error(s"unknown option: $unknown")
    }

    if (selectedLanguages.isEmpty)
      selectedLanguages ++= known

    (header, packageName, selectedLanguages, foreignSources.toList, optionalOpts)
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
  def getKnownGeneratorNames : Array[String] = {
    KnownGenerators.all.map(_.newInstance.getLanguageName).to
  }

}
