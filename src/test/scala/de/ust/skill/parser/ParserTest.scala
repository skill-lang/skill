/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import java.io.File
import scala.collection.JavaConversions._
import scala.language.implicitConversions
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ParserTest extends FunSuite {

  implicit private def basePath(path: String): File = new File("src/test/resources"+path);

  private def check(filename: String) = {
    assert(0 != Parser.process(filename).size)
  }

  test("good hints") { check("/hints.skill") }
  test("bad hints") { intercept[IllegalArgumentException] { check("/badHints.skill") } }
  test("restrictions") {
    val e = intercept[de.ust.skill.ir.ParseException] { check("/restrictions.skill") }
    assert("notAHint() is either not supported or an invalid restriction name" === e.getMessage())
  }

  test("test")(check("/test.skill"))
  test("test2")(check("/test2.skill"))
  test("test3")(check("/test3.skill"))
  test("test4")(check("/test4.skill"))
  test("example1")(check("/example1.skill"))
  test("example2a")(check("/example2a.skill"))
  test("example2b")(check("/example2b.skill"))
  test("unicode")(check("/unicode.skill"))
  test("empty")(assert(0 === Parser.process("/empty.skill").size))

}
