/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.common

import java.io.File

import scala.io.Codec

import org.scalatest.BeforeAndAfterAll
import org.scalatest.ConfigMap
import org.scalatest.FunSuite

import de.ust.skill.main.CommandLine
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import scala.io.Source

/**
 * Common implementation of generic tests
 *
 * @author Timm Felden
 */
abstract class GenericTests extends FunSuite with BeforeAndAfterAll {

  /**
   * can be used to restrict tests to a single specification; should always be [[empty string]] on commit
   */
  val testOnly = ""

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
   * hook called to call the generator
   */
  def callMainFor(name : String, source : String, options : Seq[String])

  /**
   *  creates unit tests in the target language
   */
  def makeGenBinaryTests(name : String) : Unit

  def collect(f : File) : Seq[File] = {
      (for (path ← f.listFiles if path.isDirectory) yield collect(path)).flatten ++
        f.listFiles.filterNot(_.isDirectory)
  }
  
  /**
   * helper function that collects binaries for a given test name.
   *
   * @return (accept, reject)
   */
  final def collectBinaries(name : String) : (Seq[File], Seq[File]) = {
    val base = new File("src/test/resources/genbinary")

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

  final def makeTest(path : File, name : String, options : Seq[String]) : Unit = test("generic: " + name) {
    deleteOutDir(name)

    CommandLine.exit = { s ⇒ throw (new Error(s)) }

    callMainFor(name, path.getPath, options)

    makeGenBinaryTests(name)
  }

  implicit class Regex(sc : StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ ⇒ "x") : _*)
  }

  for (path ← getFileList(new File("src/test/resources/gentest")) if path.getName.endsWith(testOnly + ".skill")) {
    try {
      val r"""#!\s(\w+)${ name }(.*)${ options }""" =
        io.Source.fromFile(path)(Codec.UTF8).getLines.toSeq.headOption.getOrElse("")

      makeTest(path, name, options.split("\\s+").filter(_.length() != 0))
    } catch {
      case e : MatchError ⇒
        println(s"failed processing of $path:")
        e.printStackTrace(System.out)
    }
  }

  override def afterAll(configMap : ConfigMap) {
    finalizeTests
  }

  def getFileList(startFile: File): Array[File] = {
    val files = startFile.listFiles();
    files ++ files.filter(_.isDirectory()).flatMap(getFileList);
  }
}
