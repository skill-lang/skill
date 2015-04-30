package de.ust.skill.parser

import org.scalatest.FunSuite
import java.io.File
import scala.collection.JavaConversions._
import scala.language.implicitConversions
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import de.ust.skill.generator
import de.ust.skill.main.CommandLine
import java.nio.file.Files

@RunWith(classOf[JUnitRunner])
class IMLTest extends FunSuite {
  val filename = new File("/home/feldentm/Desktop/iml.sf/specification/iml-all.skill")

  if (Files.exists(filename.toPath)) {

    test("parse iml.sf") {
      assert(100 < Parser.process(filename).allTypeNames.size)
    }

    test("create statistics") {
      CommandLine.main(Array("-p", "iml", "-L", "statistics", "/home/feldentm/Desktop/iml.sf/specification/iml-all.skill",
        "/home/feldentm/Desktop/iml.sf/generated"))
    }

    test("create doxygen") {
      CommandLine.main(Array("-p", "iml", "-L", "doxygen", "/home/feldentm/Desktop/iml.sf/specification/iml-all.skill",
        "/home/feldentm/Desktop/iml.sf/generated"))
    }

    ignore("create ada") {
      CommandLine.main(Array("-p", "iml", "-L", "ada", "/home/feldentm/Desktop/iml.sf/specification/iml-all.skill",
        "/home/feldentm/Desktop/iml.sf/generated"))
    }

    test("create java") {
      CommandLine.main(Array("-p", "iml", "-L", "java", "-O@java:suppressWarnings=true", "/home/feldentm/Desktop/iml.sf/specification/iml-all.skill",
        "/home/feldentm/Desktop/iml.sf/generated"))
    }

    test("create scala") {
      CommandLine.main(Array("-p", "iml", "-L", "scala", "/home/feldentm/Desktop/iml.sf/specification/iml-all.skill",
        "/home/feldentm/Desktop/iml.sf/generated"))
    }
  }
}