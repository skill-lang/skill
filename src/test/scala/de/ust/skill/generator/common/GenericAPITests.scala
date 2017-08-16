/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.common

import java.io.File

import scala.collection.mutable.HashMap

import de.ust.skill.parser.Parser
import org.json.JSONTokener
import scala.io.Source
import java.io.PrintWriter
import org.json.JSONObject

import scala.collection.JavaConversions._
import de.ust.skill.ir.TypeContext

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

  def newTestFile(packagePath : String, name : String) : PrintWriter;

  def closeTestFile(out : java.io.PrintWriter) : Unit;

  def makeSkipTest(out : PrintWriter, kind : String, name : String, testName : String, accept : Boolean);
  
  def makeRegularTest(out : PrintWriter, kind : String, name : String, testName : String, accept : Boolean, tc : TypeContext, obj : JSONObject);

  final override def makeTests(name : String) {
    val (spec, tests) = collectTestspecs.filter(file ⇒ {
      Source.fromFile(file._1).getLines().next().startsWith("#! " + name);
    }).head

    val IR = parse(spec).removeTypedefs().removeEnums()

    val out = newTestFile(name, "API")

    println(spec.getName)

    // create tests
    for (f ← tests) {
      println(s"  - ${f.getName}")

      val test = new JSONObject(new JSONTokener(new java.io.FileInputStream(f)))

      val skipped = try {
        test.getJSONArray("skip").iterator().contains(language)
      } catch {
        case e : Exception ⇒ false;
      }

      val accept = try {
        test.getString("should").toLowerCase match {
          case "fail" ⇒ false
          case "skip" ⇒ skipped
          case _      ⇒ true
        }
      } catch {
        case e : Exception ⇒ true;
      }

      val kind = try {
        val v = test.getString("kind")
        if (null == v) "core" else v
      } catch {
        case e : Exception ⇒ "core";
      }

      val testName = f.getName.replace(".json", "");

      if (skipped) {
        makeSkipTest(out, kind, name, testName, accept);
      } else {
        makeRegularTest(out, kind, name, testName, accept, IR, test.getJSONObject("obj"));
      }
    }
    closeTestFile(out)
  }

  override def finalizeTests {
    // nothing yet
  }
}
