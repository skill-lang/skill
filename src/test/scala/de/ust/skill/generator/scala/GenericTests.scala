/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
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
import scala.collection.mutable.ArrayBuffer
import de.ust.skill.generator.common

/**
 * Generic tests built for scala.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * upfront would be upgly.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GenericTests extends common.GenericTests {

  override def language = "scala"

  override def deleteOutDir(out : String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/scala/src/main/scala/", out)).deleteRecursively
  }

  def newTestFile(packagePath : String, name : String) = {
    val f = new File(s"testsuites/scala/src/test/scala/$packagePath/$name.generated.scala")
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""
package $packagePath

import org.junit.Assert

import $packagePath.api.SkillFile
import $packagePath.internal.ParseException
import $packagePath.internal.PoolSizeMissmatchError
import $packagePath.internal.TypeMissmatchError
import common.CommonTest

/**
 * Tests the file reading capabilities.
 */
class Generic${name}ReadTest extends CommonTest {
  @inline def read(s: String) = SkillFile.open("../../"+s)
""")
    rval
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
}
""")
    out.close
  }

  override def makeGenBinaryTests(name : String) {
    val (accept, reject) = collectBinaries(name)

    // generate read tests
    locally {
      val out = newTestFile(name, "Read")

      for (f ← accept) out.write(s"""
  test("$name - read (accept): ${f.getName}") { Assert.assertNotNull(read("${f.getPath}")) }
""")

      for (f ← reject) out.write(s"""
  test("$name - read (reject): ${f.getName}") { intercept[ParseException] { Assert.assertNotNull(read("${f.getPath}")) } }
""")
      closeTestFile(out)
    }

    //    mit generischem binding sf parsen um an zu erwartende daten zu kommen

    //    mit parser spec parsen um an lesbare daten zu kommen:)

    //    test generieren, der sicherstellt, dass sich die daten da raus lesen lassen

  }

  override def finalizeTests {
    // nothing yet
  }
}
