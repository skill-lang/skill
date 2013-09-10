package de.ust.skill.generator.scala

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit

class DateTest extends AssertionsForJUnit {
  @Test def test {
    Main.main(Array[String]("scala/date.skill", "scala/src/"))
  }
}