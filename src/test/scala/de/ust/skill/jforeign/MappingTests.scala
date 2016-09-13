/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign

import java.io.File

import scala.collection.mutable.ArrayBuffer
import scala.reflect.io.Path.jfile2path

import org.scalatest.BeforeAndAfterAll
import org.scalatest.ConfigMap
import org.scalatest.FunSuite

import de.ust.skill.main.CommandLine

class MappingTests extends FunSuite with BeforeAndAfterAll {

  val testOnly = ""

  def language: String = "javaforeign"

  def languageOptions: ArrayBuffer[String] = ArrayBuffer()

  def finalizeTests(): Unit = {}

  final def makeTest(path: File, name: String, mappingFile: File, skillFilePath: String): Unit = {
    CommandLine.exit = { s ⇒ throw (new Error(s)) }

    val args = languageOptions ++ ArrayBuffer[String]("-L", language, "-u", "<<some developer>>", "-h2", "<<debug>>", "-p", name + "skill")
    args += "-M"
    args += mappingFile.getPath
    args += "-F"
    args += path.getAbsolutePath
    args += skillFilePath
    args += "testsuites"
    CommandLine.main(args.toArray)
  }

  
  def mappingTest(file : File, succeed : Boolean) = test(s"mapping test ${file.getPath}") {
    try {
      makeTest(new File("src/test/resources/javaForeign/mapping"), "simple", file,
          "src/test/resources/javaForeign/mapping/simple.skill")
      assert(succeed)
    } catch {
      case e: Exception ⇒
        println(e.toString())
        assert(!succeed)
    }
  }
  
  for (path ← new File("src/test/resources/javaForeign/mapping/succeed").listFiles()) if (path.isFile()) mappingTest(path, true)

}
