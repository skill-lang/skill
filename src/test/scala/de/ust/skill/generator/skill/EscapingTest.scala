/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.skill

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import de.ust.skill.main.CommandLine
import java.io.File
import scala.sys.process._
import de.ust.skill.generator.common.KnownGenerators

/**
 * Java specific tests.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class EscapingTest extends FunSuite {

  def check(language : String, words : Array[String], escaping : Array[Boolean]) {
    CommandLine.exit = { s ⇒ fail(s) }
    val result = CommandLine.checkEscaping(language, words.iterator)

    assert(result === escaping.mkString(" "))
  }

  // if is a keyword in all real languages
  for (l ← CommandLine.known.keySet if l != "skill" && l != "statistics")
    test(s"${l} - none")(check(l, Array("if"), Array(true)))

  // some language keywords
  test("Ada - keywords") {
    check("ada", Array("while", "others", "in", "out", "case"), Array(true, true, true, true, true))
  }

  test("Java - keywords") {
    check("java", Array("int", "is", "not", "a", "class"), Array(true, false, false, false, true))
  }
}
