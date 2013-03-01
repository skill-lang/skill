package de.ust.skill.parser

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Assert._
import org.junit.Test
import org.junit.Before
import java.io.File

class ParserTest extends AssertionsForJUnit {

  @Test def test: Unit = {
    val p = new Parser
    assertNotSame(0, p.process(new File("test/data/test.skill")).size)
  }
  @Test def hints: Unit = {
    val p = new Parser
    assertNotSame(0, p.process(new File("test/data/hints.skill")).size)
  }
  @Test def test2: Unit = {
    val p = new Parser
    assertNotSame(0, p.process(new File("test/data/test2.skill")).size)
  }
  @Test def test3: Unit = {
    val p = new Parser
    assertNotSame(0, p.process(new File("test/data/test3.skill")).size)
  }
  @Test def example1: Unit = {
    val p = new Parser
    assertNotSame(0, p.process(new File("test/data/example1.skill")).size)
  }
  @Test def example2a: Unit = {
    val p = new Parser
    assertNotSame(0, p.process(new File("test/data/example2a.skill")).size)
  }
  @Test def example2b: Unit = {
    val p = new Parser
    p.process(new File("test/data/example2b.skill"))
  }
  @Test def unicode: Unit = {
    val p = new Parser
    p.process(new File("test/data/unicode.skill"))
  }
  @Test def airTop: Unit = {
    val p = new Parser
    p.process(new File("test/data/air-top.skill"))
  }
  @Test def airPamm: Unit = {
    val p = new Parser
    p.process(new File("test/data/air-pamm.skill"))
  }
  @Test def airHeap: Unit = {
    val p = new Parser
    p.process(new File("test/data/air-pamm-heap.skill"))
  }

}