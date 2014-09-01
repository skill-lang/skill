package de.ust.skill.parser

import org.scalatest.FunSuite
import java.io.File
import scala.collection.JavaConversions._
import scala.language.implicitConversions
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import de.ust.skill.generator;

@RunWith(classOf[JUnitRunner])
class IMLTest extends FunSuite {
  val filename = new File("/home/feldentm/Desktop/iml.sf/specification/iml-all.skill")

  test("parse iml.sf") {
    assert(100 < Parser.process(filename).size)
  }

  test("create scala") {
    generator.scala.Main.main(Array("/home/feldentm/Desktop/iml.sf/specification/iml-all.skill",
      "/home/feldentm/Desktop/iml.sf/specification/generated/scala"))
  }

  test("create ada") {
    generator.ada.Main.main(Array("/home/feldentm/Desktop/iml.sf/specification/iml-all.skill",
      "/home/feldentm/Desktop/iml.sf/specification/generated/scala"))
  }
}