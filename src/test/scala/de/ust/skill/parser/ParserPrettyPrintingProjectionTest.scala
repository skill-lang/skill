/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import scala.language.postfixOps

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ParserPrettyPrintingProjectionTest extends FunSuite {

  private def check(filename: String) = {
    // FIXME remake
//    val url: URL = getClass.getResource(filename)
//    val first = Parser.parseAll(new File(url.getPath())).toList
//    val tmp = File.createTempFile("test", "skill")
//    ("echo " + ASTPrettyPrinter.prettyPrint(first)) #> tmp !
//    val second = parser.parseAll(tmp).toList
//    tmp.delete()
//    ("echo " + ASTPrettyPrinter.prettyPrint(first)) #> tmp !
//    val third = parser.parseAll(tmp).toList
//    tmp.delete()
//    assert(ASTEqualityChecker.checkDefinitionList(third, second))
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
