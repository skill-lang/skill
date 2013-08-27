package de.ust.skill.parser

import java.io.File
import java.lang.AssertionError
import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import java.net.URL

/**
 * Contains a lot of tests, which should not pass the type checker.
 * @author Timm Felden
 */
class TypeCheckerTest extends AssertionsForJUnit {
  private def check(filename: String) {
    val p = new Parser
    val url: URL = getClass.getResource(filename)
    p.parseAll(new File(url.getPath()))
  }

  @Test(expected = classOf[AssertionError]) def dupDefs = check("/failures/duplicateDefinition.skill")
  @Test(expected = classOf[AssertionError]) def dupField = check("/failures/duplicateField.skill")
  @Test(expected = classOf[AssertionError]) def halfFloat = check("/failures/halfFloat.skill")
  @Test(expected = classOf[AssertionError]) def constFloat = check("/failures/floatConstant.skill")
  @Test(expected = classOf[AssertionError]) def constSelf = check("/failures/selfConst.skill")
  @Test(expected = classOf[AssertionError]) def unkownType = check("/failures/unknownType.skill")
  @Test(expected = classOf[AssertionError]) def unkownFile = check("/failures/unknownFile.skill")
  @Test(expected = classOf[AssertionError]) def empty = check("/failures/empty.skill")
  @Test(expected = classOf[AssertionError]) def anyType = check("/failures/anyType.skill")
}