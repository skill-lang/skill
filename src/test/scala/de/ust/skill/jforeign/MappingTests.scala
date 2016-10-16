/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign

import java.io.File

import scala.collection.mutable.ArrayBuffer
import scala.reflect.io.Path.jfile2path

import org.scalatest.BeforeAndAfterAll
import org.scalatest.ConfigMap
import org.scalatest.FunSuite

import de.ust.skill.main.CommandLine
import org.scalatest.exceptions.TestFailedException

class MappingTests extends FunSuite with BeforeAndAfterAll {

  val testOnly = ""

  def language: String = "javaforeign"

  def languageOptions: ArrayBuffer[String] = ArrayBuffer()

  def finalizeTests(): Unit = {}

  final def makeTest(path: File, name: String, mappingFile: File, skillFilePath: String): Unit = {
    CommandLine.exit = { s ⇒ throw (new Error(s)) }

    val args = languageOptions ++ ArrayBuffer[String]("-L", language, "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", name + "skill")
    args += s"-O@JavaForeign:M=${mappingFile.getPath}"
    args += s"-O@JavaForeign:F=${path.getAbsolutePath}"
    args += skillFilePath
    args += "/tmp"
    CommandLine.main(args.toArray)
  }

  def fail[E <: Exception](f: ⇒ Unit)(implicit manifest: scala.reflect.Manifest[E]): E = try {
    f;
    fail(s"expected ${manifest.runtimeClass.getName()}, but no exception was thrown");
  } catch {
    case e: TestFailedException ⇒ throw e
    case e: E ⇒
      println(e.getMessage()); e
    case e: Throwable ⇒ e.printStackTrace(); assert(e.getClass() === manifest.runtimeClass); null.asInstanceOf[E]
  }

  def succeedOn(file: File) = test("succeedOn: " + file.getPath()) {
    makeTest(new File("src/test/resources/javaForeign/mapping"), file.getName, file, "src/test/resources/javaForeign/mapping/simple.skill")
  }

  def failOn(file: File) = test("failOn: " + file.getPath()) {
    fail[RuntimeException] {
      makeTest(new File("src/test/resources/javaForeign/mapping"), file.getName, file, "src/test/resources/javaForeign/mapping/simple.skill")
    }
  }

  for (path ← new File("src/test/resources/javaForeign/mapping/succeed").listFiles()) if (path.isFile()) succeedOn(path)
  for (path ← new File("src/test/resources/javaForeign/mapping/fail").listFiles()) if (path.isFile()) failOn(path)

}
