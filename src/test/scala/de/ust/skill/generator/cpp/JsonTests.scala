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
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class JsonTests extends common.GenericJsonTests {

  override val language = "cpp"

  override def deleteOutDir(out: String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/cpp/src/", out)).deleteRecursively
  }

  override def callMainFor(name: String, source: String, options: Seq[String]) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-c",
      "-L", "cpp",
      "-p", name,
      "-Ocpp:revealSkillID=true",
      "-o", "testsuites/cpp/src/" + name) ++ options)
  }

  def packagePathToName(packagePath: String): String = {
    packagePath.split("/").map(EscapeFunction.apply).mkString("::")
  }

  def isKnownCppType(tp: String, testName: String) : Boolean = {
    // TODO: Change this to sth. like `for ( file <- new File("testsuites/cpp/src/" + testName).listFiles.filter(_.isFile) ) /*...*/`
    tp match {
            case "MarkerFieldType" => return false
            case "ColoredNodeFieldType" => return false
            case _  => return true
    }
  }

  def mapPackageToCpp(packagePath: String): String = {
    // TODO: use Timm's function
    packagePath match {
      case "auto" => return "_auto"
      case _ => return packagePath
    }
  }

  def mapTypeToCpp(tp: String): String = {
    // TODO: improve
    tp match {
      case "if" => return "If"
      case "∀" => return "Z2200"
      case _ => return tp
    }
  }

  def mapNameToCpp(name: String): String = {
    // TODO: improve
    name match {
      case "€" => return "Z20AC"
      case "☢" => return "Z2622"
      case _ => return name
    }
  }

  def mapTypeToPoolType(tp: String, packagePath: String): String = {
    s"${mapPackageToCpp(packagePath)}::${mapTypeToCpp(tp)}Pool"
  }

  def mapTypeToPoolName(tp: String): String = {
    s"${mapTypeToCpp(tp).toLowerCase()}_pool"
  }

  def mapValueTypesToCpp(valueTypes: String, testName: String): String = {
    var res="<"
    var types = valueTypes.replaceAll("[\" <>]", "").split(",").map(_.capitalize)
    types = types.map(t => if (isKnownCppType(t + "FieldType", testName)) "::skill::fieldTypes::" + t +  "FieldType"
                           else "::skill::api::Object *")
    types.mkString(",")
  }

  def newTestFile(packagePath: String, name: String): PrintWriter = {
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

  def makeTestForJson(rval: PrintWriter, jsonFile: String, packagePath: String): PrintWriter = {
    val packageName = packagePathToName(packagePath)
    def testname = new File(jsonFile).getName.replace(".json", "");
    val skillSpec = collectSkillSpecification(packagePath)
    var skillFile = skillSpec.getName()


    rval.write(s"""
TEST(${testname}, ${packageName}) {
    try {
"""
      + generatePoolInstantiation(packagePath)
      + generateObjectInstantiation(jsonFile, packagePath)
      + s"""
        GTEST_SUCCEED();
    } catch (ifstream::failure e) {
        GTEST_FAIL();
    } catch (TestException e) {
        GTEST_FAIL();
    } catch (skill::SkillException e) {
        GTEST_FAIL();
    }

}  // TEST(${testname}, ${packageName})
""")
    rval
  }

  def calcIsInterface(packagePath: String): Set[String] = {
    // TODO: improve
    var res = Set[String]()
    res += "E"
    res += "F"
    res += "ColoredNode"
    res += "Colored"
    res += "Marker"
    res
  }

  def calcSupertypes(packagePath: String): HashMap[String,String] = {
    // TODO: improve
    var res = new HashMap[String,String]()
    val skillSpec = collectSkillSpecification(packagePath)
    for ( line <- Source fromFile(skillSpec) getLines ) {
      if ( line.matches("^(interface *|enum *)?[A-Zi∀].*") ) {
        var tp = line.replaceAll(""" *[:{].*""", "").replaceAll("""^(interface|enum) *""", "")
        val superTp = line.replaceAll(""".*: *""", "").replaceAll(""" .*""", "")
        if ( ! superTp.matches(raw".*\{.*") && superTp != "interface" && superTp != "enum" ) {
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

  def getImportList(packagePath: String): String = {
    val packageName = packagePathToName(packagePath)
    var res = s"""
#include <fstream>

#include <gtest/gtest.h>
#include <json/json.h>
#include "../../src/$packagePath/File.h"

#include <skill/api/SkillException.h>
#include <skill/api/Sets.h>
#include <skill/api/Maps.h>
#include <skill/api/Arrays.h>
#include <skill/internal/FileParser.h>
#include <skill/internal/AbstractStoragePool.h>
#include <skill/internal/StoragePool.h>
#include <skill/fieldTypes/BuiltinFieldType.h>

"""
var done = Set[String]()
for ( tp <- supertypes.valuesIterator.toSet[String] ) {
  var superTp = tp
  while ( superTp != supertypes(superTp) ) {
    superTp = supertypes(superTp)
  }
  if ( ! done(superTp) && ! isInterface(superTp) ) {
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
using ::$packageName::api::SkillFile;

"""
    res
  }

  def generatePoolInstantiation(packagePath: String): String = {
    var res = ""
    var typeID = 0; var done = Set[String]()
    for ( tp <- supertypes.keysIterator.toSet[String] ) {
      typeID += 1
      var superTp = supertypes(tp)
      if ( ! isInterface(tp) && ! done (tp) && tp == superTp ) {  // Stand-alone pool
        done += tp
        res += s"""        ${mapTypeToPoolType(tp, packagePath)} ${mapTypeToPoolName(tp)}{$typeID, nullptr, nullptr};\n"""
      } else if ( ! isInterface(tp) && ! done(tp) ) {  // Pool with superpool
        var todo = Stack[String]()
        while ( ! done(superTp) && superTp != supertypes(superTp) && ! done(supertypes(superTp)) ) {
          if ( ! isInterface(superTp) )
            todo.push(superTp)
          superTp = supertypes(superTp)
        }
        if ( ! done(superTp) ) {
          done += superTp
          if ( superTp == supertypes(superTp) || isInterface(superTp) )
            res += s"""        ${mapTypeToPoolType(superTp, packagePath)} ${mapTypeToPoolName(superTp)}{$typeID, nullptr, nullptr};\n"""
          else
            res += s"""        ${mapTypeToPoolType(superTp, packagePath)} ${mapTypeToPoolName(superTp)}{$typeID, &${mapTypeToPoolName(supertypes(superTp))}, nullptr, nullptr};\n"""
        }
        var lastSuperTp = superTp
        while ( ! todo.isEmpty ) {
          lastSuperTp = superTp
          superTp = todo.pop
          done += superTp
          res += s"""        ${mapTypeToPoolType(superTp, packagePath)} ${mapTypeToPoolName(superTp)}{$typeID, &${mapTypeToPoolName(lastSuperTp)}, nullptr, nullptr};\n"""
        }
        done += tp
        res += s"""        ${mapTypeToPoolType(tp, packagePath)} ${mapTypeToPoolName(tp)}{$typeID, &${mapTypeToPoolName(superTp)}, nullptr, nullptr};\n"""
      }  // if ( ! isInterface(tp) && ! done (tp) && tp == superTp )
    }  // for ( tp <- ... )
    res
  }

  def generateObjectInstantiation(jsonFile: String, packagePath: String): String = {
    val testName = jsonFile.replace("""/[^/]+""", "").replace(""".*/""", "")
    val fileTokens = new JSONTokener(new java.io.FileInputStream(jsonFile));
    val content = new JSONObject(fileTokens);

    val jsonObjects = content.getJSONObject("data");
    var res = "\n";
    var toFail = false
    if (content.getBoolean("shouldFail")) {
      toFail = true
    }
    for (currentObjKey <- jsonObjects.keySet().asScala) {
      val objName = currentObjKey;
      if (jsonObjects.optJSONObject(objName) == null) {  // name of another object
        res ++= "Object* " + objName.toLowerCase()  +";\n"  //+ " = &" + jsonObjects.getString(objName) + ";\n"
      } else {  // object to instantiate
        val obj = jsonObjects.getJSONObject(objName);
        val objType = obj.getString("type");
        if (objType.startsWith("skill.")) {
          objType match {
            case "skill.map" ⇒ res ++= "        std::map<"
                               res ++= mapValueTypesToCpp(obj.getString("valueTypes"), testName)
                               res ++= "> " + objName.toLowerCase() + ";\n"
          }
        } else {
          val pkg = mapPackageToCpp(packagePath)
          val tp = mapTypeToCpp(objType)
          val pool = mapTypeToPoolName(objType)
          res ++= s"""        {
            auto* obj = ${pool}.add();\n"""
          val objAttr = obj.getJSONObject("attr")
          for ( attrName <- objAttr.keySet().asScala ) {
            var varName = s"${objName.toLowerCase()}_$attrName"
            if ( objAttr.optJSONArray(attrName) != null ) {  // Array
              var attr = objAttr.getJSONArray(attrName)
              res ++= s"""            Array<Object*> $varName{0};\n"""
//              res ++= s"""${objName.toLowerCase()}->set${attrName.capitalize}($varName);"""
            } else if ( objAttr.optJSONObject(attrName) != null ) { // Map
              var attr = objAttr.getJSONObject(attrName)
            } else {
              // TODO
            }
          }  // for ( attrName <- objAttr.keySet().asScala )
          res ++= s"""\n        }\n"""
        }  // else
      }
    }  // for (currentObjKey <- jsonObjects.keySet().asScala)
    res
  }

  def generateTypeMappings(packagePath: String) : String = {
    val skillSpec = collectSkillSpecification(packagePath)
    var skillFile = skillSpec.getName()
    var res = s"""
static std::map<std::string, const AbstractStoragePool*> pools;

"""
    res
  }

  def closeTestFile(out: java.io.PrintWriter) {
    out.write("""
""")
    out.close
  }

  var supertypes = HashMap[String,String]()
  var isInterface = Set[String]()

  override def makeGenBinaryTests(name: String) {
    locally {
      supertypes = calcSupertypes(name)
      isInterface = calcIsInterface(name)
      val out = newTestFile(name, "Json")
      closeTestFile(out)
    }
  }

  override def finalizeTests {
    // nothing yet
  }
}
