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
    p.process(new File(path))
  }

  @Test(expected = classOf[AssertionError]) def dupDefs = check("test/data/failures/duplicateDefinition.skill")
  @Test(expected = classOf[AssertionError]) def dupField = check("test/data/failures/duplicateField.skill")
  @Test(expected = classOf[AssertionError]) def missingField = check("test/data/failures/missingLengthField.skill")
  @Test(expected = classOf[AssertionError]) def halfFloat = check("test/data/failures/halfFloat.skill")
  @Test(expected = classOf[AssertionError]) def constFloat = check("test/data/failures/floatConstant.skill")
  @Test(expected = classOf[AssertionError]) def constSelf = check("test/data/failures/selfConst.skill")
  @Test(expected = classOf[AssertionError]) def unkownType = check("test/data/failures/unknownType.skill")
  @Test(expected = classOf[AssertionError]) def unkownFile = check("test/data/failures/unknownFile.skill")
  @Test(expected = classOf[AssertionError]) def empty = check("test/data/failures/empty.skill")
}