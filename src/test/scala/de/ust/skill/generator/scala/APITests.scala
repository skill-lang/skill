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

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.asScalaIterator
import scala.io.Source

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import de.ust.skill.generator.common
import de.ust.skill.ir.Field
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.SingleBaseTypeContainer
import de.ust.skill.ir.Type
import de.ust.skill.ir.TypeContext
import de.ust.skill.main.CommandLine

/**
 * Generic API tests built for Java.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class APITests extends common.GenericAPITests {

  override val language = "scala"

  val generator = new Main
  import generator._

  override def deleteOutDir(out : String) {
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-L", "scala",
      "-p", name,
      "-d", "testsuites/scala/lib",
      "-o", "testsuites/scala/src/main/scala") ++ options)
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    generator.setPackage(List(packagePath))

    val f = new File(s"testsuites/scala/src/test/scala/$packagePath/APITest.generated.scala")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""package $packagePath

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

/**
 * Tests the file reading capabilities.
 */
class GenericAPITest extends CommonTest {
""")
    rval
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
}
""")
    out.close
  }

  def makeSkipTest(out : PrintWriter, kind : String, name : String, testName : String, accept : Boolean) {
    out.write(s"""
    test("API test - $kind $name skipped : ${testName}") {${
      if (accept) ""
      else """
         fail("The test was skipped by the test generator.");"""
    }
    }
""")
  }

  def makeRegularTest(out : PrintWriter, kind : String, name : String, testName : String, accept : Boolean, tc : TypeContext, obj : JSONObject) {
    out.write(s"""
    test("API test - $kind $name ${if (accept) "acc" else "fail"} : ${testName}") (${
      if (accept) ""
      else "try"
    }{
        val sf = SkillFile.open(tmpFile("$testName.sf"), Create, Write);

        // create objects${createObjects(obj, tc, name)}
        
        // set fields${setFields(obj, tc)}

        sf.close${
      if (accept) ""
      else """

        fail("expected failure, but nothing happended")
    } catch {
      case e : Exception ⇒"""
    }
    })
""")
  }

  private def typ(tc : TypeContext, name : String) : String = {
    val n = name.toLowerCase()
    try {
      escaped((tc.getUsertypes ++ tc.getInterfaces).filter(_.getSkillName.equals(n)).head.getName.capital())
    } catch {
      case e : NoSuchElementException ⇒ fail(s"Type '$n' does not exist, fix your test description!")
    }
  }

  private def field(tc : TypeContext, typ : String, field : String) = {
    val tn = typ.toLowerCase()
    val t = tc.getUsertypes.find(_.getSkillName.equals(tn)).get
    val fn = field.toLowerCase()
    try {
      t.getAllFields.find(_.getSkillName.equals(fn)).get
    } catch {
      case e : NoSuchElementException ⇒ fail(s"Field '$fn' does not exist, fix your test description!")
    }
  }

  private def value(v : Any, f : Field) : String = value(v, f.getType)

  private def value(v : Any, t : Type) : String = t match {
    case t : GroundType ⇒
      t.getSkillName match {
        case "string"      ⇒ s""""${v.toString()}""""
        case "i8"          ⇒ v.toString() + ".toByte"
        case "i16"         ⇒ v.toString() + ".toShort"
        case "f32"         ⇒ v.toString() + ".toFloat"
        case "f64"         ⇒ v.toString()
        case "v64" | "i64" ⇒ v.toString() + "L"
        case _             ⇒ v.toString()
      }

    case t : SingleBaseTypeContainer ⇒
      v.asInstanceOf[JSONArray].iterator().toArray.map(value(_, t.getBaseType)).mkString(t match {
        case t : ListType ⇒ "list("
        case t : SetType  ⇒ "set("
        case _            ⇒ "array("
      }, ", ", ")").replace("java.util.", "")

    case t : MapType if v != null ⇒ valueMap(v.asInstanceOf[JSONObject], t.getBaseTypes.toList)

    case _                        ⇒ v.toString()
  }

  private def valueMap(v : Any, ts : List[Type]) : String = {
    if (1 == ts.length) {
      value(v, ts.head)
    } else {
      var rval = s"map"
      val obj = v.asInstanceOf[JSONObject]

      for (name ← JSONObject.getNames(obj)) {
        rval = s"put($rval, ${value(name, ts.head)}, ${valueMap(obj.get(name), ts.tail)})"
      }

      rval;
    }
  }

  private def createObjects(obj : JSONObject, tc : TypeContext, packagePath : String) : String = {
    if (null == JSONObject.getNames(obj)) {
      ""
    } else {

      val rval = for (name ← JSONObject.getNames(obj)) yield {
        val x = obj.getJSONObject(name)
        val t = JSONObject.getNames(x).head;

        val typeName = typ(tc, t);

        s"""
        val $name = sf.${typeName}.reflectiveAllocateInstance;"""
      }

      rval.mkString
    }
  }

  private def setFields(obj : JSONObject, tc : TypeContext) : String = {
    if (null == JSONObject.getNames(obj)) {
      ""
    } else {

      val rval = for (name ← JSONObject.getNames(obj)) yield {
        val x = obj.getJSONObject(name)
        val t = JSONObject.getNames(x).head;
        val fs = x.getJSONObject(t);

        if (null == JSONObject.getNames(fs))
          ""
        else {
          val assignments = for (fieldName ← JSONObject.getNames(fs).toSeq) yield {
            val f = field(tc, t, fieldName)
            val setter = escaped(f.getName.camel())
            s"""
        $name.$setter = ${value(fs.get(fieldName), f)};"""
          }

          assignments.mkString
        }
      }

      rval.mkString("\n")
    }
  }
}
