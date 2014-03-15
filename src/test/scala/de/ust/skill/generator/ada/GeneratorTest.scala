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
  test("constants")(check("constant.skill", "constants"))
  test("container")(check("container.skill", "container"))
  test("date")(check("date.skill", "date"))
  test("graph")(check("graph.skill", "graph"))
  test("node")(check("nodeExample.tool1.skill", "node"))
  test("subtypes")(check("subtypesExample.skill", "subtypes"))
  test("subtypesUnknown")(check("subtypesUnknown.skill", "unknown"))
}
