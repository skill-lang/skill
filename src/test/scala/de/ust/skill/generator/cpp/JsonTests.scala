/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.cpp

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

import scala.io.Source
import scala.collection.mutable.HashMap
import scala.collection.mutable.Stack

import org.json.JSONTokener
import org.junit.runner.RunWith

import de.ust.skill.generator.common
import de.ust.skill.main.CommandLine
import org.scalatest.junit.JUnitRunner
import org.json.JSONObject
import org.json.JSONArray
import scala.collection.JavaConverters._

/**
 * Generic tests built for C++.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * upfront would be ugly.
 *
 * @author Philipp Martis
 */
@RunWith(classOf[JUnitRunner])
class JsonTests extends common.GenericJsonTests {

  override val language = "cpp"

  override def deleteOutDir(out : String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/cpp/src/", out)).deleteRecursively
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-c",
      "-L", "cpp",
      "-p", name,
      "-Ocpp:revealSkillID=true",
      "-o", "testsuites/cpp/src/" + name) ++ options)
  }

  def packagePathToName(packagePath : String) : String = {
    packagePath.split("/").map(EscapeFunction.apply).mkString("::")
  }

  def isKnownCppType(tp : String, testName : String) : Boolean = {
    // TODO: Change this to sth. like `for ( file <- new File("testsuites/cpp/src/" + testName).listFiles.filter(_.isFile) ) /*...*/`
    tp match {
      case "MarkerFieldType"      ⇒ return false
      case "ColoredNodeFieldType" ⇒ return false
      case _                      ⇒ return true
    }
  }

  def mapPackageToCpp(packagePath : String) : String = {
    // TODO: use Timm's function
    packagePath match {
      case "auto" ⇒ return "_auto"
      case _      ⇒ return packagePath
    }
  }

  def mapTypeToCpp(tp : String) : String = {
    // TODO: improve
    tp match {
      case "if"     ⇒ return "If"
      case "∀"      ⇒ return "Z2200"
      case "bool"   ⇒ return "Boolean"
      case "string" ⇒ return "String"
      case _        ⇒ return tp
    }
  }

  def mapNameToCpp(name : String) : String = {
    // TODO: improve
    name match {
      case "€" ⇒ return "Z20AC"
      case "☢" ⇒ return "Z2622"
      case _   ⇒ return name
    }
  }

  def mapTypeToPoolType(tp : String, packagePath : String) : String = {
    s"${mapPackageToCpp(packagePath)}::${mapTypeToCpp(tp)}Pool"
  }

  def mapTypeToPoolName(tp : String) : String = {
    s"sf->${mapTypeToCpp(tp)}"
  }

  def mapValueTypesToCpp(valueTypes : String, testName : String) : String = {
    var res = "<"
    var types = valueTypes.replaceAll("[\" <>]", "").split(",").map(_.capitalize)
    types = types.map(t ⇒ if (isKnownCppType(t + "FieldType", testName)) "::skill::fieldTypes::" + t + "FieldType"
    else "::skill::api::Object *")
    types.mkString(",")
  }

  def mapArrayTypeToCpp(attrName : String) : String = {
    // TODO: improve
    var res = "Array<"
    val tp = typeMappings(attrName).replaceAll(raw"\[.*", "")
    tp match {
      case "v64" ⇒ res += "int64_t"
      case _     ⇒ res += tp
    }
    res += " *>"
    "Array<Object *>"
  }

  def mapPointerTypeToCpp(attrName : String, packagePath : String) : String = {
    var res = ""
    var tp = typeMappings(attrName)
    if (!tp.startsWith("set<") && tp != "annotation")
      res += s"${mapPackageToCpp(packagePath)}::"
    else
      println(s"Not appending ${mapPackageToCpp(packagePath)}::...")
    if (packagePath == "graphInterface" && (attrName == "next" || attrName == "f")) {
      tp = "Object"
      res = ""
    } else if (packagePath == "graphInterface" && attrName == "anAbstractNode")
      tp = "AbstractNode"
    res += s"${mapTypeToCpp(tp).capitalize} *"
    res.replaceAll("Set<Node>", "Set<graph::Node *>").replaceAll("Annotation", "Object")
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    val packageName = packagePathToName(packagePath)
    val f = new File(s"testsuites/cpp/test/$packagePath/generic${name}Test.cpp")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(getImportList(packagePath))
    rval.write(s"""
namespace $packageName {
    namespace internal {
	struct TestException : public SkillException {
	    TestException(const std::string& message) :
	        skill::SkillException(message) { }
	};
    }  // namespace internal
}  // namespace $packageName

using ::$packageName::internal::TestException;

""" + generateTypeMappings(packagePath))
    for (path ← collectSkillSpecification(packagePath).getParentFile().listFiles if path.getName.endsWith(".json")) {
      makeTestForJson(rval, path.getAbsolutePath(), packagePath);
    }
    rval
  }

  def makeTestForJson(rval : PrintWriter, jsonFile : String, packagePath : String) : PrintWriter = {
    val packageName = packagePathToName(packagePath)
    def testname = new File(jsonFile).getName.replace(".json", "");
    val skillSpec = collectSkillSpecification(packagePath)
    var skillFile = skillSpec.getName()

    rval.write(s"""
TEST(${testname}, ${packageName}) {
    std::map<std::string, Object*> objs;

    try {
"""
      + generatePoolInstantiation(packagePath, jsonFile)
      + generateObjectInstantiation(jsonFile, packagePath)
      + s"""
        sf->flush();\n""")
    val toFail = new JSONObject(new JSONTokener(new java.io.FileInputStream(jsonFile))).getBoolean("shouldFail")
    if (toFail)
      rval.write(s"""
        GTEST_FAIL();\n""")
    else
      rval.write(s"""
        GTEST_SUCCEED();\n""")
    rval.write(s"""    } catch (ifstream::failure e) {
        GTEST_FAIL();
    } catch (TestException e) {
        GTEST_FAIL();
    } catch (skill::SkillException e) {\n""")
    if (toFail)
      rval.write(s"""
        GTEST_SUCCEED();\n""")
    else
      rval.write(s"""
        GTEST_FAIL();\n""")
    rval.write(s"""    }

}  // TEST(${testname}, ${packageName})
""")
    rval
  }

  def calcTypeMappings(packagePath : String) : Map[String, String] = {
    var res = Map[String, String]()
    val skillSpec = collectSkillSpecification(packagePath)
    for (line ← Source.fromFile(skillSpec).getLines.filter(_.matches(raw""" *(auto)? *[^ "]*(<[^>]*>|\[[^]]*]|\([^)]*\))? *[^ ]*;"""))) { /* appease vim: " */
      val tp = line.replaceFirst(" *(auto)? *", "").replaceAll(" *[^ ]*$", "")
      val name = line.replaceFirst(".* ", "").replaceAll(";", "")
      res += (name -> tp)
      println("°°° " + tp + " " + name + " (" + packagePath + ")")
    }
    res
  }

  def calcIsInterface(packagePath : String) : Set[String] = {
    // TODO: improve
    var res = Set[String]()
    res += "E"
    res += "F"
    res += "ColoredNode"
    res += "Colored"
    res += "Marker"
    res
  }

  def calcSupertypes(packagePath : String) : HashMap[String, String] = {
    // TODO: improve
    var res = new HashMap[String, String]()
    val skillSpec = collectSkillSpecification(packagePath)
    for (line ← Source fromFile (skillSpec) getLines) {
      if (line.matches("^(interface *|enum *)?[A-Zi∀].*")) {
        var tp = line.replaceAll(""" *[:{].*""", "").replaceAll("""^(interface|enum) *""", "")
        val superTp = line.replaceAll(""".*: *""", "").replaceAll(""" .*""", "")
        if (!superTp.matches(raw".*\{.*") && superTp != "interface" && superTp != "enum") {
          //println("Supertype(" + tp + ") = " + superTp)
          res += (tp -> superTp)
        } else {
          //println("Supertype(" + tp + ") = " + tp)
          res += (tp -> tp)
        }
      }
    }
    res
  }

  def getImportList(packagePath : String) : String = {
    val packageName = packagePathToName(packagePath)
    var res = s"""
#include <fstream>
#include <map>

#include <gtest/gtest.h>
#include <json/json.h>
#include "../../src/$packagePath/File.h"
#include "../../../../../cppTest/test/common/utils.h"

#include <skill/api/SkillException.h>
#include <skill/api/Sets.h>
#include <skill/api/Maps.h>
#include <skill/api/Arrays.h>
#include <skill/internal/FileParser.h>
#include <skill/internal/AbstractStoragePool.h>
#include <skill/internal/StoragePool.h>
#include <skill/internal/StringPool.h>
#include <skill/fieldTypes/AnnotationType.h>
#include <skill/fieldTypes/BuiltinFieldType.h>

"""
    var done = Set[String]()
    for (tp ← supertypes.valuesIterator.toSet[String]) {
      var superTp = tp
      while (superTp != supertypes(superTp)) {
        superTp = supertypes(superTp)
      }
      if (!done(superTp) && !isInterface(superTp)) {
        done += superTp
        res ++= s"""#include "../../src/$packagePath/TypesOf${mapTypeToCpp(superTp)}.h"\n"""
      }
    }
    res ++= s"""

using ::std::ifstream;

using ::skill::api::Object;
using ::skill::api::String;
using ::skill::api::string_t;
using ::skill::api::Set;
using ::skill::api::Array;
using ::skill::api::Map;
using ::skill::SkillException;
using ::skill::internal::AbstractStoragePool;
using ::skill::internal::StoragePool;
using ::skill::internal::StringPool;
using ::$packageName::api::SkillFile;

"""
    res
  }

  def generatePoolInstantiation(packagePath : String, jsonFile : String) : String = {
    var res = s"""        auto sf = common::tempFile<SkillFile>();
        sf->changePath("write.$packagePath");\n"""
    res
  }

  def generateObjectInstantiation(jsonFile : String, packagePath : String) : String = {
    val testName = jsonFile.replace("""/[^/]+""", "").replace(""".*/""", "")
    val fileTokens = new JSONTokener(new java.io.FileInputStream(jsonFile));
    val content = new JSONObject(fileTokens);

    val jsonObjects = content.getJSONObject("data");
    var res = "\n";
    var toFail = false
    if (content.getBoolean("shouldFail")) {
      toFail = true
    }
    for (currentObjKey ← jsonObjects.keySet().asScala) {
      val objName = currentObjKey;
      if (jsonObjects.optJSONObject(objName) == null) { // name of another object
        res ++= "Object* " + objName.toLowerCase() + ";\n" //+ " = &" + jsonObjects.getString(objName) + ";\n"
      } else { // object to instantiate
        val obj = jsonObjects.getJSONObject(objName);
        val objType = obj.getString("type");
        if (objType.startsWith("skill.")) {
          objType match {
            case "skill.map" ⇒
              res ++= "        std::map<"
              res ++= mapValueTypesToCpp(obj.getString("valueTypes"), testName)
              res ++= "> " + objName.toLowerCase() + ";\n"
          }
        } else if (packagePath != "constants"
          && !jsonFile.endsWith("escaping_fail_1.json") // failing verified manually :)
          && !jsonFile.endsWith("user_fail_1.json") // failing verified manually :)
          && !jsonFile.endsWith("enum_fail_1.json") // failing verified manually :)
          && !jsonFile.endsWith("enum_fail_2.json")) { // failing verified manually :)
          val pkg = mapPackageToCpp(packagePath)
          val tp = mapTypeToCpp(objType)
          val pool = mapTypeToPoolName(objType)
          res ++= s"""        {
            auto* obj = ${pool}->add();
            objs["$objName"] = obj;\n"""
          val objAttr = obj.getJSONObject("attr")
          for (attrName ← objAttr.keySet().asScala) {
            var varName = s"${objName.toLowerCase()}_$attrName"
            if (objAttr.optJSONArray(attrName) != null) { // Array
              var attr = objAttr.getJSONArray(attrName)
              res ++= s"""            ${mapArrayTypeToCpp(attrName)} $varName{0};\n"""
              //              for ( v <- attr.list ) {
              //                if ( v.isInstanceOf[
              //              }
              """            $varName.ensureSize($varName.length() + 1);\n"""
              //              res ++= s"""${objName.toLowerCase()}->set${attrName.capitalize}($varName);"""
            } else if (objAttr.optJSONObject(attrName) != null) { // Map
              var attr = objAttr.getJSONObject(attrName)
            } else { // some other type (org.json would deliberately convert it to anything)
              var attr = objAttr.get(attrName)
              if (attr.isInstanceOf[Boolean]) {
                res ++= s"""            obj->set${mapNameToCpp(attrName).capitalize}(${objAttr.getBoolean(attrName)});\n"""
              } else if (attr.isInstanceOf[Int]) {
                res ++= s"""            obj->set${mapNameToCpp(attrName).capitalize}(${objAttr.getInt(attrName)});\n"""
              } else if (attr.isInstanceOf[Double]) {
                res ++= s"""            obj->set${mapNameToCpp(attrName).capitalize}(${objAttr.getDouble(attrName)});\n"""
              } else if (attr.isInstanceOf[String] && objAttr.getString(attrName).contains('"')) {
                res ++= s"""            obj->set${mapNameToCpp(attrName).capitalize}(((StringPool *) sf->strings)->add(${objAttr.getString(attrName)}));\n"""
              } else if (attr.isInstanceOf[String]) { // name of another object
                if (!typeMappings.contains(attrName))
                  typeMappings += (attrName -> attrName.capitalize)
                res ++= s"""            obj->set${mapNameToCpp(attrName).capitalize}((${mapPointerTypeToCpp(attrName, packagePath)}) objs["${objAttr.getString(attrName)}"]);\n"""
              }
            }
          } // for ( attrName <- objAttr.keySet().asScala )
          res ++= s"""\n        }\n"""
        } // else if ( packagePath != "constants" )
      }
    } // for (currentObjKey <- jsonObjects.keySet().asScala)
    res
  }

  def generateTypeMappings(packagePath : String) : String = {
    val skillSpec = collectSkillSpecification(packagePath)
    var skillFile = skillSpec.getName()
    var res = s"""
static std::map<std::string, const AbstractStoragePool*> pools;

"""
    res
    ""
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
""")
    out.close
  }

  var typeMappings = Map[String, String]()
  var supertypes = HashMap[String, String]()
  var isInterface = Set[String]()

  override def makeTests(name : String) {
    typeMappings = calcTypeMappings(name)
    supertypes = calcSupertypes(name)
    isInterface = calcIsInterface(name)
    val out = newTestFile(name, "Json")
    closeTestFile(out)
  }

  override def finalizeTests {
    // nothing yet
  }
}
