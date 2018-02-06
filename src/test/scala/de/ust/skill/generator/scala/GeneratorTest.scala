/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import de.ust.skill.main.CommandLine

/**
 * Scala specific tests.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  def check(src : String, out : String) {
    CommandLine.exit = { s ⇒ fail(s) }
    CommandLine.main(Array[String](
      "src/test/resources/scala/" + src,
      "--debug-header",
      "-L", "scala",
      "-Oscala:revealSkillID=true",
      "-p", out,
      "-d", "testsuites/scala/lib",
      "-o", "testsuites/scala/src/main/scala"))
  }

  test("benchmark: colored graph")(check("benchmarks.coloredGraph.skill", "benchmarks.coloredGraph"))
  test("benchmark: graph")(check("benchmarks.graph.skill", "benchmarks.graph"))

  test("blocks")(check("blocks.skill", "block"))
  test("date")(check("date.skill", "date"))

  test("restrictions: range")(check("restrictions.range.skill", "restrictions.range"))
  test("restrictions: singleton")(check("restrictions.singleton.skill", "restrictions.singleton"))

  test("hints: ignore")(check("hints.ignore.skill", "hints.ignore"))

  test("views: simple retyping")(check("views.skill", "views.retyping"))

  test("node")(check("node.skill", "node"))
  test("datedMessage")(check("datedMessage.skill", "datedMessage"))

  test("escapingg: Unit")(check("unit.skill", "unit"))

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
