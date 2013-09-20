package de.ust.skill.parser

import java.io.File
import java.net.URL
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.sys.process._
import scala.language.postfixOps

@RunWith(classOf[JUnitRunner])
class ParserPrettyPrintingProjectionTest extends FunSuite {

  def check(filename: String) = {
    val parser = new Parser
    val url: URL = getClass.getResource(filename)
    val first = parser.parseAll(new File(url.getPath())).toList
    val tmp = File.createTempFile("test", "skill")
    ("echo " + ASTPrettyPrinter.prettyPrint(first)) #> tmp !
    val second = parser.parseAll(tmp).toList
    tmp.delete()
    ("echo " + ASTPrettyPrinter.prettyPrint(first)) #> tmp !
    val third = parser.parseAll(tmp).toList
    tmp.delete()
    assert(ASTEqualityChecker.checkDefinitionList(third, second))
  }

  test("test")(check("/test.skill"))
  test("hints")(check("/hints.skill"))
  test("test2")(check("/test2.skill"))
  test("test3")(check("/test3.skill"))
  test("example1")(check("/example1.skill"))
  test("example2a")(check("/example2a.skill"))
  test("example2b")(check("/example2b.skill"))
  test("unicode")(check("/unicode.skill"))
  test("air-top")(check("/air-top.skill"))
  test("air-pamm")(check("/air-pamm.skill"))
  test("air-pamm-heap")(check("/air-pamm-heap.skill"))

}