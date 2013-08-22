package de.ust.skill.parser

import java.io.File

import scala.sys.process._

import org.junit.Assert
import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import scala.language.postfixOps

class ParserPrettyPrintingProjectionTest extends AssertionsForJUnit {

  def check(path: String) = {
    val p = new Parser
    val first = p.parseAll(new File(path)).toList
    val tmp = File.createTempFile("test", "skill")
    ("echo "+ASTPrettyPrinter.prettyPrint(first)) #> tmp !
    val second = p.parseAll(tmp).toList
    tmp.delete()
    ("echo "+ASTPrettyPrinter.prettyPrint(first)) #> tmp !
    val third = p.parseAll(tmp).toList
    tmp.delete()
    Assert.assertTrue(ASTEqualityChecker.checkDefinitionList(third, second))
  }

  @Test def test: Unit = check("testdata/test.skill")

  @Test def hints: Unit = check("testdata/hints.skill")
  @Test def test2: Unit = check("testdata/test2.skill")
  @Test def test3: Unit = check("testdata/test3.skill")
  @Test def example1: Unit = check("testdata/example1.skill")
  @Test def example2a: Unit = check("testdata/example2a.skill")
  @Test def example2b: Unit = check("testdata/example2b.skill")
  @Test def unicode: Unit = check("testdata/unicode.skill")
  @Test def airTop: Unit = check("testdata/air-top.skill")
  @Test def airPamm: Unit = check("testdata/air-pamm.skill")
  @Test def airHeap: Unit = check("testdata/air-pamm-heap.skill")

}