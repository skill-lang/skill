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

}