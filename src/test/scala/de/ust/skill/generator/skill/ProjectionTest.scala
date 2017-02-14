/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.skill

import java.io.File

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
class ProjectionTest extends FunSuite {

  def check(src : String, out : String, args : Array[String] = Array()) {
    // generate code
    CommandLine.exit = { s ⇒ fail(s) }
    CommandLine.main(
      Array[String](
        "src/test/resources/skill/" + src,
        "-L", "skill",
        "-p", out,
        "-o", "testsuites/skill/" + out)
        ++ args)

    // ensure that code can be parsed again
    CommandLine.main(Array[String](
      s"testsuites/skill/$out/specification.skill",
      "-L", "skill",
      "-p", "tmp",
      "-o", "testsuites/skill"))
  }

  // ordinary spec
  for (f ← (new File("src/test/resources/skill")).listFiles if f.getName.endsWith(".skill"))
    test(s"${f.getName} - none")(check(f.getName, "none/" + f.getName.replace(".skill", "")))

  // ordinary spec without interfaces
  for (f ← (new File("src/test/resources/skill")).listFiles if f.getName.endsWith(".skill"))
    test(s"${f.getName} - interfaces")(
      check(f.getName, "interface/" + f.getName.replace(".skill", ""), Array("-Oskill:drop=interfaces"))
    )
}
