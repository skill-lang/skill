/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.c

import java.io.File
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Scala specific tests.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GeneratorTest extends FunSuite {

  private val sourceRootFolder = new File("src/test/resources/c/");
  private val targetRootFolder = new File("testsuites/c/src/generated");

  private def deleteFiles(target : File) {
    if (target.isDirectory) {
      target.listFiles().foreach(deleteFiles)
    }
    target.delete()
  }

  def check(prefix : String, sourceFile : String, targetDir : String) {
    deleteFiles(new File(targetRootFolder, targetDir))
    Main.main(Array(
      "-p", prefix, sourceRootFolder.getAbsolutePath()+"/"+sourceFile, targetRootFolder.getAbsolutePath()+"/"+targetDir))
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
  test("container 1")(check("", "container1.skill", "container1"))
  test("container user types")(check("", "container_user_type.skill", "container_user_type"))
  test("container annotation")(check("", "container_annotation.skill", "container_annotation"))
  test("container string")(check("", "container_string.skill", "container_string"))

  // Generate two bindings with prefix into the same directory, so that the test can compile two bindings together
  test("prefix sub")(check("sub", "subtypesExample.skill", "subtypes_prefix"))
  test("prefix date")(check("date", "date.skill", "date_prefix"))
}
