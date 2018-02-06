/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import de.ust.skill.main.CommandLine

@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  def check(src : String, out : String) {
    CommandLine.exit = { s ⇒ fail(s) }
    CommandLine.main(Array[String]("src/test/resources/cpp/" + src,
      "--debug-header",
      "-c",
      "-L", "cpp",
      "-p", out,
      "-o", "testsuites/cpp/src/" + out))
  }

  // use this test to check that build without reveal skillID can be performed
  test("aircraft")(check("aircraft.skill", "aircraft"))
  
  // test for nested namespaces
  test("aircraft -- nested")(check("aircraft.skill", "air.craft"))
}
