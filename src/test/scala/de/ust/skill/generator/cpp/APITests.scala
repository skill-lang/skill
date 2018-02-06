/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

import scala.collection.JavaConverters._
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
 * Generic API tests built for C++.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class APITests extends common.GenericAPITests {

  override val language = "cpp"

  var gen = new Main

  override def deleteOutDir(out : String) {
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.main(Array[String](
      source,
      "--debug-header",
      "-c",
      "-L", "cpp",
      "-p", name,
      "-Ocpp:revealSkillID=true",
      "-o", "testsuites/cpp/src/" + name) ++ options)
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    val packageName = packagePath.split("/").map(EscapeFunction.apply).mkString("::")
    gen = new Main
    gen.setPackage(List(packagePath))

    val f = new File(s"testsuites/cpp/test/$packagePath/generic${name}Test.cpp")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""#include <gtest/gtest.h>
#include "../common/utils.h"
#include "../../src/$packagePath/File.h"

using ::$packageName::api::SkillFile;
using namespace common;
""")
    rval
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
""")
    out.close
  }

  def makeSkipTest(out : PrintWriter, kind : String, name : String, testName : String, accept : Boolean) {
    out.write(s"""
TEST(${name.capitalize}_APITest, ${gen.escaped(kind)}_skipped_${gen.escaped(testName)}) {${
      if (accept) ""
      else """
    GTEST_FAIL() << "The test was skipped by the test generator.";"""
    }
}
""")
  }

  def makeRegularTest(out : PrintWriter, kind : String, name : String, testName : String, accept : Boolean, IR : TypeContext, obj : JSONObject) {
    val tc = IR.removeSpecialDeclarations();
    out.write(s"""
TEST(${name.capitalize}_APITest, ${if (accept) "Acc" else "Fail"}_${gen.escaped(testName)}) {
    try {
        auto sf = common::tempFile<SkillFile>();

        // create objects${createObjects(obj, tc, name)}
        
        // set fields${setFields(obj, tc)}

        sf->close();
    } catch (skill::SkillException e) {${
      if (accept) """
        GTEST_FAIL() << "an exception was thrown:" << std::endl << e.message;
    }
    GTEST_SUCCEED();"""
      else """
        GTEST_SUCCEED();
        return;
    }
    GTEST_FAIL() << "expected an exception, but none was thrown.";"""
    }
}
""")
  }

  private def typ(tc : TypeContext, name : String) : String = {
    val n = name.toLowerCase()
    try {
      gen.escaped((tc.getUsertypes.asScala ++ tc.getInterfaces.asScala).filter(_.getSkillName.equals(n)).head.getName.capital())
    } catch {
      case e : NoSuchElementException ⇒ fail(s"Type '$n' does not exist, fix your test description!")
    }
  }

  private def field(tc : TypeContext, typ : String, field : String) = {
    val tn = typ.toLowerCase()
    val t = tc.getUsertypes.asScala.find(_.getSkillName.equals(tn)).get
    val fn = field.toLowerCase()
    try {
      t.getAllFields.asScala.find(_.getSkillName.equals(fn)).get
    } catch {
      case e : NoSuchElementException ⇒ fail(s"Field '$fn' does not exist, fix your test description!")
    }
  }

  private def value(v : Any, f : Field) : String = value(v, f.getType)

  private def value(v : Any, t : Type) : String = t match {
    case t : GroundType ⇒
      t.getSkillName match {
        case "string" if null != v ⇒ s"""sf->strings->add("${v.toString()}")"""
        case "i8"                  ⇒ "(int8_t)" + v.toString()
        case "i16"                 ⇒ "(short)" + v.toString()
        case "f32"                 ⇒ "(float)" + v.toString()
        case "f64"                 ⇒ "(double)" + v.toString()
        case "v64" | "i64"         ⇒ v.toString() + "L"
        case _ ⇒
          if (null == v || v.toString().equals("null"))
            "nullptr"
          else
            v.toString()
      }

    case t : SingleBaseTypeContainer ⇒
      locally {
        var rval = t match {
          case t : SetType ⇒ s"set<${gen.mapType(t.getBaseType)}>()"
          case _           ⇒ s"array<${gen.mapType(t.getBaseType)}>()"
        }
        for (x ← v.asInstanceOf[JSONArray].iterator().asScala) {
          rval = s"put<${gen.mapType(t.getBaseType)}>($rval, ${value(x, t.getBaseType)})"
        }
        rval
      }

    case t : MapType if v != null ⇒ valueMap(v.asInstanceOf[JSONObject], t.getBaseTypes.asScala.toList)

    case _ ⇒
      if (null == v || v.toString().equals("null"))
        "nullptr"
      else
        v.toString()
  }

  private def valueMap(v : Any, ts : List[Type]) : String = {
    if (1 == ts.length) {
      value(v, ts.head)
    } else {
      var rval = s"map<${gen.mapType(ts.head)}, ${
        ts.tail match {
          case t if t.size >= 2 ⇒ t.map(gen.mapType).reduceRight((k, v) ⇒ s"::skill::api::Map<$k, $v>*")
          case t                ⇒ gen.mapType(t.head)
        }
      }>()"
      val obj = v.asInstanceOf[JSONObject]

      for (name ← JSONObject.getNames(obj)) {
        rval = s"put<${gen.mapType(ts.head)}, ${
          ts.tail match {
            case t if t.size >= 2 ⇒ t.map(gen.mapType).reduceRight((k, v) ⇒ s"::skill::api::Map<$k, $v>*")
            case t                ⇒ gen.mapType(t.head)
          }
        }>($rval, ${value(name, ts.head)}, ${valueMap(obj.get(name), ts.tail)})"
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
        auto $name = sf->${typeName}->add();"""
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
            val setter = gen.escaped("set" + f.getName.capital())
            s"""
        $name->$setter(${value(fs.get(fieldName), f)});"""
          }

          assignments.mkString
        }
      }

      rval.mkString("\n")
    }
  }
}
