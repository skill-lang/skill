package de.ust.skill.generator.scala

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test

class GeneratorTest extends AssertionsForJUnit {
  def check(src: String, out: String) {
    Main.main(Array[String]("-p", out, "src/test/resources/scala/"+src, "tmp/scala/src/"))
  }

  @Test def date = check("date.skill", "date")
  @Test def pamm = check("air-pamm.skill", "pamm")
  @Test def blocks = check("blocks.skill", "block")
}