/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.javaforeign

import java.io.File

import scala.collection.mutable.ArrayBuffer
import scala.reflect.io.Path.jfile2path

import org.scalatest.BeforeAndAfterAll
import org.scalatest.ConfigMap
import org.scalatest.FunSuite

import de.ust.skill.main.CommandLine

class GenericTests extends FunSuite with BeforeAndAfterAll {

  val testOnly = ""

  def language: String = "javaforeign"

  def languageOptions: ArrayBuffer[String] = ArrayBuffer()

  def deleteOutDir(out: String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/javaForeign/src/main/java/", out)).deleteRecursively
  }

  def makeGenBinaryTests(name: String): Unit = {

  }

  /**
   * helper function that collects binaries for a given test name.
   *
   * @return (accept, reject)
   */
  final def collectBinaries(name: String): (Seq[File], Seq[File]) = {
    val base = new File("src/test/resources/genbinary")
    def collect(f: File): Seq[File] =
      (for (path ← f.listFiles if path.isDirectory) yield collect(path)).flatten ++
        f.listFiles.filterNot(_.isDirectory)

    val targets = (
      collect(new File(base, "[[all]]"))
      ++ collect(if (new File(base, name).exists) new File(base, name) else new File(base, "[[empty]]"))).filter(_.getName.endsWith(".sf")).sortBy(_.getName)

    targets.partition(_.getPath.contains("accept"))
  }

  def finalizeTests(): Unit = {}

  final def makeTest(path: File, name: String, mappingFile: File, skillFilePath: String) = test("generic: " + name) {
    deleteOutDir(name)

    CommandLine.exit = { s ⇒ throw (new Error(s)) }

    val args = languageOptions ++ ArrayBuffer[String]("-L", language, "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", name + "skill")
    args += "-M"
    args += mappingFile.getPath
    args += "-F"
    args += path.getAbsolutePath
    args += skillFilePath
    args += "testsuites"
    CommandLine.main(args.toArray)

    makeGenBinaryTests(name)
  }

  implicit class Regex(sc: StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ ⇒ "x"): _*)
  }

  for (path ← new File("src/test/resources/javaForeign").listFiles() if path.isDirectory() && path.getName.endsWith(testOnly)) {
    val baseName: String = path.getName
    val mappingFile = new File(path.getAbsolutePath + "/mapping")
    assert(mappingFile.exists(), s"Mapping file not found in ${path.getAbsolutePath}")
    val skillSpec = new File(path.getAbsolutePath + s"/$baseName.skill")
    val skillSpecPath = if (skillSpec.exists()) skillSpec.getPath else "-"

    try {
      makeTest(path, baseName, mappingFile, skillSpecPath)
    } catch {
      case e: MatchError ⇒
        println(s"failed processing of $path:")
        e.printStackTrace(System.out)
    }
  }

  override def afterAll(configMap: ConfigMap) {
    finalizeTests
  }
}
