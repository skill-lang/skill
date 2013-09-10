package de.ust.skill.generator.scala

import java.io.File
import java.io.PrintWriter
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Type

/**
 * The parent class for all output makers.
 * 
 * @author Timm Felden
 */
trait GeneralOutputMaker {
  
  /**
   * The base path of the output.
   */
  var outPath:String
  
  /**
   * The intermediate representation of the (known) output type system.
   */
  var IR:List[Declaration]
  
  /**
   * Makes the output; has to invoke super.make!!!
   */
  protected def make{}
  
  /**
   * Creates the correct PrintWriter for the argument file.
   */
  def open(path:String) = {
    val f = new File(s"$outPath$packagePath$path")
    f.getParentFile.mkdirs
    f.createNewFile
    new PrintWriter(f)
  }

  /**
   * Assume the existence of a translation function for types.
   */
  protected def _T(t: Type): String

  /**
   * Assume template copy functionality.
   */
  protected def copyFromTemplate(out: PrintWriter, template: String): Unit

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix(): String
  
  private lazy val packagePath = if(packagePrefix.length > 0){
    packagePrefix.replace(".", "/")
  } else {
    ""
  }
}