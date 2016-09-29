/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.haskell

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import de.ust.skill.main.CommandLine

import scala.sys.process._
import java.io.File

@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  test("make tests") {
    // ensure testsuite folder
    val testsuite = new File("testsuites/haskell");
    testsuite.mkdirs()
    testsuite.mkdir()
    
    // compile generator
    print(Process(
      Seq("ghc", "-e", """generate "src/test/resources/genbinary/subtypes/accept/" "testsuites/haskell/"""", "src/main/resources/haskell/TestGenerator.hs")
    ).lineStream.mkString("\n"))
    
    // copy haskell library
    print(Process(
      Seq("bash", "-c", """cp src/main/resources/haskell/*.hs testsuites/haskell""")
    ).lineStream.mkString("\n"))

    // build tests
    print(Process(
      Seq("ghc", "RunTests.hs"),
      Some(testsuite)
    ).lineStream.mkString("\n"))
    
    // run tests
    print(Process(
      Seq("testsuites/haskell/RunTests")
    ).lineStream.mkString("\n"))
  }
}
