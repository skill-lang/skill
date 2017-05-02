/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.common

import java.io.File

import scala.io.Codec

import org.scalatest.BeforeAndAfterAll
import org.scalatest.ConfigMap
import org.scalatest.FunSuite

import de.ust.skill.main.CommandLine
import java.nio.file.Paths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import scala.io.Source

/**
 * Common implementation of generic tests
 *
 * @author Timm Felden
 */
abstract class GenericJsonTests extends GenericTests {

  /**
   * Helper function that collects a specification for a given package name.
   */
  final def collectSkillSpecification(packageName: String): File = {
    val base = new File("src/test/resources/gentest");

    collect(base)
      .filter(_.getName.endsWith(".skill"))
      .filter(file => {
        Source.fromFile(file).getLines().next().startsWith("#! " + packageName);
      })
      .head;
  }
  
}
