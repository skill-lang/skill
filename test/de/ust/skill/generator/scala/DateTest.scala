package de.ust.skill.generator.scala

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test

class DateTest extends AssertionsForJUnit {
  @Test def test {
    Main.main(Array[String]("testdata/scala/date.skill", "testdata/scala/src/"))
  }
}