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
class GenericTests extends common.GenericTests {

  override val language = "java"

  override def deleteOutDir(out : String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/java/src/main/java", out)).deleteRecursively
  }

  override def callMainFor(name : String, source : String) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-L", "java",
      "-p", name,
      "-Ojava:SuppressWarnings=true",
      "-d", "testsuites/java/lib",
      "-o", "testsuites/java/src/main/java/"))
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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import $packagePath.api.SkillFile;

import de.ust.skill.common.java.api.Access;
import de.ust.skill.common.java.api.SkillFile.Mode;
import de.ust.skill.common.java.internal.SkillObject;
import de.ust.skill.common.java.internal.ParseException;

/**
 * Tests the file reading capabilities.
 */
@SuppressWarnings("static-method")
public class Generic${name}Test extends common.CommonTest {
    public SkillFile read(String s) throws Exception {
        return SkillFile.open("../../" + s);
    }

    @Test
    public void writeGeneric() throws Exception {
        Path path = tmpFile("write.generic");
        SkillFile sf = SkillFile.open(path);
        reflectiveInit(sf);
        sf.close();
    }

    @Test
    public void writeGenericChecked() throws Exception {
        Path path = tmpFile("write.generic.checked");
        SkillFile sf = SkillFile.open(path);
        reflectiveInit(sf);
        // write file
        sf.flush();

        // create a name -> type map
        Map<String, Access<? extends SkillObject>> types = new HashMap<>();
        for (Access<?> t : sf.allTypes())
            types.put(t.name(), t);

        // read file and check skill IDs
        SkillFile sf2 = SkillFile.open(path, Mode.Read);
        for (Access<?> t : sf2.allTypes()) {
            Iterator<? extends SkillObject> os = types.get(t.name()).iterator();
            for (SkillObject o : t) {
                Assert.assertTrue("to few instances in read stat", os.hasNext());
                Assert.assertEquals(o.getSkillID(), os.next().getSkillID());
            }
            Assert.assertFalse("to many instances in read stat", os.hasNext());
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
    val (accept, reject) = collectBinaries(name)

    // generate read tests
    locally {
      val out = newTestFile(name, "Read")

      for (f ← accept) out.write(s"""
    @Test
    public void test_${name}_read_accept_${f.getName.replaceAll("\\W", "_")}() throws Exception {
        Assert.assertNotNull(read("${f.getPath}"));
    }
""")
      for (f ← reject) out.write(s"""
    @Test(expected = ParseException.class)
    public void test_${name}_read_reject_${f.getName.replaceAll("\\W", "_")}() throws Exception {
        Assert.assertNotNull(read("${f.getPath}"));
    }
""")
      closeTestFile(out)
    }

    //    mit generischem binding sf parsen um an zu erwartende daten zu kommen

    //    mit parser spec parsen um an lesbare daten zu kommen:)

    //    test generieren, der sicherstellt, dass sich die daten da raus lesen lassen

  }

  override def finalizeTests {
    // nothing yet
  }
}
