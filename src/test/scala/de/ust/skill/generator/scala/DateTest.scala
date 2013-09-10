package de.ust.skill.generator.scala

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit

class DateTest extends AssertionsForJUnit {
  @Test def test {
    Main.main(Array[String]("src/test/resources/scala/date.skill", "tmp/scala/src/"))
  }
}