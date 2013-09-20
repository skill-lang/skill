package de.ust.skill.parser

import java.io.File
import java.net.URL
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConversions._

@RunWith(classOf[JUnitRunner])
class ParserTest extends FunSuite {

  private def check(filename: String) = {
    val parser = new Parser
    val url: URL = getClass.getResource(filename)
    assert(0 != parser.parseAll(new File(url.getPath())).size)
  }

  test("hints")(check("/hints.skill"))
  test("restrictions")(check("/restrictions.skill"))

  test("skill")(check("/test.skill"))
  test("test2")(check("/test2.skill"))
  test("test3")(check("/test3.skill"))
  test("test4")(check("/test4.skill"))
  test("example1")(check("/example1.skill"))
  test("example2a")(check("/example2a.skill"))
  test("example2b")(check("/example2b.skill"))
  test("unicode")(check("/unicode.skill"))
  test("air-top")(check("/air-top.skill"))
  test("air-pamm")(check("/air-pamm.skill"))
  test("air-pamm-heap")(check("/air-pamm-heap.skill"))

  test("properIncludes") {
    val parser = new Parser
    val url1: URL = getClass.getResource("/air-top.skill")
    val url2: URL = getClass.getResource("/air-pamm.skill")
    val list1: List[Definition] = parser.parseAll(new File(url1.getPath())).toList
    val list2: List[Definition] = parser.parseAll(new File(url2.getPath())).toList
    assert(ASTEqualityChecker.checkDefinitionList(list1, list2))
  }

  test("process") {
    val parser = new Parser
    val url1: URL = getClass.getResource("/air-top.skill")
    val url2: URL = getClass.getResource("/air-pamm.skill")
    val array1: Array[Object] = parser.process(new File(url1.getPath())).map(_.toString()).toArray
    val array2: Array[Object] = parser.process(new File(url2.getPath())).map(_.toString()).toArray
    assert(array1 != array2)
  }

}