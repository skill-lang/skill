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

  def newTestFile(packagePath: String, name: String): PrintWriter = {
    val packageName = packagePathToName(packagePath)
    val f = new File(s"testsuites/cpp/test/$packagePath/generic${name}Test.cpp")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""
#include <fstream>

#include <gtest/gtest.h>
#include <json/json.h>
#include "../../src/$packagePath/File.h"

using ::$packageName::api::SkillFile;
using ::std::ifstream;

""")
    for (path ← collectSkillSpecification(packagePath).getParentFile().listFiles if path.getName.endsWith(".json")) {
      makeTestForJson(rval, path.getAbsolutePath(), packagePath);
    }
    rval
  }

  def makeTestForJson(rval: PrintWriter, testfile: String, packagePath: String): PrintWriter = {
    val packageName = packagePathToName(packagePath)
    def testname = new File(testfile).getName.replace(".json", "");
    rval.write(s"""
TEST(${testname}, ${packageName}) {

    ifstream json{};
    json.exceptions(ifstream::failbit | ifstream::badbit);

    try {
        json.open("$testfile");
        Json::Value root;
        json >> root;
        if ( root.get("shouldFail", false).asBool() )
            std::cout << "This is a test supposed to fail." << std::endl;
            // TODO: Implement
        else
            std::cout << "This is a test supposed to succeed." << std::endl;
            // TODO: Implement
        GTEST_SUCCEED();
    } catch (ifstream::failure e) {
        GTEST_FAIL();
    }

}  // TEST(${testname}, ${packageName})
""")
    rval
  }

  def closeTestFile(out: java.io.PrintWriter) {
    out.write("""
""")
    out.close
  }

  override def makeGenBinaryTests(name: String) {
    locally {
      val out = newTestFile(name, "Json")
      closeTestFile(out)
    }
  }

  override def finalizeTests {
    // nothing yet
  }
}
