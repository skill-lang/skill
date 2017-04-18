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

import org.junit.runner.RunWith

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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.ust.skill.common.java.internal.SkillObject;

public class GenericJSONReaderTest {

	@Rule //http://stackoverflow.com/a/2935935
	public final ExpectedException exception = ExpectedException.none();

	private static Path path;
	private JSONObject currentJSON;

	/**
	 * Tests the object generation capabilities.
	 */
	@BeforeClass
	public static void init() throws JSONException, MalformedURLException, IOException {
		path = Paths.get(System.getProperty("user.dir"), 
        "src", 
        "test", 
        "resources", 
        "${packagePath}");
	}

""")
    
    for(path ← new File("src/test/resources/gentest/" + packagePath).listFiles if path.getName.endsWith(".json")){
      makeTestForJson(rval, path.getName());
    }
    rval
  }
  
  def makeTestForJson(rval: PrintWriter, testfile: String): PrintWriter = {
    rval.write(s"""
	@Test
	public void jsonTest() throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, JSONException, MalformedURLException, IOException {
		Path testFilePath = path.resolve("${testfile}");
    this.currentJSON = JSONReader.readJSON(testFilePath.toFile());
		JSONObject currentTest = currentJSON;
		if(JSONReader.shouldExpectException(currentTest)){
			System.out.println("There should be an exception coming up!");
			exception.expect(Exception.class);
		}
		SkillObject obj = JSONReader.createSkillObjectFromJSON(currentTest);
		System.out.println(obj.prettyString());
		assertTrue(true);
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
