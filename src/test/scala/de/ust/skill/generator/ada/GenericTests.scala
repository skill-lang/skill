/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.ada

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter

import scala.collection.mutable.ArrayBuffer
import scala.reflect.io.Directory
import scala.reflect.io.Path.jfile2path

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import de.ust.skill.generator.common
import de.ust.skill.parser.Name
import de.ust.skill.main.CommandLine

/**
 * Generic tests built for scala.
 * Generic tests have an implementation for each programming language, because otherwise deleting the generated code
 * up-front would be ugly.
 *
 * @author Timm Felden
 */
@RunWith(classOf[JUnitRunner])
class GenericTests extends common.GenericTests {
  override def language = "ada"

  override def callMainFor(name : String, source : String) {
    CommandLine.main(Array[String](source,
      "--debug-header",
      "-L", "ada",
      "-p", name, 
      "-o", "testsuites/ada/src/" + name))
  }

  val tests = new ArrayBuffer[Name]()

  def makeGenBinaryTests(__name : String) {
    val (accept, reject) = collectBinaries(__name)
    implicit val name = new Name(__name, true, true)
    tests.append(name)
    def cStyle(implicit name : Name) = name.lowercase
    def adaStyle(implicit name : Name) = name.lowercase.capitalize
    def file2ID(f : File) = f.getName.replaceAll("\\W", "_");

    // test setup
    locally {
      val out = newFile(cStyle, s"test_$cStyle.ads")
      out.print(s"""package Test_$adaStyle is

end Test_$adaStyle;""")
      out.close
    }

    // generate [[test]]-generic.ads
    locally {
      val out = newFile(cStyle, s"test_$cStyle-gen_test.ads")
      out.print(s"""with Ahven.Framework;
with $adaStyle.Api;

package Test_$adaStyle.Gen_Test is

   type Test is new Ahven.Framework.Test_Case with null record;

   procedure Initialize (T : in out Test);

${
        (for (f ← accept ++ reject)
          yield s"""
   procedure Read_${file2ID(f)};""").mkString
      }

end Test_$adaStyle.Gen_Test;""")
      out.close
    }

    // generate [[test]]-generic.adb
    locally {
      val out = newFile(cStyle, s"test_$cStyle-gen_test.adb")
      out.print(s"""with Skill.Errors;

package body Test_$adaStyle.Gen_Test is

   procedure Initialize (T : in out Test) is
   begin
      Set_Name (T, "Test_$adaStyle.Gen_Test");
${
        (
          (for (f ← accept)
            yield s"""
      Ahven.Framework.Add_Test_Routine (T, Read_${file2ID(f)}'Access, "read (accept): ${f.getName}");""")
          ++
          (for (f ← reject) yield s"""
      Ahven.Framework.Add_Test_Routine (T, Read_${file2ID(f)}'Access, "read (reject): ${f.getName}");""")
        ).mkString
      }
   end Initialize;

${
        (for (f ← accept)
          yield s"""
   procedure Read_${file2ID(f)} is
      State : Standard.$adaStyle.Api.File := Standard.$adaStyle.Api.Open("../../${f.getPath}");
   begin
      State.Check;
      State.Free;
   end Read_${file2ID(f)};
""").mkString
      }${
        (for (f ← reject)
          yield s"""
   procedure Read_${file2ID(f)} is
      State : Standard.$adaStyle.Api.File;
   begin
      State := Standard.$adaStyle.Api.Open("../../${f.getPath}");
      State.Check;
      State.Free;
      Ahven.Fail("expected an exception to be thrown");
   exception
      when E : Skill.Errors.Skill_Error => null;
   end Read_${file2ID(f)};
""").mkString
      }
end Test_$adaStyle.Gen_Test;""")
      out.close
    }
  }

  override def finalizeTests {
    // rebuild test.adb
    val f = new File(s"testsuites/ada/test/test.adb")
    f.getParentFile.mkdirs
    f.createNewFile
    val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))

    for (t ← tests.sortWith(_.lowercase < _.lowercase))
      out.println(s"with Test_${t.lowercase.capitalize}.Gen_Test;")

    out.print(s"""
package body Test is
   procedure Add_Generic_Testes (Suite : in out Ahven.Framework.Test_Suite) is
   begin
${
      (for (t ← tests.sortWith(_.lowercase < _.lowercase)) yield s"      Ahven.Framework.Add_Test (Suite, new Test_${t.lowercase.capitalize}.Gen_Test.Test);").mkString("\n")
    }
   end Add_Generic_Testes;
end Test;""")

    out.close
  }

  override def deleteOutDir(out : String) {
    import scala.reflect.io.Directory
    Directory(new File("testsuites/ada/src/", out)).deleteRecursively
  }

  /**
   * create a file for a test
   */
  def newFile(testName : String, fileName : String) = {
    val f = new File(s"testsuites/ada/test/$testName/$fileName")
    f.getParentFile.mkdirs
    f.createNewFile
    new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8")))
  }
}
