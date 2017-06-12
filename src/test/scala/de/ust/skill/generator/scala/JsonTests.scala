/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

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
 * Generic tests built for scala.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * upfront would be ugly.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class JsonTests extends common.GenericJsonTests {

  override def language : String = "scala"

  override def deleteOutDir(out : String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/scala/src/main/scala/", out)).deleteRecursively
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.exit = s ⇒ throw new RuntimeException(s)
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-L", "scala",
      "-p", name,
      "-d", "testsuites/scala/lib",
      "-o", "testsuites/scala/src/main/scala") ++ options)
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    val f = new File(s"testsuites/scala/src/test/scala/$packagePath/Generic${name}Test.scala")
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""
package $packagePath

import java.nio.file.Path

import org.junit.Assert

import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.Create
import de.ust.skill.common.scala.api.SkillException
import de.ust.skill.common.scala.api.Read
import de.ust.skill.common.scala.api.ReadOnly
import de.ust.skill.common.scala.api.Write

import $packagePath.api.SkillFile
import common.CommonTest

""")

    for (path ← collectSkillSpecification(packagePath).getParentFile().listFiles if path.getName.endsWith(".json")) {
      makeTestForJson(rval, path.getAbsolutePath(), packagePath);
    }
    rval
  }

  
  def makeTestForJson(rval: PrintWriter, testfile: String, packagePath : String): PrintWriter = {
    def testname = new File(testfile).getName.replace(".json", "");
    rval.write(s"""
	test("${packagePath} - ${testname}") {
    val path = tmpFile("write.generic");
    val sf = SkillFile.open(path, Create, Write);
    reflectiveInit(sf);
    
    def types = creator.SkillObjectCreator.generateSkillFileTypeMappings(sf);
    def typeFieldMapping = creator.SkillObjectCreator.generateSkillFileFieldMappings(sf);
    
    //auto-generated instansiation from json

"""
      +
      generateObjectInstantiation(testfile)
      +

      """
	}

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

    val tmp = collectBinaries(name);
    val accept = tmp._1
    val reject = tmp._2

    // generate read tests
    locally {
      val out = newTestFile(name, "Read")

      for (f ← accept) out.write(s"""
  test("$name - read (accept): ${f.getName}") { read("${f.getPath.replaceAll("\\\\", "\\\\\\\\")}").check }
""")

      for (f ← reject) out.write(s"""
  test("$name - read (reject): ${f.getName}") { intercept[SkillException] { read("${f.getPath.replaceAll("\\\\", "\\\\\\\\")}").check } }
""")
      closeTestFile(out)
    }

    // reflective write tests
    locally {
      val out = newTestFile(name, "WriteReflective")

      out.write(s"""
  test("$name - write reflective") {
    val path = tmpFile("write.generic");
    val sf = SkillFile.open(path, Create, Write);
    reflectiveInit(sf);
    sf.close
  }

  test("$name - write reflective checked") {
    val path = tmpFile("write.generic.checked");
    val sf = SkillFile.open(path, Create, Write);
    reflectiveInit(sf);
    // write file
    sf.flush()

    // create a name -> type map
    val types : Map[String, Access[_]] = sf.map(t ⇒ t.name -> t).toMap

    // read file and check skill IDs
    val sf2 = SkillFile.open(path, Read, ReadOnly);
    for (t2 ← sf2) {
      val t = types(t2.name)
      if(t.size != t2.size)
        fail("size missmatch")
    }
  }
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
