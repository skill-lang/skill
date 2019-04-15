/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import java.io._

import de.ust.skill.generator.common
import de.ust.skill.ir._
import de.ust.skill.main.CommandLine
import org.json.{JSONArray, JSONObject}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import scala.collection.JavaConversions.{asScalaBuffer, asScalaIterator}

/**
 * Generic API tests built for Java.
 *
 * @author Alexander Maisch
 */
@RunWith(classOf[JUnitRunner])
class APITests extends common.GenericAPITests {

  override val language = "python"

  val generator = new Main
  import generator._

  override def deleteOutDir(out : String) {
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.main(Array[String](
      source,
      "--debug-header",
      "-c",
      "-L", language,
      "-p", name,
      "-d", "testsuites",
      "-o", "testsuites/python/src/"
    ) ++ options)
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    val f = new File(s"testsuites/python/src/$packagePath/Generic${name}Test.py")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""

import os
from unittest import TestCase
from python.src.$packagePath.api import *
from python.src.common.CommonTest import CommonTest


class Generic${name}Test(TestCase, CommonTest):
    \"\"\"
    Tests the file reading capabilities.
    \"\"\"
""")
    rval
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
""")
    out.close()
  }

  def makeSkipTest(out : PrintWriter, kind : String, name : String, testName : String, accept : Boolean) {
    out.write(s"""
    def test_API_${escaped(kind)}_${name}_skipped_${escaped(testName)}(self):${
      if (accept) ""
      else """
         print("The test was skipped by the test generator.")"""
    }
    }
""")
  }

  def makeRegularTest(out : PrintWriter, kind : String, name : String, testName : String, accept : Boolean, tc : TypeContext, obj : JSONObject) {
    out.write(s"""
    def test_API_${escaped(kind)}_${name}_${if (accept) "acc" else "fail"}_${escaped(testName)}(self):
        file = self.tmpFile("$testName.sf")
        sf = SkillFile.open(file.name, Mode.Create, Mode.Write)
        try:
            # create objects${createObjects(obj, tc, name)}
            # set fields${setFields(obj, tc)}
            sf.close()

            # read back and assert correctness
            sf2 = SkillFile.open(sf.currentPath(), Mode.Read, Mode.ReadOnly)
            # check count per Type${createCountChecks(obj, tc, name)}
            # create objects from file${createObjects2(obj, tc, name)}
            # assert fields${assertFields(obj, tc)}
            # close file
            sf2.close()
        finally:
            # delete files
            os.remove(sf.currentPath())
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

  private def equalValue(left : String, v : Any, f : Field) : String = equalValue(left, v, f.getType)

  private def equalValue(left : String, v : Any, t : Type) : String = t match {
    case t : GroundType ⇒
      t.getSkillName match {
        case "string" if null != v ⇒ s"""$left is not None and $left, "${v.toString}""""
        case "i8" | "i16" | "i32" | "v64" | "i64" | "f32" | "f64" ⇒ s"$left, " + v.toString
        case _ if null != v && !v.toString.equals("null") && !v.toString.equals("true")
            && !v.toString.equals("false") ⇒ s"$left, " + v.toString + "_2"
        case "true" ⇒  s"$left, " + "True"
        case "false" ⇒ s"$left, " + "False"
        case "null" ⇒ s"$left, " + "None"
        case _ ⇒ s"$left, " + v.toString
      }

    case t : SetType ⇒ v.asInstanceOf[JSONArray].iterator().toArray.map(value(_, t.getBaseType)).mkString(s"$left is not None and $left, set(", ", ", ")")

    case t : ListType ⇒ v.asInstanceOf[JSONArray].iterator().toArray.map(value(_, t.getBaseType)).mkString(s"$left is not None and $left, [", ", ", "]")

    case t : MapType if v != null ⇒ s"$left is not None and $left, " + valueMap(v.asInstanceOf[JSONObject], "_2")

    case _ ⇒
      if (v == null || v.toString.equals("None")) s"$left is None"
      else s"$left, ${v.toString}_2"
  }

  //private def value(v : Any, f : Field, suffix : String = "") : String = value(v, f.getType, suffix)

  private def value(v : Any, t : Type, suffix : String = "") : String = t match {
    case t : GroundType ⇒
      t.getSkillName match {
        case "string" if null != v ⇒ s""""${v.toString}""""
        case "i8" | "i16" | "i32" | "v64" | "i64" ⇒ v.toString
        case "f32" | "f64" ⇒ if (!v.toString.contains(".")){v.toString + ".0"} else {v.toString}
        case _ if null != v        ⇒ v.toString + suffix
        case _                     ⇒ v.toString
      }

    case t : SetType ⇒ v.asInstanceOf[JSONArray].iterator().toArray.map(value(_, t.getBaseType, suffix)).mkString("{", ", ", "}") //TODO

    case t : ListType ⇒ v.asInstanceOf[JSONArray].iterator().toArray.map(value(_, t.getBaseType, suffix)).mkString("[", ", ", "]")

    case t : MapType if v != null ⇒ valueMap(v.asInstanceOf[JSONObject], suffix)

    case _ ⇒
      if (v == null || v.toString.equals("None")) s"None"
      else v.toString + suffix
  }

  private def valueMap(v: Any, suffix: String = "") = {
      var rval = s"dict()"
      val obj = v.asInstanceOf[JSONObject]

      // https://docs.scala-lang.org/overviews/collections/maps.html#operations-in-class-map
      // ms put (k, v) Adds mapping from key k to value v to ms and returns any value previously associated with k as an option.
      for (name ← JSONObject.getNames(obj)) {
        rval = s"self.put($rval, '${value(name, null, suffix)}', $suffix)"
      }

      rval
  }

  private def createCountChecks(obj : JSONObject, tc : TypeContext, packagePath : String) : String = {
    if (null == JSONObject.getNames(obj)) {
      ""
    } else {

      val objCountPerType = scala.collection.mutable.Map[String, Int]()
      for (name ← JSONObject.getNames(obj)) {
        val x = obj.getJSONObject(name)
        val t = JSONObject.getNames(x).head

        val typeName = typ(tc, t)
        if (!(objCountPerType contains typeName)) {
          objCountPerType += (typeName -> 0)
        }
        objCountPerType(typeName) = objCountPerType(typeName) + 1
      }

      val rval = for ((typeName, objCount) ← objCountPerType) yield {
        s"""
            self.assertEqual($objCount, sf.$typeName.staticSize())"""
      }

      rval.mkString
    }
  }

  private def createObjects2(obj : JSONObject, tc : TypeContext, packagePath : String) : String = {
    if (null == JSONObject.getNames(obj)) {
      ""
    } else {

      val rval = for (name ← JSONObject.getNames(obj)) yield {
        val x = obj.getJSONObject(name)
        val t = JSONObject.getNames(x).head

        val typeName = typ(tc, t)

        s"""
            ${name}_2 = sf2.$typeName.getByID($name.skillID)"""
      }

      rval.mkString
    }
  }

  private def createObjects(obj : JSONObject, tc : TypeContext, packagePath : String) : String = {
    if (null == JSONObject.getNames(obj)) {
      ""
    } else {

      val rval = for (name ← JSONObject.getNames(obj)) yield {
        val x = obj.getJSONObject(name)
        val t = JSONObject.getNames(x).head

        val typeName = typ(tc, t)

        s"""
            $name = sf.$typeName.make()"""
      }

      rval.mkString
    }
  }

  private def assertFields(obj : JSONObject, tc : TypeContext) : String = {
    if (null == JSONObject.getNames(obj)) {
      ""
    } else {

      val rval = for (name ← JSONObject.getNames(obj)) yield {
        val x = obj.getJSONObject(name)
        val t = JSONObject.getNames(x).head
        val fs = x.getJSONObject(t)

        if (null == JSONObject.getNames(fs))
          ""
        else {
          val assignments = for (fieldName ← JSONObject.getNames(fs).toSeq) yield {
            val f = field(tc, t, fieldName)
            val getter = "get" + escaped(f.getName.capital()) + "()"

            // do not check auto fields as they cannot obtain the stored value from file
            if (f.isAuto) ""
            else {
                s"""
            self.assertEqual(${equalValue(s"${name}_2.$getter", fs.get(fieldName), f)})"""
            }}

          assignments.mkString
        }
      }

      rval.mkString("\n")
    }
  }

  private def setFields(obj : JSONObject, tc : TypeContext) : String = {
    if (null == JSONObject.getNames(obj)) {
      ""
    } else {

      val rval = for (name ← JSONObject.getNames(obj)) yield {
        val x = obj.getJSONObject(name)
        val t = JSONObject.getNames(x).head
        val fs = x.getJSONObject(t)

        if (null == JSONObject.getNames(fs))
          ""
        else {
          val assignments = for (fieldName ← JSONObject.getNames(fs).toSeq) yield {
            val f = field(tc, t, fieldName)
            val setter = "set" + escaped(f.getName.capital())

            s"""
            $name.$setter(${if (value(fs.get(fieldName), f.getType) == "null") {"None"} else {value(fs.get(fieldName), f.getType)}})"""
          }

          assignments.mkString
        }
      }

      rval.mkString("\n")
    }
  }
}
