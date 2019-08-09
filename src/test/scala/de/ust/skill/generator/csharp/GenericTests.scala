/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.csharp

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
 * Generic tests built for C#.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * upfront would be ugly.
 *
 * @author Simon Glaub, Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GenericTests extends common.GenericTests {

  override val language = "csharp"

  override def deleteOutDir(out : String) {
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-c",
      "-L", "csharp",
      "-p", name,
      "-d", "testsuites/csharp/lib",
      "-o", "testsuites/csharp/src/main/csharp/") ++ options)
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    val f = new File(s"testsuites/csharp/src/test/csharp/$packagePath/Generic${name}Test.cs")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""using System.Collections;
using System.Collections.Generic;
using System.IO;

using NUnit.Framework;

using IAccess = de.ust.skill.common.csharp.api.IAccess;
using SkillException = de.ust.skill.common.csharp.api.SkillException;
using Mode = de.ust.skill.common.csharp.api.Mode;
using ForceLazyFields = de.ust.skill.common.csharp.@internal.ForceLazyFields;
using SkillObject = de.ust.skill.common.csharp.@internal.SkillObject;
using SkillFile = $packagePath.api.SkillFile;

namespace $packagePath
{

    /// <summary>
    /// Tests the file reading capabilities.
    /// </summary>
    [TestFixture]
    public class Generic${name}Test : common.CommonTest {
        public SkillFile read(string s) {
            return SkillFile.open(basePath + s, Mode.Read, Mode.ReadOnly);
        }

        [Test]
        public void writeGeneric() {
            string path = tmpFile("write.generic");
            SkillFile sf = SkillFile.open(path);
            reflectiveInit(sf);
            sf.close();
            File.Delete(path);
        }

        [Test]
        public void writeGenericChecked() {
            string path = tmpFile("write.generic.checked");
            SkillFile sf = SkillFile.open(path);
            reflectiveInit(sf);
            // write file
            sf.flush();

            // create a name -> type map
            Dictionary<string, IAccess> types = new Dictionary<string, IAccess>();
            foreach (IAccess t in sf.allTypes())
                types[t.Name] = t;

            // read file and check skill IDs
            SkillFile sf2 = SkillFile.open(path, Mode.Read);
            foreach (IAccess t in sf2.allTypes()) {
                IEnumerator os = types[t.Name].GetEnumerator();
                foreach (SkillObject o in t) {
                    Assert.IsTrue(os.MoveNext(), "to few instances in read state");
                    Assert.AreEqual(o.SkillID, ((SkillObject)os.Current).SkillID);
                }
                Assert.IsFalse(os.MoveNext(), "to many instances in read state");
            }
            File.Delete(path);
        }
""")
    rval
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
    }
}
""")
    out.close
  }

  override def makeTests(name : String) {
    val (accept, reject) = collectBinaries(name)

    // generate read tests
    val out = newTestFile(name, "Read")

    for (f ← accept) out.write(s"""
        [Test]
        public void test_${name}_read_accept_${f.getName.replaceAll("\\W", "_")}() {
            Assert.IsNotNull(read("${f.getPath.replaceAll("\\\\", "\\\\\\\\")}"));
        }
""")
    for (f ← reject) out.write(s"""
        [Test]
        public void test_${name}_read_reject_${f.getName.replaceAll("\\W", "_")}() {
            try {
                ForceLazyFields.forceFullCheck(read("${f.getPath.replaceAll("\\\\", "\\\\\\\\")}"));
                Assert.Fail("Expected ParseException to be thrown");
            } catch (SkillException e) {
                return; // success
            }
        }
""")
    closeTestFile(out)
  }

  override def finalizeTests {
    // nothing yet
  }
}
