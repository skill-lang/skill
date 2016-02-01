/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
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
import org.scalatest.Ignore

@RunWith(classOf[JUnitRunner])
class IMLTest extends FunSuite {
  val specPath = "/home/feldentm/Desktop/iml.sf/specification/iml-all.skill"
  val filename = new File(specPath)

  test("parse iml.sf") {
    if (Files.exists(filename.toPath))
      assert(100 < Parser.process(filename).allTypeNames.size)
  }

  test("create statistics") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "iml", "-L", "statistics", specPath,
        System.getProperty("user.home")+"/Desktop/iml.sf/generated"))
  }

  test("create doxygen") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "iml", "-L", "doxygen", specPath,
        System.getProperty("user.home")+"/Desktop/iml.sf/generated"))
  }

  test("create ada") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "siml", "-L", "ada", specPath,
        System.getProperty("user.home")+"/Desktop/iml.sf/generated"))
  }

  test("create c++") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "siml", "-L", "cpp", specPath,
        System.getProperty("user.home")+"/Desktop/iml.sf/generated"))
  }

  test("create c++sf") {
    val outDir = System.getProperty("user.home")+"/projekte/bauhausSF/functionNames++"
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "siml", "-L", "cpp", outDir+"/iml.spec",
        outDir))
  }

  test("create java") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "iml", "-L", "java", "-O@java:suppressWarnings=true", specPath,
        System.getProperty("user.home")+"/Desktop/iml.sf/generated"))
  }

  test("create scala") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "iml", "-L", "scala", specPath,
        System.getProperty("user.home")+"/Desktop/iml.sf/generated"))
  }

  test("create skill") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array("-p", "iml", "-L", "skill", specPath,
        System.getProperty("user.home")+"/Desktop/iml.sf/generated"))
  }
}
