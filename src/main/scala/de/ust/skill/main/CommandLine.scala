package de.ust.skill.main

import de.ust.skill.generator.common.KnownGenerators
import de.ust.skill.generator.common.Generator
import de.ust.skill.parser.Parser
import java.io.File
import de.ust.skill.generator.common.HeaderInfo
import scala.annotation.tailrec
import scala.collection.mutable.HashMap
import scala.collection.JavaConversions._

object CommandLine {
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
""")
    gens.foreach(_.printHelp)
  }

  /**
   * print an error message and quit
   */
  def error(msg : String) : Nothing = {
    System.err.println(msg);
    System.exit(0);
    ???
  }

  def main(args : Array[String]) {

    // get known generator for languages
    val known = KnownGenerators.all.map(_.newInstance).map { g ⇒ g.getLanguageName -> g }.toMap

    // process options
    if (2 > args.length) {
      printHelp(known.values)
    }
    val skillPath : String = args(args.length - 2)
    var outPath : String = args(args.length - 1)

    val (header, packageName, languages) = parseOptions(args.view(0, args.length - 2).to, known)

    //      m.setOptions(args.slice(0, args.length - 2)).ensuring(m._packagePrefix != "", "You have to specify a non-empty package name!")
    //      val skillPath = args(args.length - 2)
    //      m.outPath = args(args.length - 1)
    //
    //      //parse argument code
    //      m.setIR(Parser.process(new File(skillPath)).to)
    //
    //      // create output using maker chain
    //      m.make;

    assert(!packageName.isEmpty, "A package name must be specified. Generators rely on it!")

    // invoke generators
    val tc = Parser.process(new File(skillPath))

    for ((n, m) ← languages) {
      m.setTC(tc)
      m.setPackage(packageName)
      m.headerInfo = header
      m.outPath = outPath

      print(s"run $n: ")
      m.make
      println("-done-")
    }
  }

  def parseOptions(args : Iterator[String], known : Map[String, Generator]) = {

    var packageName = List[String]()
    val header = new HeaderInfo()
    val actual = new HashMap[String, Generator]()

    while (args.hasNext) args.next match {

      case "-p"       ⇒ packageName = args.next.split('.').toList

      case "-h1"      ⇒ header.line1 = Some(args.next)
      case "-h2"      ⇒ header.line2 = Some(args.next)
      case "-h3"      ⇒ header.line3 = Some(args.next)

      case "-u"       ⇒ header.userName = Some(args.next)

      case "-date"    ⇒ header.date = Some(args.next)

      case "-license" ⇒ header.license = Some(args.next)

      case "-L" ⇒ args.next match {
        case "all" ⇒ actual ++= known
        case lang  ⇒ actual(lang) = known.get(lang).getOrElse(error(s"Language $lang is not known and can therefore not be used!"))
      }

      case option if option.matches("""-O@\w+:\w+=\w+""") ⇒ ???

      case unknown                                        ⇒ error(s"unknown option: $unknown")
    }

    if (actual.isEmpty)
      actual ++= known

    (header, packageName, actual)
  }

}