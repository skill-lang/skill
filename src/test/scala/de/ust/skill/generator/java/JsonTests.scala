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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
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

public class GenericReadTest extends common.CommonTest {

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
    def testname = new File(testfile).getName.replace(".json", "");
    rval.write(s"""
	@Test
	public void ${testname}Test() throws Exception  {
    Class<?> refClass;
    Constructor<?> refConstructor;Map<String, Access<?>> types = new HashMap<>();
		Map<String, HashMap<String, FieldDeclaration<?, ?>>> typeFieldMapping = new HashMap<>();
		
		Path tempBinaryFile = tmpFile("write.generic.checked");
		SkillFile sf = SkillFile.open(tempBinaryFile);
        reflectiveInit(sf);
        
		creator.SkillObjectCreator.generateSkillFileMappings(sf, types, typeFieldMapping);
    
    //auto-generated instansiation from json

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
    
    if(content.getBoolean("shouldFail")){
      instantiations = instantiations.concat(
      """			System.out.println("There should be an exception coming up!");
			exception.expect(Exception.class);
      """);
    }

    //First create skillobjects
    for (currentObjKey <- asScalaSetConverter(jsonObjects.keySet()).asScala) { //currentObjKey is the name of the skillobj to create
      val currentObj = jsonObjects.getJSONObject(currentObjKey); //The skillobject to create
      val currentObjType = currentObj.getString("type"); //The type of the skillObject

      instantiations = instantiations.concat("SkillObject " + currentObjKey.toLowerCase() + " = types.get(\"" + currentObjType.toLowerCase() + "\").make();\n");
    }
    instantiations = instantiations.concat("\n")
    //Set skillobject values
    for (currentObjKey <- asScalaSetConverter(jsonObjects.keySet()).asScala) { //currentObjKey is the name of the skillobj to create
      val currentObj = jsonObjects.getJSONObject(currentObjKey); //The skillobject to create
      val currentObjType = currentObj.getString("type").toLowerCase(); //The type of the skillObject
      val objAttributes = currentObj.getJSONObject("attr"); //The attributes/Fields of the skillObject 

      for (currentAttrKey <- asScalaSetConverter(objAttributes.keySet()).asScala) {

        val currentAttrValue = getcurrentAttrValue(objAttributes, currentAttrKey, currentObjKey, currentObjType);

        if (objAttributes.optJSONArray(currentAttrKey) != null) {

          instantiations = instantiateArray(instantiations, objAttributes.getJSONArray(currentAttrKey), currentObjType, currentAttrKey.toLowerCase(),currentAttrValue);

        } else if (objAttributes.optJSONObject(currentAttrKey) != null) {

          instantiations = instatiateMap(instantiations, objAttributes.getJSONObject(currentAttrKey), currentObjType, currentAttrKey.toLowerCase(),currentAttrValue);

        }

        instantiations = instantiations.concat(currentObjKey + ".set(cast(typeFieldMapping.get(\"" + currentObjType + "\").get(\"" + currentAttrKey.toLowerCase() + "\")), " + currentAttrValue + ");\n\n");

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

  def instatiateMap(instantiations: String, map: JSONObject, valueType: String, attrKey: String, mapName : String): String = {
    var ins = instantiations.concat("\n");
    ins = ins.concat("HashMap " + mapName + " = new HashMap<>();\n");
    for (currentObjKey <- asScalaSetConverter(map.keySet()).asScala) {
      ins = ins.concat(mapName + ".put(" + currentObjKey + ", " + map.get(currentObjKey) + ");\n");
    }
    ins = ins.concat("\n");
    return ins;
  }

  def instantiateArray(instantiations: String, array: JSONArray, objValueType: String, attrKey: String, collectionName : String): String = {
  var ins = instantiations.concat(s"""
refClass = Class.forName(getProperCollectionType(typeFieldMapping.get("${objValueType}").get("${attrKey}").toString()));
refConstructor = refClass.getConstructor();
Collection ${collectionName} = (Collection) refConstructor.newInstance();
""");
    for (x <- intWrapper(0) until array.length()) {
      ins = ins.concat(collectionName +".add(" + array.get(x) + ");\n");
    }
    ins = ins.concat("\n");
    return ins;
  }

  def getcurrentAttrValue(attributes: JSONObject, currentAttrKey: String, currentObjKey: String, currentObjType: String): String = {

    if (attributes.optJSONArray(currentAttrKey) != null) {

      return currentObjKey.toLowerCase() + currentAttrKey + "Collection";

    } else if (attributes.optJSONObject(currentAttrKey) != null) {

      return currentObjKey.toLowerCase() + currentAttrKey.toLowerCase() + "Map";

    } else if (attributes.optBoolean(currentAttrKey) ||
      (!attributes.optBoolean(currentAttrKey) && !attributes.optBoolean(currentAttrKey, true))) {

      return attributes.getBoolean(currentAttrKey).toString();

    }  else if (attributes.optDouble(currentAttrKey, 2009) != 2009) {

      return "wrapPrimitveTypes(" + attributes.getDouble(currentAttrKey).toString()+ ", typeFieldMapping.get(\""+ currentObjType +"\").get(\""+ currentAttrKey.toLowerCase() +"\"))";

    } else if (attributes.optLong(currentAttrKey, 2009) != 2009) {

      return "wrapPrimitveTypes(" + attributes.getLong(currentAttrKey).toString() + ", typeFieldMapping.get(\"" + currentObjType +"\").get(\""+ currentAttrKey.toLowerCase() +"\"))";

    } else if (!attributes.optString(currentAttrKey).isEmpty()) {
      return attributes.getString(currentAttrKey);

    } else {

      return "null";
    }
  }
}
