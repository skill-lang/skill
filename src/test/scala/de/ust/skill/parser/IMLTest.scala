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
  val specPath = "/tmp/iml.sf/specification/iml-all.skill"
  val filename = new File(specPath)

  test("parse iml.sf") {
    if (Files.exists(filename.toPath))
      assert(100 < Parser.process(filename).allTypeNames.size)
  }

  test("create statistics") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "iml", "-L", "statistics", specPath,
        System.getProperty("user.home") + "/Desktop/iml.sf/generated"))
  }

  test("create doxygen") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "iml", "-L", "doxygen", specPath,
        System.getProperty("user.home") + "/Desktop/iml.sf/generated"))
  }

  ignore("create ada") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "iml", "-L", "ada", specPath,
        System.getProperty("user.home") + "/Desktop/iml.sf/generated"))
  }

  test("create java") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "iml", "-L", "java", "-O@java:suppressWarnings=true", specPath,
        System.getProperty("user.home") + "/Desktop/iml.sf/generated"))
  }

  test("create scala") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "iml", "-L", "scala", specPath,
        System.getProperty("user.home") + "/Desktop/iml.sf/generated"))
  }
}