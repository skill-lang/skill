package de.ust.skill.generator.common

import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Type
import java.io.PrintWriter

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

  /**
   * Use this to create a 5 lines header that looks similar in all languages.
   *
   * This corresponds to the -hN, -u, -license, -date options.
   */
  case class HeaderInfo(
    line1 : Option[String],
    line2 : Option[String],
    line3 : Option[String],
    license : Option[String],
    userName : Option[String],
    date : Option[String])
  var headerInfo : HeaderInfo = _;

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
   * Provides a string representation of the default value of f.
   *
   * TODO add declaration and field, to account for @default
   */
  protected def defaultValue(t : Type) : String;

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  protected def escaped(target : String) : String;

  /**
   * Create a new file with a default header.
   */
  protected def open(path : String) : PrintWriter;

  /**
   * Makes the output. Use trait stacking, i.e. traits must invoke super.make!!!
   *
   * This function is called after options have been set.
   */
  def make : Unit;
}