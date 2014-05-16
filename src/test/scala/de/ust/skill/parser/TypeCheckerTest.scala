/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
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
 * Contains a lot of tests, which should not pass the type checker.
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class TypeCheckerTest extends FunSuite {

  implicit private def basePath(path: String): File = new File("src/test/resources"+path);
  private def check(filename: String) = Parser.process(filename)

  def fail[E <: Exception](f: ⇒ Unit)(implicit manifest: scala.reflect.Manifest[E]): E = try {
    f;
    fail(s"expected ${manifest.runtimeClass.getName()}, but no exception was thrown");
  } catch {
    case e: TestFailedException ⇒ throw e
    case e: E ⇒
      println(e.getMessage()); e
    case e: Throwable ⇒ e.printStackTrace(); assert(e.getClass() === manifest.runtimeClass); null.asInstanceOf[E]
  }

  test("duplicateDefinition") {
    fail[ir.ParseException] {
      check("/failures/duplicateDefinition.skill")
    }
  }

  test("case sensitive duplicate definition") {
    fail[ir.ParseException] {
      check("/failures/duplicateDefinitionCaseSensitive.skill")
    }
  }

  test("cyclic definitions") {
    fail[ir.ParseException] {
      check("/failures/cyclicDefinitions.skill")
    }
  }

  test("duplicateField") {
    fail[ir.ParseException] {
      check("/failures/duplicateField.skill")
    }
  }

  test("halfFloat") {
    fail[ir.ParseException] {
      check("/failures/halfFloat.skill")
    }
  }

  test("floatConstant") {
    fail[ir.ParseException] {
      check("/failures/floatConstant.skill")
    }
  }

  test("selfConst") {
    fail[ir.ParseException] {
      check("/failures/selfConst.skill")
    }
  }

  test("unknownType") {
    fail[ir.ParseException] {
      check("/failures/unknownType.skill")
    }
  }

  test("unknownFile") {
    fail[ir.ParseException] {
      check("/failures/unknownFile.skill")
    }
  }

  test("anyType") {
    fail[ir.ParseException] {
      check("/failures/anyType.skill")
    }
  }

  test("negative array size") {
    fail[ir.ParseException] {
      check("/failures/negativeArrayLength.skill")
    }
  }

  test("nesting is illegal") {
    fail[ir.ParseException] {
      check("/failures/compound.skill")
    }
  }

}
