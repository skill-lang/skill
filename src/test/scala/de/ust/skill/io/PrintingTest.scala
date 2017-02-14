package de.ust.skill.io

import java.io.File
import java.io.IOException

import scala.io.Source

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterEach
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Tests intended behavior of code printing.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class PrintingTest extends FunSuite with BeforeAndAfterEach {

  var tmpDir : File = _;

  override def beforeEach {
    tmpDir = new File("tmp-printing-test")
    tmpDir.mkdirs()
  }

  override def afterEach {
    def deleteRecursively(file : File) : Unit = {
      if (file.isDirectory)
        file.listFiles.foreach(deleteRecursively)
      if (file.exists && !file.delete)
        throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
    }
    deleteRecursively(tmpDir)
  }

  private def exists(path : String) {
    assert(new File(tmpDir, path).exists())
  }
  private def existsNot(path : String) {
    assert(!new File(tmpDir, path).exists())
  }
  private def contains(path : String, expected : String) {
    try {
      assert(Source.fromFile(new File(tmpDir, path)).getLines().mkString("\n") === expected)
    } catch {
      case e : IOException ⇒ fail(e.getMessage)
    }
  }

  test("create file") {
    val ps = new PrintingService(tmpDir, "head\n")
    val f = ps.open("f")
    f.write("body")
    existsNot("f")
    f.close()
    contains("f", """head
body""")
  }

  test("overwrite file") {
    val ps = new PrintingService(tmpDir, "head\n")
    val f = ps.open("f")
    f.write("body")
    existsNot("f")
    assert(null != f.checkPreexistingFile)
    f.close()
    contains("f", """head
body""")
    val g = ps.open("f")
    g.write("body")
    assert(null == g.checkPreexistingFile)
  }

  test("delete files") {
    val ps = new PrintingService(tmpDir, "head\n")
    val f = ps.open("f")
    f.write("body")
    f.close()
    exists("f")

    new File(tmpDir, "g").createNewFile()
    new File(tmpDir, "dir").mkdirs()
    exists("g")
    ps.deleteForeignFiles(false)
    exists("f")
    exists("dir")
    existsNot("g")
  }

  test("delete dirs") {
    val ps = new PrintingService(tmpDir, "head\n")
    val f = ps.open("f")
    f.write("body")
    f.close()
    exists("f")

    new File(tmpDir, "g").createNewFile()
    new File(tmpDir, "dir").mkdirs()
    exists("g")
    ps.deleteForeignFiles(true)
    exists("f")
    existsNot("dir")
    existsNot("g")
  }

  test("delete inner dirs") {
    val ps = new PrintingService(tmpDir, "head\n")
    val f = ps.open("inner/f")
    f.write("body")
    f.close()
    exists("inner/f")

    new File(tmpDir, "g").createNewFile()
    new File(tmpDir, "dir").mkdirs()
    exists("g")
    ps.deleteForeignFiles(true)
    exists("inner/f")
    exists("dir")
    exists("g")
  }

  test("wipe dirs") {
    val ps = new PrintingService(tmpDir, "head\n")
    val f = ps.open("inner/f")
    f.write("body")
    f.close()
    exists("inner/f")

    new File(tmpDir, "g").createNewFile()
    new File(tmpDir, "dir").mkdirs()
    exists("g")
    ps.wipeOutPath()
    exists("inner/f")
    existsNot("dir")
    existsNot("g")
  }
}