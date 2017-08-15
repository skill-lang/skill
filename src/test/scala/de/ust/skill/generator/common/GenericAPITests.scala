/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.common

import java.io.File

import scala.collection.mutable.HashMap

import de.ust.skill.parser.Parser

/**
 * Common implementation of generic tests
 *
 * @author Timm Felden
 */
abstract class GenericAPITests extends GenericTests {

  /**
   * Helper function that collects test-specifications.
   * @return map from .skill-spec to list of .json-specs
   */
  lazy val collectTestspecs : HashMap[File, Array[File]] = {
    val base = new File("src/test/resources/gentest");

    val specs = collect(base).filter(_.getName.endsWith(".skill"));

    val rval = new HashMap[File, Array[File]]
    for (s ← specs)
      rval(s) = s.getParentFile.listFiles().filter(_.getName.endsWith(".json"))

    rval
  }

  def parse(spec : File) = Parser.process(spec)
}
