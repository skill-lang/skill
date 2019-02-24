/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.python

import java.io._

import de.ust.skill.generator.common
import de.ust.skill.main.CommandLine
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Generic tests built for Java.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * upfront would be ugly.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GenericTests extends common.GenericTests {

  override val language = "python"

  override def deleteOutDir(out : String) {
  }

  override def callMainFor(name : String, source : String, options : Seq[String]) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-c",
      "-L", language,
      "-p", name,
      "-o", "testsuites/python/src/main/python/") ++ options)
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    val f = new File(s"testsuites/python/src/test/python/$packagePath/Generic${name}Test.java")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""
from unittest import Testcase
from tempfle import TemporaryFile
from src.test.de.ust.skill.generator.common.CommonTest import CommonTest
from $packagePath.api.SkillFile import SkillFile

from src.api.SkillException import SkillException
from src.api.SkillFile import SkillFile.Mode
from src.internal.ForceLazyFields import ForceLazyFields
from src.internal.SkillObject import SkillObject


class Generic${name}Test(CommonTest, Testcase):
    \"\"\"
    Tests the file reading capabilities.
    \"\"\"
    def test_read(self, s):
        return SkillFile.open("../../" + s, Mode.Read, Mode.ReadOnly)

    def test_writeGeneric(self):
        path = tmpFile("write.generic")
        try:
            sf = SkillFile.open(path))
            self.reflectiveInit(sf)

    def test_writeGenericChecked(self):
        path = tmpFile("write.generic.checked")

        // create a name -> type map
        types = dict()
        try:
            sf = SkillFile.open(path))
            self.reflectiveInit(sf);

            for t in sf.allTypes():
                types.put(t.name(), t)

        // read file and check skill IDs
        sf2 = SkillFile.open(path, Mode.Read);
        for t in sf2.allTypes():
            os = types.get(t.name()).iterator()
            for o in t:
                assertTrue("to few instances in read stat", os.hasNext())
                assertEquals(o.getSkillID(), os.next().getSkillID())
""")
    rval
  }

  def closeTestFile(out : java.io.PrintWriter) {
    out.write("""
""")
    out.close()
  }

  override def makeTests(name : String) {
    val (accept, reject) = collectBinaries(name)

    // generate read tests
    val out = newTestFile(name, "Read")

    for (f ← accept) out.write(s"""
    def test_${name}_read_accept_${f.getName.replaceAll("\\W", "_")}(self):
        try:
            sf = read("${f.getPath.replaceAll("\\\\", "\\\\\\\\")}")
            self.assertIsNotNone(sf)
""")
    for (f ← reject) out.write(s"""
    def test_${name}_read_reject_${f.getName.replaceAll("\\W", "_")}(self):
        try:
            sf = read("${f.getPath.replaceAll("\\\\", "\\\\\\\\")}")
            ForceLazyFields.forceFullCheck(sf);
            Assert.fail("Expected ParseException to be thrown");
        except SkillException:
            # success
""")
    closeTestFile(out)
  }

  override def finalizeTests() {
    // nothing yet
  }
}
