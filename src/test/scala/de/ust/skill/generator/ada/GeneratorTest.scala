/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import de.ust.skill.main.CommandLine

@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  def check(src : String, out : String) {
    CommandLine.exit = {s ⇒ fail(s)}
    CommandLine.main(Array[String]("-L", "ada", "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", out, "src/test/resources/ada/"+src, "testsuites/"))
  }

  test("aircraft")(check("aircraft.skill", "aircraft"))
  test("autofield")(check("autofield.skill", "autofield"))
  test("date")(check("date.skill", "date"))
  test("filter")(check("filter.skill", "filter"))
  test("graph_1")(check("graph1.skill", "graph_1"))
  test("graph_2")(check("graph2.skill", "graph_2"))
  test("node")(check("nodeExample.tool1.skill", "node"))
}
