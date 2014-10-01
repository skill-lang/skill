/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import de.ust.skill.main.CommandLine

/**
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  def check(src : String, out : String) {
    CommandLine.main(Array[String]("-L", "scala", "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", out, "src/test/resources/scala/"+src, "testsuites"))
  }

  test("benchmark: colored graph")(check("benchmarks.coloredGraph.skill", "benchmarks.coloredGraph"))
  test("benchmark: graph")(check("benchmarks.graph.skill", "benchmarks.graph"))

  test("annotation")(check("annotation.skill", "annotation"))
  test("blocks")(check("blocks.skill", "block"))
  test("container")(check("container.skill", "container"))
  test("date")(check("date.skill", "date"))
  test("graph")(check("graph.skill", "graph"))
  test("float")(check("float.skill", "floatTest"))
  test("unicode")(check("unicode.skill", "unicode"))

  test("restrictions: range")(check("restrictions.range.skill", "restrictions.range"))
  test("restrictions: singleton")(check("restrictions.singleton.skill", "restrictions.singleton"))

  test("hints: ignore")(check("hintIgnore.skill", "hints.ignore"))

  test("views: simple retyping")(check("views.skill", "views.retyping"))

  test("node")(check("node.skill", "node"))
  test("number")(check("number.skill", "number"))
  test("subtypes")(check("subtypesExample.skill", "subtypes"))
  test("subtypesUnknown")(check("subtypesUnknown.skill", "unknown"))
  test("datedMessage")(check("datedMessage.skill", "datedMessage"))

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
}
