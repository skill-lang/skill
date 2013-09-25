package de.ust.skill.parser

import java.io.File
import java.lang.AssertionError
import java.net.URL
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Contains a lot of tests, which should not pass the type checker.
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class TypeCheckerTest extends FunSuite {

  private def check(filename: String) {
    val url: URL = getClass.getResource(filename)
    Parser.process(new File(url.getPath()))
  }

  test("duplicateDefinition") {
    intercept[AssertionError] {
      check("/failures/duplicateDefinition.skill")
    }
  }

  test("duplicateField") {
    intercept[AssertionError] {
      check("/failures/duplicateField.skill")
    }
  }

  test("halfFloat") {
    intercept[AssertionError] {
      check("/failures/halfFloat.skill")
    }
  }

  test("floatConstant") {
    intercept[AssertionError] {
      check("/failures/floatConstant.skill")
    }
  }

  test("selfConst") {
    intercept[AssertionError] {
      check("/failures/selfConst.skill")
    }
  }

  test("unknownType") {
    intercept[AssertionError] {
      check("/failures/unknownType.skill")
    }
  }

  test("unknownFile") {
    intercept[AssertionError] {
      check("/failures/unknownFile.skill")
    }
  }

  test("empty") {
    intercept[AssertionError] {
      check("/failures/empty.skill")
    }
  }

  test("anyType") {
    intercept[AssertionError] {
      check("/failures/anyType.skill")
    }
  }

}
