/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.java

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
class JsonTests extends common.GenericTests {

  override val language = "java"

  override def deleteOutDir(out : String) {
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-c",
      "-L", "java",
      "-p", name,
      "-Ojava:SuppressWarnings=true",
      "-d", "testsuites/java/lib",
      "-o", "testsuites/java/src/main/java/") ++ options)
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    val f = new File(s"testsuites/java/src/test/java/$packagePath/Generic${name}Test.java")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""
package $packagePath;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import $packagePath.api.SkillFile;

import de.ust.skill.common.java.api.Access;
import de.ust.skill.common.java.api.SkillFile.Mode;
import de.ust.skill.common.java.internal.SkillObject;
import de.ust.skill.common.java.internal.ParseException;

public class GenericJSONReaderTest${name} {

	/**
	 * Tests the object generation capabilities.
	 */
	@Test
	public void test() {
		Path path = Paths.get(System.getProperty("user.dir"), "src", "test", "resources");
		path = path.resolve("values.json");
		
		try {
			JSONArray currentJSON = JSONReader.readJSON(path.toFile());
			for (int i = 0; i < currentJSON.length(); i++) {
				JSONObject currentTest = currentJSON.getJSONObject(i);
				SkillObject obj = JSONReader.createSkillObjectFromJSON(currentTest);
				System.out.println(obj.prettyString());
				assertTrue(true);
			}
		} catch (JSONException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (SecurityException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (InstantiationException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
""")
    rval
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
}
""")
    out.close
  }

  override def makeGenBinaryTests(name : String) {
    locally {
      val out = newTestFile(name, "Read")
      closeTestFile(out)
    }
  }
  
  override def finalizeTests {
    // nothing yet
  }
}
