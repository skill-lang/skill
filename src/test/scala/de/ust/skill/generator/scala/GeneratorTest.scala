/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  def check(src: String, out: String) {
    Main.main(Array[String]("-u", "<<some developer>>", "-h2", "<<debug>>", "-p", out, "src/test/resources/scala/"+src, "testsuites/scala/src/main/scala/"))
  }

  test("annotation")(check("annotation.skill", "annotation"))
  test("blocks")(check("blocks.skill", "block"))
  test("date")(check("date.skill", "date"))
  test("hints: ignore")(check("hintIgnore.skill", "hints.ignore"))
  test("node")(check("node.skill", "node"))
  test("number")(check("number.skill", "number"))
  test("pamm")(check("air-pamm-heap.skill", "pamm"))
  test("subtypes")(check("subtypesExample.skill", "subtypes"))
  test("subtypesUnknown")(check("subtypesUnknown.skill", "unknown"))
}
