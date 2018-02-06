/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c

import java.io.File

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import de.ust.skill.main.CommandLine

/**
 * Scala specific tests.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  private val sourceRootFolder = new File("src/test/resources/c/");

  private def deleteFiles(target : File) {
    if (target.isDirectory) {
      target.listFiles().foreach(deleteFiles)
    }
    target.delete()
  }

  def check(prefix : String, src : String, target : String) {
    // delete seems to cause sporadic test failure
    //    deleteFiles(new File(s"testsuites/c/src/generated/$target"))
    CommandLine.exit = { s ⇒ fail(s) }
    CommandLine.main(Array[String]("src/test/resources/c/" + src,
      "--debug-header",
      "-L", "C",
      "-p", prefix,
      "-o", "testsuites/c/src/generated/" + target))
  }

  test("annotation")(check("", "annotation.skill", "annotation"))

  test("date")(check("", "date.skill", "date"))
  test("basic types")(check("", "basic_types.skill", "basic_types"))
  test("empty")(check("", "empty.skill", "empty"))
  test("const")(check("", "const.skill", "const"))
  test("auto")(check("", "auto.skill", "auto"))
  test("float")(check("", "float.skill", "float"))
  test("node")(check("", "node.skill", "node"))
  test("subtypes")(check("", "subtypesExample.skill", "subtypes"))
  ignore("container 1")(check("", "container1.skill", "container1"))
  ignore("container user types")(check("", "container_user_type.skill", "container_user_type"))
  ignore("container annotation")(check("", "container_annotation.skill", "container_annotation"))
  ignore("container string")(check("", "container_string.skill", "container_string"))

  // Generate two bindings with prefix into the same directory, so that the test can compile two bindings together
  test("prefix sub")(check("sub", "subtypesExample.skill", "subtypes_prefix"))
  test("prefix date")(check("date", "date.skill", "date_prefix"))
}
