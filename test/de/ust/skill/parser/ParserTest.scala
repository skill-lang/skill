package de.ust.skill.parser

import java.io.File

import org.junit.Assert
import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit

class ParserTest extends AssertionsForJUnit {

  def check(path: String) = {
    val p = new Parser
    Assert.assertNotSame(0, p.process(new File(path)).size)
  }

  @Test def test: Unit = check("test/data/test.skill")

  @Test def hints: Unit = check("test/data/hints.skill")
  @Test def test2: Unit = check("test/data/test2.skill")
  @Test def test3: Unit = check("test/data/test3.skill")
  @Test def example1: Unit = check("test/data/example1.skill")
  @Test def example2a: Unit = check("test/data/example2a.skill")
  @Test def example2b: Unit = check("test/data/example2b.skill")
  @Test def unicode: Unit = check("test/data/unicode.skill")
  @Test def airTop: Unit = check("test/data/air-top.skill")
  @Test def airPamm: Unit = check("test/data/air-pamm.skill")
  @Test def airHeap: Unit = check("test/data/air-pamm-heap.skill")

  @Test def properIncludes: Unit = {
    val p = new Parser
    Assert.assertTrue(
      ASTEqualityChecker.checkDefinitionList(
        p.process(new File("test/data/air-top.skill")).toList,
        p.process(new File("test/data/air-pamm.skill")).toList))
  }

}