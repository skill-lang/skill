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

  implicit private def basePath(path : String) : File = new File("src/test/resources"+path);

  private def check(filename : String) = {
    assert(0 != Parser.process(filename).allTypeNames.size)
  }

  test("good hints") { check("/hints.skill") }
  test("bad hints") { intercept[IllegalArgumentException] { check("/badHints.skill") } }
  test("restrictions") {
    val e = intercept[de.ust.skill.ir.ParseException] { check("/restrictions.skill") }
    assert("notahint() is either not supported or an invalid restriction name" === e.getMessage())
  }

  test("test")(check("/test.skill"))
  test("test2")(check("/test2.skill"))
  test("test3")(check("/test3.skill"))
  test("test4")(check("/test4.skill"))
  test("example1")(check("/example1.skill"))
  test("example2a")(check("/example2a.skill"))
  test("example2b")(check("/example2b.skill"))
  test("unicode")(check("/unicode.skill"))
  test("empty")(assert(0 === Parser.process("/empty.skill").allTypeNames.size))

  test("type ordered IR") {
    val IR = Parser.process("/typeOrderIR.skill").getUsertypes()
    val order = IR.map(_.getSkillName).mkString("")
    assert(order == "abdc" || order == "acbd", order+" is not in type order!")
  }

  test("regression: casing of user types") {
    val ir = Parser.process("/regressionCasing.skill").getUsertypes
    assert(2 === ir.size)
    // note: this is a valid test, because IR has to be in type order
    assert(ir.get(0).getSkillName === "message")
    assert(ir.get(1).getSkillName === "datedmessage")
  }

  test("regression: report missing types") {
    val e = intercept[de.ust.skill.ir.ParseException] { check("/failures/missingTypeCausedBySpelling.skill") }
    assert("""The type "MessSage" is unknown!
  Known types are: message, datedmessage""" === e.getMessage())
  }

  test("regression: comments - declaration") {
    val d = Parser.process("/comments.skill").getUsertypes.get(0)
    assert(d.getComment.format("/**\n", " *", 120, " */") === """/**
 * this is a class comment with ugly formatting but completely legal. We want to have this in a single line.
 */""")
  }

  test("regression: comments - field") {
    val d = Parser.process("/comments.skill").getUsertypes.get(0)
    for (f ← d.getFields()) {
      println(f.getComment)
      if (f.getName().camel == "commentWithStar") assert(f.getComment.format("/**\n", " *", 120, " */") === """/**
 * * <- the only real star here!
 */""")
      else if (f.getName().camel == "commentWithoutStars") assert(f.getComment.format("/**\n", " *", 120, " */") === """/**
 * funny formated comment .
 */""")
    }
  }
}
