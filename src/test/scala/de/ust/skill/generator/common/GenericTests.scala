/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.common

import org.scalatest.FunSuite
import java.io.File
import de.ust.skill.main.CommandLine
import scala.collection.mutable.ArrayBuffer
import java.nio.file.Files
import org.scalatest.Assertions
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ConfigMap

/**
 * Common implementation of generic tests
 *
 * @author Timm Felden
 */
abstract class GenericTests extends FunSuite with BeforeAndAfterAll {

  /**
   * can be used to restrict tests to a single specification; should always be [[empty string]] on commit
   */
  val testOnly = "unicode"

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

  /**
   * helper function that collects binaries for a given test name.
   *
   * @return (accept, reject)
   */
  final def collectBinaries(name : String) : (Seq[File], Seq[File]) = {
    val base = new File("src/test/resources/genbinary")
    def collect(f : File) : Seq[File] =
      (for (path ← f.listFiles if path.isDirectory) yield collect(path)).flatten ++
        f.listFiles.filterNot(_.isDirectory)

    val targets = (
      collect(new File(base, "[[all]]"))
      ++ collect(if (new File(base, name).exists) new File(base, name) else new File(base, "[[empty]]"))
    ).filter(_.getName.endsWith(".sf")).sortBy(_.getName)

    targets.partition(_.getPath.contains("accept"))
  }

  /**
   * hook called once after all tests have been generated
   */
  def finalizeTests() : Unit

  final def makeTest(path : File, name : String, options : String) = test("generic: "+name) {
    deleteOutDir(name)

    CommandLine.exit = { s ⇒ throw (new Error(s)) }

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

  for (path ← new File("src/test/resources/gentest").listFiles if path.getName.endsWith(testOnly+".skill")) {
    try {
      val r"""#!\s(\w+)${ name }(.*)${ options }""" = Files.lines(path.toPath).findFirst().orElse("")
      makeTest(path, name, options.trim)
    } catch {
      case e : MatchError ⇒
        println(s"failed processing of $path:")
        e.printStackTrace(System.out)
    }
  }

  override def afterAll(configMap : ConfigMap) {
    finalizeTests
  }
}
