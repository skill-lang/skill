package de.ust.skill.generator.common

import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Type
import java.io.PrintWriter
import de.ust.skill.ir.Field

/**
 * Use this to create a 5 lines header that looks similar in all languages.
 *
 * This corresponds to the -hN, -u, -license, -date options.
 *
 * @author Timm Felden
 */
case class HeaderInfo(
  var line1 : Option[String] = None,
  var line2 : Option[String] = None,
  var line3 : Option[String] = None,
  var license : Option[String] = None,
  var userName : Option[String] = None,
  var date : Option[String] = None)

/**
 * every code generator shares these properties.
 */
trait Generator {

  /**
   * returns the generators output language.
   */
  def getLanguageName : String;

  /**
   * Base path of the output for this generator.
   */
  var outPath : String = _;

  /**
   * set IR of the known specifation.
   */
  def setIR(IR : List[Declaration]);

  var headerInfo : HeaderInfo = HeaderInfo();

  /**
   * Set output package/namespace/...
   * This is a list of Strings, each denoting a package.
   *
   * This correpsonds to the -p option.
   */
  def setPackage(names : List[String]) : Unit;

  /**
   * Process options passed to this generator only.
   *
   * This corresponds to all --L $getLanguageName [...] -- options.
   */
  def setOptions(args : Array[String]) : Unit;

  /**
   * Prints help for language specific options.
   */
  def printHelp : Unit;

  /**
   * Provides a string representation of the default value of f.
   */
  protected def defaultValue(f : Field) : String;

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  protected def escaped(target : String) : String;

  /**
   * Create a new file with a default header.
   */
  protected def open(path : String) : PrintWriter;

  /**
   * Transform a comment of a declaration into the language's comment system
   */
  protected def comment(d : Declaration) : String;

  /**
   * Transform a comment of a field into the language's comment system
   */
  protected def comment(d : Field) : String;

  /**
   * Makes the output. Use trait stacking, i.e. traits must invoke super.make!!!
   *
   * This function is called after options have been set.
   */
  def make : Unit;
}