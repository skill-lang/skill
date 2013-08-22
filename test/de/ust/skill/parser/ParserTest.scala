package de.ust.skill.parser

import java.io.File
import org.scalatest._
import org.scalatest.junit.AssertionsForJUnit
import org.junit.Assert
import org.junit.Test
import scala.collection.JavaConversions._

class ParserTest extends AssertionsForJUnit {

  private def check(path: String) = {
    val p = new Parser
    assert(0 != p.parseAll(new File(path)).size)
  }

  @Test def hints: Unit = check("testdata/hints.skill")
  @Test def restrictions: Unit = check("testdata/restrictions.skill")

  @Test def test: Unit = check("testdata/test.skill")
  @Test def test2: Unit = check("testdata/test2.skill")
  @Test def test3: Unit = check("testdata/test3.skill")
  @Test def test4: Unit = check("testdata/test4.skill")
  @Test def example1: Unit = check("testdata/example1.skill")
  @Test def example2a: Unit = check("testdata/example2a.skill")
  @Test def example2b: Unit = check("testdata/example2b.skill")
  @Test def unicode: Unit = check("testdata/unicode.skill")
  @Test def airTop: Unit = check("testdata/air-top.skill")
  @Test def airPamm: Unit = check("testdata/air-pamm.skill")
  @Test def airHeap: Unit = check("testdata/air-pamm-heap.skill")

  @Test def properIncludes: Unit = {
    val p = new Parser
    Assert.assertTrue(
      ASTEqualityChecker.checkDefinitionList(
        p.parseAll(new File("testdata/air-top.skill")).toList,
        p.parseAll(new File("testdata/air-pamm.skill")).toList))
  }

  @Test def process: Unit = {
    val p = new Parser
    Assert.assertArrayEquals(
      p.process(new File("testdata/air-top.skill")).map(_.toString()).toArray: Array[Object],
      p.process(new File("testdata/air-pamm.skill")).map(_.toString()).toArray: Array[Object])
  }

}