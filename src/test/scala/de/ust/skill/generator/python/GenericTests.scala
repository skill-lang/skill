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
 * Generic tests built for Python.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * upfront would be ugly.
 *
 * @author Alexander Maisch
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
      "-o", "testsuites/python/src/") ++ options)
  }

  def newTestFile(packagePath : String, name : String) : PrintWriter = {
    val f = new File(s"testsuites/python/src/$packagePath/Generic${name}Test.py")
    f.getParentFile.mkdirs
    if (f.exists)
      f.delete
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    rval.write(s"""
from unittest import TestCase
from tempfile import TemporaryFile
from python.src.$packagePath.api import *
from python.src.common.CommonTest import CommonTest


class Generic${name}Test(TestCase, CommonTest):
    \"\"\"
    Tests the file reading capabilities.
    \"\"\"
    def read(self, s):
        return SkillFile.open("../../../../" + s, Mode.Read, Mode.ReadOnly)

    def test_writeGeneric(self):
        path = self.tmpFile("write.generic")

        sf = SkillFile.open(path.name)
        self.reflectiveInit(sf)

    def test_writeGenericChecked(self):
        path = self.tmpFile("write.generic.checked")

        # create a name -> type map
        types = dict()
        sf = SkillFile.open(path.name)
        self.reflectiveInit(sf)

        for t in sf.allTypes():
            types[t.name()] = t

        # read file and check skill IDs
        sf2 = SkillFile.open(path.name, Mode.Read)
        for t in sf2.allTypes():
            os = types.get(t.name()).__iter__()
            for o in t:
                self.assertTrue("to few instances in read stat", os.hasNext())
                self.assertEquals(o.getSkillID(), os.next().getSkillID())
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
        sf = self.read("${f.getPath.replaceAll("\\\\", "\\\\\\\\")}")
        self.assertIsNotNone(sf)
""")
  /**  for (f ← reject) out.write(s"""
    def test_${name}_read_reject_${f.getName.replaceAll("\\W", "_")}(self):
        try:
            sf = self.read("${f.getPath.replaceAll("\\\\", "\\\\\\\\")}")
            self.assertRaises()
            ForceLazyFields.forceFullCheck(sf)
            Assert.fail("Expected ParseException to be thrown")
        except SkillException:
            # success
""")**/
    closeTestFile(out)
  }

  override def finalizeTests() {
    // nothing yet
  }
}
