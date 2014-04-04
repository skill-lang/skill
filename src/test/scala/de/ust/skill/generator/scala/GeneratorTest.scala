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

  def check(src : String, out : String) {
    Main.main(Array[String]("-u", "<<some developer>>", "-h2", "<<debug>>", "-p", out, "src/test/resources/scala/"+src, "testsuites/scala/src/main/scala/"))
  }

  test("graph benchmark")(check("graphBenchmark.skill", "benchmarks.graph"))

  test("annotation")(check("annotation.skill", "annotation"))
  test("blocks")(check("blocks.skill", "block"))
  test("container")(check("container.skill", "container"))
  test("date")(check("date.skill", "date"))
  test("graph")(check("graph.skill", "graph"))
  test("float")(check("float.skill", "floatTest"))

  test("restrictions: range")(check("restrictions.range.skill", "restrictions.range"))
  test("restrictions: singleton")(check("restrictions.singleton.skill", "restrictions.singleton"))

  test("hints: ignore")(check("hintIgnore.skill", "hints.ignore"))

  test("node")(check("node.skill", "node"))
  test("number")(check("number.skill", "number"))
  test("subtypes")(check("subtypesExample.skill", "subtypes"))
  test("subtypesUnknown")(check("subtypesUnknown.skill", "unknown"))

  /**
   * generate code for a more complex example that makes use of a set of tools to modify very simple nodes.
   * these tools enrich nodes with custom fields.
   * the related tests should also serve as a demonstration on the boundaries of format-change.
   */
  test("node tool chain example") {
    check("nodeExample.tool1.skill", "toolchains.node.tool1")
    check("nodeExample.tool2.skill", "toolchains.node.tool2")
    check("nodeExample.tool3.skill", "toolchains.node.tool3")
    check("nodeExample.viewer.skill", "toolchains.node.viewer")
  }

  test("check _root_ bug")(
    assert(intercept[AssertionError] {
      Main.main(Array[String]("-u", "<<some developer>>", "-h2", "<<debug>>", "src/test/resources/scala/date.skill", "testsuites/scala/src/main/scala/"))
    }.getMessage === "assertion failed: You have to specify a non-empty package name!")
  )
}
