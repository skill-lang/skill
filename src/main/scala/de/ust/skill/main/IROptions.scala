package de.ust.skill.main

import java.io.File

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import de.ust.skill.BuildInfo
import de.ust.skill.generator.common.HeaderInfo
import de.ust.skill.generator.common.KnownGenerators
import de.ust.skill.ir.TypeContext
import de.ust.skill.parser.Parser

trait IROptions extends AbstractOptions {
  case class IRConfig(
      target : String,
      header : HeaderInfo = new HeaderInfo(),
      fromSpec : File = null) extends WithProcess {

    def process {
      // create ir from spec, if requested, otherwise try to load it from target
      val sir = if (null != fromSpec) {
        val tc = Parser.process(fromSpec)
        new SIRHandler(tc, target)
      } else {
        new SIRHandler(target)
      }
    }
  }
  val irParser = new scopt.OptionParser[IRConfig]("skillc <file.sir>") {
    head("skillc", BuildInfo.version, "(skill intermediate representation mode)")

    opt[File]("from-spec").optional().action(
      (p, c) ⇒ c.copy(fromSpec = p)
    ).text("create a new sir file from an argument specification overwriting a potentially preexisting one")

    help("help").text("prints this usage text")
  }
}