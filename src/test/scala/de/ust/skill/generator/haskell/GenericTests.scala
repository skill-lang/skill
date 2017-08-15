/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.haskell

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

import scala.reflect.io.Path.jfile2path

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import de.ust.skill.generator.common
import de.ust.skill.main.CommandLine

/**
 * Generic tests built for Haskell.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GenericTests extends common.GenericTests {

  override def language : String = "haskell"

  override def deleteOutDir(out : String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/haskell/src/", out)).deleteRecursively
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-L", language,
      "-p", name,
      "-o", "testsuites/haskell/" + name) ++ options)
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    val f = new File(s"testsuites/haskell/$packagePath/runGenericReadTests.hs")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""import Test.HUnit
import Test.Framework
import Test.Framework.Providers.HUnit
import Test.Framework.Runners.Console
import Deserialize
import Data.IORef
import Methods

main = defaultMain $$ [(testGroup "$packagePath.$name") $$ hUnitTestToTests $$ TestList """)
    rval
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("]")
    out.close
  }

  override def makeTests(name : String) {
    val (accept, reject) = collectBinaries(name)

    // generate read tests
    locally {
      val out = newTestFile(name, "Read")

      out.write(
        (for (f ← accept if f.length < 1000) yield s"""
  TestLabel "accept ${f.getName}" (TestCase $$ printTestName "accept ${f.getName}" >> initializeTest "../../../${f.getPath}")"""
        ).mkString("[", ",", "]")
      )

      // TODO reject tests

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
