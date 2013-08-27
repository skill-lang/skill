package de.ust.skill.parser

import java.io.File
import org.scalatest._
import org.scalatest.junit.AssertionsForJUnit
import org.junit.Assert
import org.junit.Test
import scala.collection.JavaConversions._
import java.net.URL

class ParserTest extends AssertionsForJUnit {

  private def check(filename: String) = {
    val p = new Parser
    val url: URL = getClass.getResource(filename)
    assert(0 != p.parseAll(new File(url.getPath())).size)
  }

  @Test def hints: Unit = check("/hints.skill")
  @Test def restrictions: Unit = check("/restrictions.skill")

  @Test def test: Unit = check("/test.skill")
  @Test def test2: Unit = check("/test2.skill")
  @Test def test3: Unit = check("/test3.skill")
  @Test def test4: Unit = check("/test4.skill")
  @Test def example1: Unit = check("/example1.skill")
  @Test def example2a: Unit = check("/example2a.skill")
  @Test def example2b: Unit = check("/example2b.skill")
  @Test def unicode: Unit = check("/unicode.skill")
  @Test def airTop: Unit = check("/air-top.skill")
  @Test def airPamm: Unit = check("/air-pamm.skill")
  @Test def airHeap: Unit = check("/air-pamm-heap.skill")

  @Test def properIncludes: Unit = {
    val p = new Parser
    val url1: URL = getClass.getResource("/air-top.skill")
    val url2: URL = getClass.getResource("/air-pamm.skill")
    Assert.assertTrue(
      ASTEqualityChecker.checkDefinitionList(
        p.parseAll(new File(url1.getPath())).toList,
        p.parseAll(new File(url2.getPath())).toList))
  }

  @Test def process: Unit = {
    val p = new Parser
    val url1: URL = getClass.getResource("/air-top.skill")
    val url2: URL = getClass.getResource("/air-pamm.skill")
    Assert.assertArrayEquals(
      p.process(new File(url1.getPath())).map(_.toString()).toArray: Array[Object],
      p.process(new File(url2.getPath())).map(_.toString()).toArray: Array[Object])
  }

}