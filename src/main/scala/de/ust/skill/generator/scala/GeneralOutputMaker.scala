/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala

import java.io.File
import java.io.PrintWriter
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Type
import de.ust.skill.ir.Field
import java.util.Date

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden
 */
trait GeneralOutputMaker {

  /**
   * The base path of the output.
   */
  var outPath: String

  /**
   * The intermediate representation of the (known) output type system.
   */
  var IR: List[Declaration]

  /**
   * Makes the output; has to invoke super.make!!!
   */
  protected def make {}

  /**
   * Creates the correct PrintWriter for the argument file.
   */
  protected def open(path: String) = {
    val f = new File(s"$outPath$packagePath$path")
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(f)
    // make header
    val date = (new java.text.SimpleDateFormat("dd.MM.yyyy")).format(new Date)
    val userImageLength = 47
    val userName = System.getProperty("user.name").padTo(userImageLength, " ").mkString
    rval.write(s"""/*  ___ _  ___ _ _                                                            *\\
** / __| |/ (_) | |       Your SKilL Scala Binding                            **
** \\__ \\ ' <| | | |__     generated: $date                               **
** |___/_|\\_\\_|_|____|    by: $userName **
\\*                                                                            */
""")
    rval
  }

  /**
   * Assume the existence of a translation function for types.
   */
  protected def mapType(t: Type): String

  /**
   * Assume template copy functionality.
   */
  protected def copyFromTemplate(out: PrintWriter, template: String): Unit

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix(): String

  /**
   * Provides a string representation of the default value of f.
   */
  protected def defaultValue(f: Field): String

  private lazy val packagePath = if (packagePrefix.length > 0) {
    packagePrefix.replace(".", "/")
  } else {
    ""
  }
}
