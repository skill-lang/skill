package de.ust.skill.parser

import java.io.File
import scala.sys.process._
import org.junit.Assert
import org.junit.Test
import org.scalatest.junit.AssertionsForJUnit
import scala.language.postfixOps
import java.net.URL

class ParserPrettyPrintingProjectionTest extends AssertionsForJUnit {

  def check(filename: String) = {
    val p = new Parser
    val url: URL = getClass.getResource(filename)
    val first = p.parseAll(new File(url.getPath())).toList
    val tmp = File.createTempFile("test", "skill")
    ("echo "+ASTPrettyPrinter.prettyPrint(first)) #> tmp !
    val second = p.parseAll(tmp).toList
    tmp.delete()
    ("echo "+ASTPrettyPrinter.prettyPrint(first)) #> tmp !
    val third = p.parseAll(tmp).toList
    tmp.delete()
    Assert.assertTrue(ASTEqualityChecker.checkDefinitionList(third, second))
  }

  @Test def test: Unit = check("/test.skill")

  @Test def hints: Unit = check("/hints.skill")
  @Test def test2: Unit = check("/test2.skill")
  @Test def test3: Unit = check("/test3.skill")
  @Test def example1: Unit = check("/example1.skill")
  @Test def example2a: Unit = check("/example2a.skill")
  @Test def example2b: Unit = check("/example2b.skill")
  @Test def unicode: Unit = check("/unicode.skill")
  @Test def airTop: Unit = check("/air-top.skill")
  @Test def airPamm: Unit = check("/air-pamm.skill")
  @Test def airHeap: Unit = check("/air-pamm-heap.skill")

}