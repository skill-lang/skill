/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import java.io.File
import java.net.URL
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

  test("hints")(check("/hints.skill"))
  test("restrictions")(check("/restrictions.skill"))

  test("test")(check("/test.skill"))
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
  test("empty")(assert(0 === Parser.process("/empty.skill").size))

  test("process") {
    val parser = new Parser
    val array1: Array[Object] = Parser.process("/air-top.skill").map(_.toString()).toArray
    val array2: Array[Object] = Parser.process("/air-pamm.skill").map(_.toString()).toArray
    assert(array1 != array2)
  }

}
