/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import de.ust.skill.main.CommandLine

/**
 * Java specific tests.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  def check(src : String, out : String) {
    CommandLine.exit = {s ⇒ fail(s)}
    CommandLine.main(Array[String]("-L", "java", "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", out, "src/test/resources/java/"+src, "testsuites"))
  }

  test("date")(check("date.skill", "date"))
//
//  test("restrictions: range")(check("restrictions.range.skill", "restrictions.range"))
//  test("restrictions: singleton")(check("restrictions.singleton.skill", "restrictions.singleton"))
//
//  test("hints: ignore")(check("hints.ignore.skill", "hints.ignore"))
//
//  test("views: simple retyping")(check("views.skill", "views.retyping"))
//
//  test("node")(check("node.skill", "node"))
//  test("subtypesUnknown")(check("subtypesUnknown.skill", "unknown"))
//  test("datedMessage")(check("datedMessage.skill", "datedMessage"))
//
//  test("escapingg: Unit")(check("unit.skill", "unit"))
}
