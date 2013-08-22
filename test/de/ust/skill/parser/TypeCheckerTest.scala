package de.ust.skill.parser

import java.io.File
import java.lang.AssertionError

import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit

/**
 * Contains a lot of tests, which should not pass the type checker.
 * @author Timm Felden
 */
class TypeCheckerTest extends AssertionsForJUnit {
  private def check(path: String) {
    val p = new Parser
    p.parseAll(new File(path))
  }

  @Test(expected = classOf[AssertionError]) def dupDefs = check("testdata/failures/duplicateDefinition.skill")
  @Test(expected = classOf[AssertionError]) def dupField = check("testdata/failures/duplicateField.skill")
  @Test(expected = classOf[AssertionError]) def halfFloat = check("testdata/failures/halfFloat.skill")
  @Test(expected = classOf[AssertionError]) def constFloat = check("testdata/failures/floatConstant.skill")
  @Test(expected = classOf[AssertionError]) def constSelf = check("testdata/failures/selfConst.skill")
  @Test(expected = classOf[AssertionError]) def unkownType = check("testdata/failures/unknownType.skill")
  @Test(expected = classOf[AssertionError]) def unkownFile = check("testdata/failures/unknownFile.skill")
  @Test(expected = classOf[AssertionError]) def empty = check("testdata/failures/empty.skill")
  @Test(expected = classOf[AssertionError]) def anyType = check("testdata/failures/anyType.skill")
}