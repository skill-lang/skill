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

public class GenericJsonTest extends common.CommonTest {

	@Rule //http://stackoverflow.com/a/2935935
	public final ExpectedException exception = ExpectedException.none();

	private static Path path;
	private JSONObject currentJSON;

	/**
	 * Tests the object generation capabilities.
	 */
	@BeforeClass
	public static void init() throws JSONException, MalformedURLException, IOException {
		path = Paths.get(java.lang.System.getProperty("user.dir"), 
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
		
		Path binaryFile = createFile("write.generic.${testname}");
		SkillFile sf = SkillFile.open(binaryFile);
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

    if (content.getBoolean("shouldFail")) {
      instantiations = instantiations.concat(
        """			java.lang.System.out.println("There should be an exception coming up!");
			exception.expect(Exception.class);
      """);
    }

    //First create skillobjects
    var referenceInstantiations = "";
    for (currentObjKey <- asScalaSetConverter(jsonObjects.keySet()).asScala) { //currentObjKey is the name of the skillobj to create

      if (jsonObjects.optJSONObject(currentObjKey) == null) { //if value is not a JSONObject value must be a reference to an existing object
        referenceInstantiations = referenceInstantiations.concat("SkillObject " + currentObjKey.toLowerCase() + " = " + jsonObjects.getString(currentObjKey) + ";\n");
      } else {
        val currentObj = jsonObjects.getJSONObject(currentObjKey); //The skillobject to create
        val currentObjType = currentObj.getString("type"); //The type of the skillObject

        instantiations = instantiations.concat("SkillObject " + currentObjKey.toLowerCase() + " = types.get(\"" + currentObjType.toLowerCase() + "\").make();\n");
      }
    }
    instantiations = instantiations.concat(referenceInstantiations);
    instantiations = instantiations.concat("\n")
    //Set skillobject values
    for (currentObjKey <- asScalaSetConverter(jsonObjects.keySet()).asScala) { //currentObjKey is the name of the skillobj to create
      if (jsonObjects.optJSONObject(currentObjKey) != null) {

        val currentObj = jsonObjects.getJSONObject(currentObjKey); //The skillobject to create
        val currentObjType = currentObj.getString("type").toLowerCase(); //The type of the skillObject
        val objAttributes = currentObj.getJSONObject("attr"); //The attributes/Fields of the skillObject 

        for (currentAttrKey <- asScalaSetConverter(objAttributes.keySet()).asScala) {

          val currentAttrValue = getAttributeValue(objAttributes, currentAttrKey, currentObjKey, currentObjType);

          if (objAttributes.optJSONArray(currentAttrKey) != null) {

            instantiations = instantiateArray(instantiations, objAttributes.getJSONArray(currentAttrKey), currentObjType, currentAttrKey.toLowerCase(), currentAttrValue);

          } else if (objAttributes.optJSONObject(currentAttrKey) != null) {

            instantiations = instantiateMap(instantiations, objAttributes.getJSONObject(currentAttrKey), currentObjType, currentAttrKey.toLowerCase(), currentAttrValue);

          }

          instantiations = instantiations.concat(currentObjKey.toLowerCase() + ".set(cast(typeFieldMapping.get(\"" + currentObjType.toLowerCase() + "\").get(\"" + currentAttrKey.toLowerCase() + "\")), " + currentAttrValue + ");\n\n");

        }
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
      val out = newTestFile(name, "Json")
      closeTestFile(out)
    }
  }

  override def finalizeTests {
    // nothing yet
  }

  /**
    * Generate code for instantiating a map with the given variable name and fill it using the data contained in the
    * parameter 'map'
    *
    * @param instantiations previously generated code of the test suite
    * @param map JSON object containing data with which the resulting instantiated map will be filled.
    * @param objValueType name of SKilL class which is being processed
    * @param attrKey name of map attribute of the SKilL class
    * @param mapName variable name the instantiated map should have
    * @return generated test suite code with added code for instantiating the provided map.
    */
  def instantiateMap(instantiations: String, map: JSONObject, objValueType: String, attrKey: String, mapName: String): String = {
    var ins = instantiations.concat("\n");

    ins = ins.concat("HashMap " + mapName + " = new HashMap<>();\n");   //Declaration

    for (currentObjKey <- asScalaSetConverter(map.keySet()).asScala) {
      var key = currentObjKey;
      if (currentObjKey.contains("\"")) {                               //True: interpret 'currentObjKey' as string
        key = "wrapPrimitveMapTypes(" + currentObjKey + ", typeFieldMapping.get(\"" + objValueType.toLowerCase() + "\").get(\"" + attrKey.toLowerCase() + "\"),true)";
      }                                                                 //False: interpret 'currentObjKey' as reference

      var value = map.get(currentObjKey).toString();
      if (map.get(currentObjKey).toString().contains("\"")) {           //True: interpret value as string
        value = "wrapPrimitveMapTypes(" + map.get(currentObjKey).toString() + ", typeFieldMapping.get(\"" + objValueType.toLowerCase() + "\").get(\"" + attrKey.toLowerCase() + "\"),false)";
      }                                                                 //False: interpret value as reference

      ins = ins.concat(mapName + ".put(" + key + ", " + value + ");\n");  //Code for adding key-value-pair
    }
    ins = ins.concat("\n");
    return ins;
  }

  /**
    * Generate code for instantiating an array. The code is appended to the template code provided in the parameter
    * 'instantiations' and then returned.
    *
    * The generated code constructs a collection using reflection and adds all elements of the array in parameter
    * 'array' afterwards.
    * @param instantiations previously generated code of the test suite
    * @param array JSON type array to be instantiated
    * @param objValueType name of SKilL class which is being processed
    * @param attrKey name of attribute of the SKilL class for which this array is instantiated
    * @param collectionName variable name of the collection in which this array shall be instantiated. The elements of
    *                       the JSON array are added to the collection referenced by this name after it has been created
    *                       using reflection.
    * @return generated test suite code with added code for instantiating the provided array.
    */
  def instantiateArray(instantiations: String, array: JSONArray, objValueType: String, attrKey: String, collectionName: String): String = {
    var ins = instantiations.concat(s"""
    refClass = Class.forName(getProperCollectionType(typeFieldMapping.get("${objValueType}").get("${attrKey}").toString()));
    refConstructor = refClass.getConstructor();
    Collection ${collectionName} = (Collection) refConstructor.newInstance();
    """);
    for (x <- intWrapper(0) until array.length()) {
      ins = ins.concat(collectionName + ".add(" + getArrayElementValue(array, x, attrKey, objValueType) + ");\n");
    }
    ins = ins.concat("\n");
    return ins;
  }

  /**
    * Generates code for statements yielding the values of an attribute of a SKilL object
    *
    * In case of collections, maps and strings this is a reference to the corresponding object.
    * In case of booleans it is 'true' or 'false' as string.
    * In case of doubles, floats, longs and ints this is an object wrapping the actual, primitive value.
    * @param attributes 'attr' object of a SKilL object. This is a map of SKilL object attribute names to their
    *                   corresponding values.
    * @param currentAttrKey name of the attribute from which the value will be retrieved
    * @param currentObjKey name of SKilL class which is being processed
    * @param currentObjType name of attribute of the SKilL class from which the value will be read
    * @return generated code for a statement yielding the value of the specified attribute
    */
  def getAttributeValue(attributes: JSONObject, currentAttrKey: String, currentObjKey: String, currentObjType: String): String = {

    if (attributes.optJSONArray(currentAttrKey) != null) {

      return currentObjKey.toLowerCase() + currentAttrKey + "Collection";

    } else if (attributes.optJSONObject(currentAttrKey) != null) {

      return currentObjKey.toLowerCase() + currentAttrKey.toLowerCase() + "Map";

    } else if (attributes.optBoolean(currentAttrKey) ||
      (!attributes.optBoolean(currentAttrKey) && !attributes.optBoolean(currentAttrKey, true))) {

      return attributes.getBoolean(currentAttrKey).toString();

    } else if (attributes.optDouble(currentAttrKey, 2009) != 2009) {

      return "wrapPrimitveTypes(" + attributes.getDouble(currentAttrKey).toString() + ", typeFieldMapping.get(\"" + currentObjType + "\").get(\"" + currentAttrKey.toLowerCase() + "\"))";

    } else if (attributes.optLong(currentAttrKey, 2009) != 2009) {

      return "wrapPrimitveTypes(" + attributes.getLong(currentAttrKey).toString() + ", typeFieldMapping.get(\"" + currentObjType + "\").get(\"" + currentAttrKey.toLowerCase() + "\"))";

    } else if (!attributes.optString(currentAttrKey).isEmpty()) {
      return attributes.getString(currentAttrKey).toLowerCase();

    } else {

      return "null";
    }
  }

  /**
    * Generates code for statements yielding the value of the array element at position 'currentObj'. Primitive types
    * are automatically wrapped into their corresponding wrapper objects in the generated code. Therefore the generated
    * code statements always yield an object.
    * @param array array from which the element to be yielded is read
    * @param currentObj index of the array element
    * @param currentObjType name of the SKilL class which uses the array elements
    * @param currentAttrKey name of the attribute of that SKilL class which uses the array elements
    * @return generated code for a statement yielding an object instantiation of the specified array element
    */
  def getArrayElementValue(array: JSONArray, currentObj: Int, currentAttrKey: String, currentObjType: String): String = {

    if (array.optBoolean(currentObj) ||
      (!array.optBoolean(currentObj) && !array.optBoolean(currentObj, true))) {

      return array.getBoolean(currentObj).toString();

    } else if (array.optDouble(currentObj, 2009) != 2009) {

      return "wrapPrimitveTypes(" + array.getDouble(currentObj).toString() + ", typeFieldMapping.get(\"" + currentObjType + "\").get(\"" + currentAttrKey.toLowerCase() + "\"))";

    } else if (array.optLong(currentObj, 2009) != 2009) {

      return "wrapPrimitveTypes(" + array.getLong(currentObj).toString() + ", typeFieldMapping.get(\"" + currentObjType + "\").get(\"" + currentAttrKey.toLowerCase() + "\"))";

    } else if (!array.optString(currentObj).isEmpty()) {
      return array.getString(currentObj).toLowerCase();

    } else {

      return "null";
    }
  }

  /**
    * Generates code for statement yielding the field declaration for the attribute of the given SKilL object.
    * @param objectType SKilL object which owns the attribute
    * @param attributeKey attribute for which to get the corresponding field declaration
    * @return generated code for a statement yielding the FieldDeclaration<?, ?> object of the specified object and
    *         attribute
    */
  def getFieldDeclaration(objectType: String, attributeKey: String): String = {
    return "typeFieldMapping.get(\"" + objectType + "\").get(\"" + attributeKey.toLowerCase() + "\")";
  }
}
