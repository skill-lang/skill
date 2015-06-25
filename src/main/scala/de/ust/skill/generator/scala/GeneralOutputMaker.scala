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
import de.ust.skill.ir.MapType
import de.ust.skill.ir.Field
import java.util.Date
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.io.FileOutputStream

/**
 * The parent class for all output makers.
 *
 * @author Timm Felden
 */
trait GeneralOutputMaker {

  val ArrayTypeName = "scala.Array"
  val VarArrayTypeName = "scala.collection.mutable.ArrayBuffer"
  val ListTypeName = "scala.collection.mutable.ListBuffer"
  val SetTypeName = "scala.collection.mutable.HashSet"
  val MapTypeName = "scala.collection.mutable.HashMap"
  
  def arrayTypeName(t : Type) = {
    t match {
      case t : Declaration ⇒ s"_root_.${packagePrefix}internal.RefArray[${mapType(t)}]"
      case t if t.getSkillName == "annotation" ⇒ s"_root_.${packagePrefix}internal.AnnotationArray"
      case _ ⇒ s"scala.Array[${mapType(t)}]"
    }
  }
  
  def varArrayTypeName(t : Type) = {
    t match {
      case t : Declaration ⇒ s"_root_.${packagePrefix}internal.RefArrayBuffer[${mapType(t)}]"
      case t if t.getSkillName == "annotation" ⇒ s"_root_.${packagePrefix}internal.AnnotationArrayBuffer"
      case _ ⇒ s"scala.collection.mutable.ArrayBuffer[${mapType(t)}]"
    }
  }
  
  def listTypeName(t : Type) = {
    t match {
      case t : Declaration ⇒ s"_root_.${packagePrefix}internal.RefListBuffer[${mapType(t)}]"
      case t if t.getSkillName == "annotation" ⇒ s"_root_.${packagePrefix}internal.AnnotationListBuffer"
      case _ ⇒ s"scala.collection.mutable.ListBuffer[${mapType(t)}]"
    }
  }
  
  def setTypeName(t : Type) = {
    t match {
      case t : Declaration ⇒ s"_root_.${packagePrefix}internal.RefHashSet[${mapType(t)}]"
      case t if t.getSkillName == "annotation" ⇒ s"_root_.${packagePrefix}internal.AnnotationHashSet"
      case _ ⇒ s"scala.collection.mutable.HashSet[${mapType(t)}]"
    }
  }
  
  def mapTypeName(types : scala.collection.mutable.Buffer[Type]) = {
    @inline def typeAndNameOf(t : Type) = t match {
      case t if t.getSkillName == "annotation" ⇒ ("Annotation", "")
      case t : Declaration ⇒ ("Ref", mapType(t))
      case _ ⇒ ("Basic", mapType(t))
    }
    def collapse(t : Type, rest : scala.collection.mutable.Buffer[Type]) : (String, String) = {
      if (rest.isEmpty)
        typeAndNameOf(t)
      else {
        val first = typeAndNameOf(t)
        val second = collapse(rest.head, rest.tail)
        val mapType = if (first._1 == second._1) first._1 else first._1 + second._1
        val args = if (first._2 == "" && second._2 == "") "" else s"[${first._2}${if (first._2 != "" && second._2 != "") ", "}${second._2}]"
        ("Map", s"_root_.${packagePrefix}internal.${mapType}MapView$args")
      }
    }
    collapse(types.head, types.tail)._2
  }

  /**
   * The base path of the output.
   */
  var outPath : String

  /**
   * The intermediate representation of the (known) output type system.
   */
  var IR : List[Declaration]

  /**
   * Makes the output; has to invoke super.make!!!
   */
  def make : Unit;

  private[scala] def header : String

  /**
   * Creates the correct PrintWriter for the argument file.
   */
  protected def open(path : String) = {
    val f = new File(s"$outPath$packagePath$path")
    f.getParentFile.mkdirs
    f.createNewFile
    val rval = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
      new FileOutputStream(f), "UTF-8")))
    rval.write(header)
    rval
  }

  /**
   * Assume the existence of a translation function for types.
   */
  protected def mapType(t : Type) : String

  /**
   * Assume the existence of a translation function for type representations.
   */
  protected def mapTypeRepresentation(t : Type) : String

  /**
   * creates argument list of a constructor call, not including potential skillID or braces
   */
  protected def makeConstructorArguments(t : Declaration) : String
  /**
   * creates argument list of a constructor call, including a trailing comma for insertion into an argument list
   */
  protected def appendConstructorArguments(t : Declaration) : String

  /**
   * turns a declaration and a field into a string writing that field into an outStream
   * @note the used iterator is "outData"
   * @note the used target OutStream is "dataChunk"
   */
  protected def writeField(d : Declaration, f : Field) : String

  /**
   * Assume a package prefix provider.
   */
  protected def packagePrefix() : String
  protected def packageName = packagePrefix.substring(0, packagePrefix.length - 1)

  /**
   * Provides a string representation of the default value of f.
   */
  protected def defaultValue(f : Field) : String

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  protected def escaped(target : String) : String

  private lazy val packagePath = if (packagePrefix.length > 0) {
    packagePrefix.replace(".", "/")
  } else {
    ""
  }
}
