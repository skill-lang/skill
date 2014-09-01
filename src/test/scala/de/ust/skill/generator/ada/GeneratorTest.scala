/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  def check(src: String, out: String) {
    Main.main(Array[String]("-u", "<<some developer>>", "-h2", "<<debug>>", "-p", out, "src/test/resources/ada/"+src, "testsuites/ada/src/"))
  }

  test("aircraft")(check("aircraft.skill", "aircraft"))
  test("annotation")(check("annotation.skill", "annotation"))
  test("autofield")(check("autofield.skill", "autofield"))
  test("constants")(check("constant.skill", "constants"))
  test("container")(check("container.skill", "container"))
  test("date")(check("date.skill", "date"))
  test("filter")(check("filter.skill", "filter"))
  test("floats")(check("float.skill", "floats"))
  test("graph_1")(check("graph1.skill", "graph_1"))
  test("graph_2")(check("graph2.skill", "graph_2"))
  test("node")(check("nodeExample.tool1.skill", "node"))
  test("number")(check("number.skill", "number"))
  test("subtypes")(check("subtypesExample.skill", "subtypes"))
  test("subtypesUnknown")(check("subtypesUnknown.skill", "unknown"))
  test("utf8 handling")(check("utf8.skill", "utf8"))
}
