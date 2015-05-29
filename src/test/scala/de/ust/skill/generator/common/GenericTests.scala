package de.ust.skill.generator.common

import org.scalatest.FunSuite
import java.io.File
import de.ust.skill.main.CommandLine
import scala.collection.mutable.ArrayBuffer
import java.nio.file.Files
import org.scalatest.Assertions

/**
 * Common implementation of generic tests
 *
 * @author Timm Felden
 */
abstract class GenericTests extends FunSuite {

  /**
   * parameter name of the language. This is required for code generator invocation.
   */
  def language : String

  /**
   * This method should delete output directories, if they exist.
   * It is invoked once before invocation of CommandLine.main
   */
  def deleteOutDir(out : String) : Unit

  /**
   *  creates unit tests in the target language
   */
  def makeGenBinaryTests(name : String) : Unit

  final def makeTest(path : File, name : String, options : String) = test("generic: "+name) {
    deleteOutDir(name)

    CommandLine.exit = { s ⇒ throw(new Error(s)) }

    val args = ArrayBuffer[String]("-L", language, "-O@java:SuppressWarnings=true", "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", name)
    if (options.size > 0)
      args ++= options.split("\\s+").to
    args += path.getPath
    args += "testsuites"
    CommandLine.main(args.toArray)

    makeGenBinaryTests(name)
  }

  implicit class Regex(sc : StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ ⇒ "x") : _*)
  }

  for (path ← new File("src/test/resources/gentest").listFiles if path.getName.endsWith(".skill")) {
    try {
      val r"""#!\s(\w+)${ name }(.*)${ options }""" = Files.lines(path.toPath).findFirst().orElse("")
      makeTest(path, name, options.trim)
    } catch {
      case e : MatchError ⇒
        println(s"failed processing of $path:")
        e.printStackTrace(System.out)
    }
  }
}