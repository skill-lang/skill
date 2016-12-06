/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
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

  final def makeTest(path : File, name : String, mappingFile : File, skillFilePath : String) : Unit = {
    CommandLine.exit = { s ⇒ throw (new Error(s)) }
    CommandLine.main(Array[String](
      skillFilePath,
      "--debug-header",
      "-L", "javaforeign",
      "-p", name + "skill",
      s"-Ojavaforeign:M=${mappingFile.getPath}",
      s"-Ojavaforeign:F=${path.getAbsolutePath}",
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
    test("succeedOn: " + file.getPath()) {
      makeTest(new File("src/test/resources/javaForeign/mapping"), file.getName,
        file, "src/test/resources/javaForeign/mapping/simple.skill")
    }
  }

  def failOn(file : File) {
    test("failOn: " + file.getPath()) {
      fail[RuntimeException] {
        makeTest(new File("src/test/resources/javaForeign/mapping"), file.getName,
          file, "src/test/resources/javaForeign/mapping/simple.skill")
      }
    }
  }

  "ant -f src/test/resources/javaForeign/mapping/build.xml".!

  for (path ← new File("src/test/resources/javaForeign/mapping/succeed").listFiles()) if (path.isFile()) succeedOn(path)
  for (path ← new File("src/test/resources/javaForeign/mapping/fail").listFiles()) if (path.isFile()) failOn(path)

}
