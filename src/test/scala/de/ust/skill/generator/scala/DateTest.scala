package de.ust.skill.generator.scala

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class DateTest extends FunSuite {

  test("test")(Main.main(Array[String]("src/test/resources/scala/date.skill", "tmp/scala/src/")))

}