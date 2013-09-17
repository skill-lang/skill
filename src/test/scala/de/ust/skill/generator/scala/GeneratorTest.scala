package de.ust.skill.generator.scala

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit

class GeneratorTest extends AssertionsForJUnit {
  def check(src: String, out: String) {
    Main.main(Array[String]("-p", out, "src/test/resources/scala/"+src, "tmp/scala/src/"))
  }

  @Test def date = check("date.skill", "date")
  @Test def pamm = check("air-pamm.skill", "pamm")
  @Test def blocks = check("blocks.skill", "block")
  @Test def subtypes = check("subtypesExample.skill", "subtypes")
  @Test def subtypesUnknown = check("subtypesUnknown.skill", "unknown")
}