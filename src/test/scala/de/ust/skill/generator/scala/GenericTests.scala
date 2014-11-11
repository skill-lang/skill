/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.file.Files
import scala.reflect.io.Directory
import scala.reflect.io.Path.jfile2path
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import de.ust.skill.main.CommandLine
import org.scalatest.junit.JUnitRunner

/**
 * Generic tests built for scala.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * upfront would be upgly.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GenericTests extends FunSuite {

  def newTestFile(packagePath : String, name : String) = {
    val f = new File(s"testsuites/scala/src/test/scala/$packagePath/$name.generated.scala")
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""
package $packagePath

import org.junit.Assert

import $packagePath.api.SkillState
import $packagePath.internal.ParseException
import $packagePath.internal.PoolSizeMissmatchError
import $packagePath.internal.TypeMissmatchError
import common.CommonTest

/**
 * Tests the file reading capabilities.
 */
class Generic${name}ReadTest extends CommonTest {
  @inline def read(s: String) = SkillState.read("../../"+s)
""")
    rval
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
}
""")
    out.close
  }

  def makeGenBinaryTests(name : String) {
    // find all relevant sf-files
    val base = new File("src/test/resources/genbinary")
    def collect(f : File) : Seq[File] =
      (for (path ← f.listFiles if path.isDirectory) yield collect(path)).flatten ++
        f.listFiles.filterNot(_.isDirectory)

    val targets = (
      collect(new File(base, "<all>"))
      ++ collect(if (new File(base, name).exists) new File(base, name) else new File(base, "<empty>"))
    ).filter(_.endsWith(".sf"))

    // generate read tests
    locally {
      val out = newTestFile(name, "Read")
      for (f ← targets) {
        if (f.getPath.contains("accept")) out.write(s"""
  test("$name - read (accept): ${f.getName}") { Assert.assertNotNull(read("${f.getPath}")) }
      """)
        else out.write(s"""
  test("$name - read (reject): ${f.getName}") { intercept[ParseException] { Assert.assertNotNull(read("${f.getPath}")) } }
      """)
      }
      closeTestFile(out)
    }

    //    mit generischem binding sf parsen um an zu erwartende daten zu kommen

    //    mit parser spec parsen um an lesbare daten zu kommen:)

    //    test generieren, der sicherstellt, dass sich die daten da raus lesen lassen

  }

  def check(path : File, out : String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/scala/src/main/scala/", out)).deleteRecursively

    CommandLine.main(Array[String]("-L", "scala", "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", out, path.getPath, "testsuites"))

    makeGenBinaryTests(out)
  }

  def makeTest(path : File, name : String) = test("generic: "+name)(check(path, name))

  implicit class Regex(sc : StringContext) {
    def r = new util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ ⇒ "x") : _*)
  }

  for (path ← new File("src/test/resources/gentest").listFiles if path.getName.endsWith(".skill")) {
    try {
      val r"""#!\s(\w+)${ name }""" = Files.lines(path.toPath).findFirst().orElse("")
      makeTest(path, name)
    } catch {
      case e : MatchError ⇒ // just continue, the first line did not match the command
    }
  }
}
