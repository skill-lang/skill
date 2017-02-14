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

import scala.reflect.io.Path.jfile2path

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import de.ust.skill.generator.common
import de.ust.skill.main.CommandLine

/**
 * Generic tests built for Java.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * upfront would be ugly.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GenericTests extends common.GenericTests {

  override def language : String = "cpp"

  override def deleteOutDir(out : String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/cpp/src/", out)).deleteRecursively
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-L", "cpp",
      "-p", name,
      "-Ocpp:revealSkillID=true",
      "-o", "testsuites/cpp/src/" + name) ++ options)
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    val packageName = packagePath.split("/").map(EscapeFunction.apply).mkString("::")
    val f = new File(s"testsuites/cpp/test/$packagePath/generic${name}Test.cpp")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""
#include <gtest/gtest.h>
#include "../../src/$packagePath/File.h"

using ::$packageName::api::SkillFile;
""")
    rval
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
""")
    out.close
  }

  override def makeGenBinaryTests(name : String) {
    val (accept, reject) = collectBinaries(name)

    // generate read tests
    locally {
      val out = newTestFile(name, "Read")

      for (f ← accept) out.write(s"""
TEST(${name.capitalize}Parser, Accept_${f.getName.replaceAll("\\W", "_")}) {
    try {
        auto s = std::unique_ptr<SkillFile>(SkillFile::open("../../${f.getPath.replaceAll("\\\\", "\\\\\\\\")}"));
        s->check();
    } catch (skill::SkillException e) {
        GTEST_FAIL() << "an exception was thrown:" << std::endl << e.message;
    }
    GTEST_SUCCEED();
}
""")
      for (f ← reject) out.write(s"""
TEST(${name.capitalize}Parser, Reject_${f.getName.replaceAll("\\W", "_")}) {
    try {
        auto s = std::unique_ptr<SkillFile>(SkillFile::open("../../${f.getPath.replaceAll("\\\\", "\\\\\\\\")}"));
        s->check();
    } catch (skill::SkillException e) {
        GTEST_SUCCEED();
        return;
    }
    GTEST_FAIL() << "expected an exception, but none was thrown.";
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
