package de.ust.skill.generator.scala

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  def check(src: String, out: String) {
    Main.main(Array[String]("-p", out, "src/test/resources/scala/"+src, "testsuites/scala/src/main/scala/"))
  }

  test("annotation")(check("annotation.skill", "annotation"))
  test("date")(check("date.skill", "date"))
  test("pamm")(check("air-pamm.skill", "pamm"))
  test("blocks")(check("blocks.skill", "block"))
  test("subtypes")(check("subtypesExample.skill", "subtypes"))
  test("subtypesUnknown")(check("subtypesUnknown.skill", "unknown"))

}
