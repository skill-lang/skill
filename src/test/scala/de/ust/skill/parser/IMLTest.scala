/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import java.io.File
import java.nio.file.Files

import scala.language.implicitConversions

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import de.ust.skill.main.CommandLine

@RunWith(classOf[JUnitRunner])
class IMLTest extends FunSuite {
  CommandLine.exit = s ⇒ fail(s)

  val specPath = "/home/feldentm/Desktop/iml.sf/specification/iml1.skill"
  val filename = new File(specPath)

  test("parse iml.sf") {
    if (Files.exists(filename.toPath))
      assert(100 < Parser.process(filename).allTypeNames.size)
  }

  test("create iml packages") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array(
        specPath,
        "-p", "iml",
        "-L", "statistics",
        "-L", "doxygen",
        "-L", "java",
        "-Ojava:suppressWarnings=true",
        "-L", "scala",
        "-L", "skill",
        "-L", "ecore",
        "-o", System.getProperty("user.home") + "/Desktop/iml.sf"))
  }

  test("create siml packages") {
    if (Files.exists(filename.toPath))
      CommandLine.main(Array(
        specPath,
        "-p", "siml",
        "-L", "ada",
        "-L", "cpp",
        "-Ocpp:revealSkillID=true",
        "-o", System.getProperty("user.home") + "/Desktop/iml.sf"))
  }
}
