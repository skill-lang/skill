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
import java.io.File
import java.nio.file.Files

/**
 * Generic tests built for scala.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GenericTests extends FunSuite {

  def check(path : File, out : String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/scala/src/main/scala/", out)).deleteRecursively

    CommandLine.main(Array[String]("-L", "scala", "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", out, path.getPath, "testsuites"))
  }

  def makeTest(path : File, name : String) = test("generic: "+name)(check(path, name))

  implicit class Regex(sc : StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ ⇒ "x") : _*)
  }

  for (path ← new File("src/test/resources/gentest").listFiles if path.getName.endsWith(".skill")) {
    try {
      val r"""#!\s(\w+)${ name }""" = Files.lines(path.toPath).findFirst().orElse("")
      makeTest(path, name)
    } catch {
      case e : MatchError ⇒ // just continue, the first line did not match the command
    }
  }
}
