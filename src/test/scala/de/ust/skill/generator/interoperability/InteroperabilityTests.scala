/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.interoperability

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
import org.json.JSONTokener
import org.json.JSONObject
import org.json.JSONArray
import scala.collection.JavaConverters._

/**
 * Generic tests built for interoparability.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * upfront would be ugly.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class InteroperabilityTests extends common.GenericTests {

  override def language : String = "scala"

  override def deleteOutDir(out : String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/interoperability/src/main/scala/", out)).deleteRecursively
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.exit = s ⇒ throw new RuntimeException(s)
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-L", "scala",
      "-p", name,
      "-d", "testsuites/interoperability/lib",
      "-o", "testsuites/interoperability/src/main/scala") ++ options)
  }

  override
  def makeTests(packagePath : String) {
    val f = new File(s"testsuites/interoperability/src/test/scala/$packagePath/GenericInteroperabilityTest.generated.scala")
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""
package $packagePath

import scala.collection.mutable.HashSet
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration.Inf

import common.CommonTest
import de.ust.skill.common.scala.api.ReadOnly
import de.ust.skill.common.scala.api.Read
import java.io.File


import $packagePath.api.SkillFile


/**
 * Tests the file reading capabilities.
 */
class GenericInteroperabilityTest extends CommonTest {
  test("${packagePath} - JavaInteroperabilityTest") {
    
    val base = new File("src/test/resources/serializedTestfiles/orakel/${packagePath}");

    val files = collect(base)
      .filter(_.getName.endsWith(".sf"))
      .head;
    
    for(path <- files.getParentFile().listFiles){
      assert( compareFiles("src/test/resources/serializedTestfiles/java/${packagePath}/" + path.getName, "src/test/resources/serializedTestfiles/orakel/${packagePath}/" + path.getName));
    }
}

  test("${packagePath} - ScalaInteroperabilityest") {

    val base = new File("src/test/resources/serializedTestfiles/orakel/${packagePath}");

    val files = collect(base)
      .filter(_.getName.endsWith(".sf"))
      .head;
    
    for(path <- files.getParentFile().listFiles){
      assert( compareFiles("src/test/resources/serializedTestfiles/scala/${packagePath}/" + path.getName, "src/test/resources/serializedTestfiles/orakel/${packagePath}/" + path.getName));
    }
  }

  /**
   * comapre two files
   */
  def compareFiles(fileA : String, fileB : String): scala.Boolean = {
    
      implicit val context = global
            
      val asyncA = Future {
        SkillFile.open(fileA, Read, ReadOnly)
      }
      val asyncB = Future {
        SkillFile.open(fileB, Read, ReadOnly)
      }

      val a = Await.result(asyncA, Inf)
      val b = Await.result(asyncB, Inf)
      val typenames = (a ++ b).map(_.name).toSet
      for (name ← typenames.par) (a.find(_.name.equals(name)), b.find(_.name.equals(name))) match {
        case (scala.None, _)      ⇒ println(s"A does not contain type interoperability"); return false;
        case (_, scala.None)      ⇒ println(s"B does not contain type interoperability"); return false;
        case (Some(ta), Some(tb)) ⇒  if(!compareType(ta, tb)) {return false;}
      }
      return true;
  }
  
""")
    rval.close;
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
}
""")
    out.close
  }

  override def finalizeTests {
    // nothing yet
  }

}
