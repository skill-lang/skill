/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign

import java.io.File

import scala.sys.process.stringToProcess

import org.scalatest.FunSuite
import org.scalatest.exceptions.TestFailedException

import de.ust.skill.main.CommandLine

/**
 * test for the mapping parser
 *
 * @author Constantin Weißer, Timm Felden
 */

class MappingTests extends FunSuite {

  // rebuild class files
  "ant -f src/test/resources/javaForeign/mapping/build.xml".!

  // set base path for tests
  val basePath = new File("src/test/resources/javaForeign/mapping")

  final def makeTest(name : String, mappingFile : File, skillFile : File) : Unit = {
    CommandLine.exit = { s ⇒ throw (new Error(s)) }
    CommandLine.main(Array[String](
      skillFile.getPath,
      "--debug-header",
      "-L", "javaforeign",
      "-p", name + "skill",
      s"-Ojavaforeign:M=${mappingFile.getPath}",
      s"-Ojavaforeign:F=${basePath.getAbsolutePath}",
      "-o", "/tmp"))
  }

  def fail[E <: Exception](f : ⇒ Unit)(implicit manifest : scala.reflect.Manifest[E]) : E = try {
    f;
    fail(s"expected ${manifest.runtimeClass.getName()}, but no exception was thrown");
  } catch {
    case e : TestFailedException ⇒ throw e
    case e : E ⇒
      println(e.getMessage()); e
    case e : Throwable ⇒ e.printStackTrace(); assert(e.getClass() === manifest.runtimeClass); null.asInstanceOf[E]
  }

  def succeedOn(file : File) {
    test("succeedOn: " + file.getParentFile.getName + " - " + file.getName) {
      makeTest(file.getName, file, new File(basePath, s"${file.getParentFile.getName}.skill"))
    }
  }

  def failOn(file : File) {
    test("failOn: " + file.getParentFile.getName + " - " + file.getName) {
      fail[RuntimeException] {
        makeTest(file.getName, file, new File(basePath, s"${file.getParentFile.getName}.skill"))
      }
    }
  }

  ∀(new File(basePath, "succeed"), succeedOn)
  ∀(new File(basePath, "fail"), failOn)

  def ∀(base : File, action : File ⇒ Unit) {
    for (f ← base.listFiles()) {
      if (f.isDirectory()) ∀(f, action)
      else action(f)
    }
  }
}
