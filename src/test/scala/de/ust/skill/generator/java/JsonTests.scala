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

import org.json.JSONTokener
import org.junit.runner.RunWith

import de.ust.skill.generator.common
import de.ust.skill.main.CommandLine
import org.scalatest.junit.JUnitRunner
import org.json.JSONObject
import org.json.JSONArray
import scala.collection.JavaConverters._

/**
 * Generic tests built for Java.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * upfront would be ugly.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class JsonTests extends common.GenericJsonTests {

  override val language = "java"

  override def deleteOutDir(out: String) {
  }

  override def callMainFor(name: String, source: String, options: Seq[String]) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-c",
      "-L", "java",
      "-p", name,
      "-Ojava:SuppressWarnings=true",
      "-d", "testsuites/java/lib",
      "-o", "testsuites/java/src/main/java/") ++ options)
  }

  def newTestFile(packagePath: String, name: String): PrintWriter = {
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
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import de.ust.skill.common.java.api.Access;
import de.ust.skill.common.java.internal.FieldDeclaration;
import de.ust.skill.common.java.internal.SkillObject;
import $packagePath.api.SkillFile;

public class GenericJSONReaderTest extends common.CommonTest {

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

    for (path ← collectSkillSpecification(packagePath).getParentFile().listFiles if path.getName.endsWith(".json")) {
      makeTestForJson(rval, path.getAbsolutePath());
    }
    rval
  }

  def makeTestForJson(rval: PrintWriter, testfile: String): PrintWriter = {
    rval.write(s"""
	@Test
	public void jsonTest() throws ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, JSONException, MalformedURLException, IOException {
    Map<String, Access<?>> types = new HashMap<>();
		Map<String, HashMap<String, FieldDeclaration<?, ?>>> typeFieldMapping = new HashMap<>();
		
		Path tempBinaryFile = tmpFile("write.generic.checked");
		SkillFile sf = SkillFile.open(tempBinaryFile);
        reflectiveInit(sf);
        
		creator.SkillObjectCreator.generateSkillFileMappings(sf, types, typeFieldMapping);
"""
      +
      generateObjectInstantiation(testfile)
      +

      """
	}

""")
    rval
  }

  def generateObjectInstantiation(jsonFile: String): String = {
    val fileTokens = new JSONTokener(new java.io.FileInputStream(jsonFile));
    val content = new JSONObject(fileTokens);
    val jsonObjects = content.getJSONObject("data");
    var instantiations = "";

    for (currentObjKey <- asScalaSetConverter(jsonObjects.keySet()).asScala) { //currentObjKey is our own name for the obj to create
      val currentObj = jsonObjects.getJSONObject(currentObjKey); //The skillobject to create
      val currentType = currentObj.getString("type"); //The type of the skillObject

      instantiations =instantiations.concat("SkillObject " + currentObjKey + " = types.get(\"" + currentType + "\").make();\n");

      val attributes = currentObj.getJSONObject("attr"); //The attributes of the skillObject 

      for (currentAttrKey <- asScalaSetConverter(attributes.keySet()).asScala) {
        var currentAttrValue = "";
        if (attributes.optJSONArray(currentAttrKey) != null) {
          
          instantiations = instantiateArray(instantiations, attributes.getJSONArray(currentAttrKey), currentObjKey + "Array", currentType, currentAttrKey);
          currentAttrValue =  currentObjKey + "Array";
       
        } else if (attributes.optJSONObject(currentAttrKey) != null) {
          
          instantiations = instatiateMap(instantiations, attributes.getJSONObject(currentAttrKey), currentObjKey + "Map", currentType, currentAttrKey);
          currentAttrValue =  currentObjKey + "Map";
          
        } else if (attributes.opt(currentAttrKey) != null) {
          
          currentAttrValue = "\"" + attributes.get(currentAttrKey) + "\"";
          
        } else {

          currentAttrValue = "null";
        }
        instantiations = instantiations.concat(currentObjKey + ".set(cast(typeFieldMapping.get(" + currentType + ").get(" + currentAttrKey + ")), " + currentAttrValue + ");\n\n");

      }
    }
    instantiations = instantiations.concat("sf.close();\n");
    return instantiations;
  }

  def closeTestFile(out: java.io.PrintWriter) {
    out.write("""
}
""")
    out.close
  }

  override def makeGenBinaryTests(name: String) {
    locally {
      val out = newTestFile(name, "Read")
      closeTestFile(out)
    }
  }

  override def finalizeTests {
    // nothing yet
  }

  def instatiateMap(instantiations: String, map: JSONObject, valueName: String, valueType: String, attrKey: String): String = {
    var ins = instantiations.concat("\n");
    val attrType = "cast(typeFieldMapping.get(" + valueType + ").get(" + attrKey + "))";
    ins = ins.concat(attrType + " " + valueName + " = new " + attrType + ";\n");
    for (currentObjKey <- asScalaSetConverter(map.keySet()).asScala) {
      ins = ins.concat(valueName + ".put(" + currentObjKey + ", " + map.get(currentObjKey) + ");\n");
    }
    ins = ins.concat("\n");
    return ins;
  }

  def instantiateArray(instantiations: String, array: JSONArray, valueName: String, valueType: String, attrKey: String): String = {
    var ins = instantiations.concat("\n");
     val attrType = "cast(typeFieldMapping.get(" + valueType + ").get(" + attrKey + "))";
    ins = ins.concat(attrType + " " + valueName + " = new " + attrType + ";\n");
    for (x <- intWrapper(0) until array.length()) {
      ins = ins.concat(valueName + ".add(" + array.get(x) + ");\n");
    }
    ins = ins.concat("\n");
    return ins;
  }
}
