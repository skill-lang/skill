/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.parser

import java.io.File

import scala.language.implicitConversions

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.exceptions.TestFailedException
import org.scalatest.junit.JUnitRunner

import de.ust.skill.ir

/**
 * Contains generic parser tests based on src/test/resources/frontend directory.
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GenericFrontendTest extends FunSuite {

  private def check(file : File) = Parser.process(file)

  def fail[E <: Exception](f : ⇒ Unit)(implicit manifest : scala.reflect.Manifest[E]) : E = try {
    f;
    fail(s"expected ${manifest.runtimeClass.getName()}, but no exception was thrown");
  } catch {
    case e : TestFailedException ⇒ throw e
    case e : E ⇒
      println(e.getMessage()); e
    case e : Throwable ⇒ e.printStackTrace(); assert(e.getClass() === manifest.runtimeClass); null.asInstanceOf[E]
  }

  def succeedOn(file : File) = test("succeedOn: "+file.getName()) { check(file) }

  def failOn(file : File) = test("failOn: "+file.getName()) {
    fail[ir.ParseException] {
      check(file)
    }
  }

  for (f ← new File("src/test/resources/frontend").listFiles() if f.isFile() && f.getName.endsWith(".skill")) succeedOn(f)
  for (f ← new File("src/test/resources/frontend/ParseException").listFiles() if f.isFile() && f.getName.endsWith(".skill")) failOn(f)
}
