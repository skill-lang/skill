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

  override def language: String = "scala"

  override def deleteOutDir(out: String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/interoperability/src/main/scala/", out)).deleteRecursively
  }

  override def callMainFor(name: String, source: String, options: Seq[String]) {
    CommandLine.exit = s ⇒ throw new RuntimeException(s)
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-L", "scala",
      "-p", name,
      "-d", "testsuites/interoperability/lib",
      "-o", "testsuites/interoperability/src/main/scala") ++ options)
  }

  def newTestFile(packagePath: String, name: String): PrintWriter = {
    val f = new File(s"testsuites/interoperability/src/test/scala/$packagePath/Generic${name}Test.generated.scala")
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""
package $packagePath

mport scala.collection.mutable.HashSet
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration.Inf

import de.ust.skill.common.scala.api.Access
import de.ust.skill.common.scala.api.Read
import de.ust.skill.common.scala.api.ReadOnly
import de.ust.skill.common.scala.api.SkillObject
import de.ust.skill.common.scala.internal.fieldTypes.MapType
import de.ust.skill.common.scala.internal.fieldTypes.SetType
import de.ust.skill.common.scala.internal.fieldTypes.SingleBaseTypeContainer


import $packagePath.api.SkillFile


/**
 * Tests the file reading capabilities.
 */
class Generic${name}Test extends CommonTest {
  test("${packagePath} - Java${name}Test") {
    compareFiles("src/test/resources/serializedTestfiles/java/${packagePath}/blub.sf",
                              "src/test/resources/serializedTestfiles/orakel/${packagePath}/blub.sf");
  }

  test("${packagePath} - Scala${name}Test") {
    compareFiles("src/test/resources/serializedTestfiles/scala/${packagePath}/blub.sf",
                              "src/test/resources/serializedTestfiles/orakel/${packagePath}/blub.sf");
  }

  /**
   * comapre two files
   */
  def compareFiles(fileA : String, fileB : String) {
    
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
        case (None, _)            ⇒ println(s"A does not contain type $$name")
        case (_, None)            ⇒ println(s"B does not contain type $$name")
        case (Some(ta), Some(tb)) ⇒ compareType(ta, tb)
      }
  }

  /**
   * compare two types
   */
  def compareType(a : Access[_ <: SkillObject], b : Access[_ <: SkillObject]) {

    val fas = a.fields

    // they have no meaningful data
    if ((fas.isEmpty && b.fields.isEmpty) || a.isEmpty || b.isEmpty) {
      if (a.size > b.size) {
        println(s"A.$$a has $${a.size - b.size} more instances");
      } else if (a.size < b.size) {
        println(s"B.$$b has $${b.size - a.size} more instances");
      }

      return
    }

    if (!a.fields.map(_.name).toSeq.sorted.sameElements(b.fields.map(_.name).toSeq.sorted)) {
      println(s"Type $$a has different fields")
    }

    while (fas.hasNext) {
      val fa = fas.next
      val fb = b.fields.find(_.name.equals(fa.name)).get

      val as = a.all
      val bs = b.all

      while (as.hasNext && bs.hasNext) {
        val xa = as.next
        val xb = bs.next

        fa.t match {
          case t : SetType[_] ⇒
            locally {
              val (left, right) = (xa.get(fa).asInstanceOf[HashSet[_]], xb.get(fb).asInstanceOf[HashSet[_]])
              if (compare(left, right) && !left.toArray.map(_.toString).sorted.sameElements(right.toArray.map(_.toString).sorted)) {
                println(s"A.$$xa.$$fa != B.$$xb.$$fb")
                println(s"  $${xa.get(fa)} <-> $${xb.get(fb)}")
              }
            }

          case t : SingleBaseTypeContainer[_, _] ⇒
            locally {
              var i = 0
              val (left, right) = (xa.get(fa).asInstanceOf[Iterable[_]], xb.get(fb).asInstanceOf[Iterable[_]])
              if (compare(left, right) && !left.map(_.toString).sameElements(right.map(_.toString))) {
                println(s"A.$$xa.$$fa != B.$$xb.$$fb")
                println(s"  $${xa.get(fa)} <-> $${xb.get(fb)}")
              }
            }
          case t : MapType[_, _] ⇒

          case _ ⇒
            if (compare(xa.get(fa), xb.get(fb))) {
              println(s"A.$$xa.$$fa != B.$$xb.$$fb")
              println(s"  $${xa.get(fa)} <-> $${xb.get(fb)}")
            }
        }
      }
    }

    if (a.size > b.size) {
      println(s"A.$$a has $${a.size - b.size} more instances");
    } else if (a.size < b.size) {
      println(s"B.$$b has $${b.size - a.size} more instances");
    }
  }

  @inline
  def compare(x : Any, y : Any) = x != y && x != null && y != null && x.toString() != y.toString()
""")
return rval;
  }

  def closeTestFile(out: java.io.PrintWriter) {
    out.write("""
}
""")
    out.close
  }

  override def makeGenBinaryTests(name: String) {
    // generate read tests
    locally {
      val out = newTestFile(name, "interoperability")
      closeTestFile(out)
    }

  }

  override def finalizeTests {
    // nothing yet
  }

}
