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
      Array[String]("-L", "skill", "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", out)
        ++ args
        ++ Array[String]("src/test/resources/skill/"+src, "testsuites"))

    // ensure that code can be parsed again
    CommandLine.main(Array[String]("-L", "skill", "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", "tmp", s"testsuites/skill/$out/specification.skill", "testsuites"))
  }

  // ordinary spec
  for (f ← (new File("src/test/resources/skill")).listFiles if f.getName.endsWith(".skill"))
    test(s"${f.getName} - none")(check(f.getName, "none/"+f.getName.replace(".skill", "")))

  // ordinary spec without interfaces
  for (f ← (new File("src/test/resources/skill")).listFiles if f.getName.endsWith(".skill"))
    test(s"${f.getName} - interfaces")(
      check(f.getName, "interface/"+f.getName.replace(".skill", ""), Array("-O@skill:drop=interfaces"))
    )
}
