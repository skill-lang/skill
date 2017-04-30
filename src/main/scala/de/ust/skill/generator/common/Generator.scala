/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.common

import de.ust.skill.io.PrintingService
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.FieldLike
import de.ust.skill.ir.TypeContext
import de.ust.skill.main.HeaderInfo

/**
 * every code generator shares these properties.
 */
trait Generator {

  /**
   * returns the generators output language.
   */
  def getLanguageName : String;

  /**
   * Improved source printing to be used by a generator.
   * Instantiation is performed before invocation of the generator.
   */
  var files : PrintingService = _;

  /**
   * This string will be prepended to the output directory of files.
   * It is evaluated after setting the package and before creating files.
   */
  def packageDependentPathPostfix : String;

  /**
   * Create the header submitted to the PrintingService
   */
  def makeHeader(headerInfo : HeaderInfo) : String;

  /**
   * Base path of dependencies copied by this generator.
   */
  var depsPath : String = _;
  /**
   * request the code generator to skip copying of dependencies
   * @note this is useful, for instance, as part of code regeneration in a build
   * system where dependencies and specification are managed by the version control system
   */
  var skipDependencies = false;

  /**
   * Set the type context. This is a function to make clear that generators may in fact project a type context prior to
   * using it.
   */
  def setTC(tc : TypeContext) : Unit;

  /**
   * Set output package/namespace/...
   * This is a list of Strings, each denoting a package.
   *
   * This correpsonds to the -p option.
   */
  def setPackage(names : List[String]) : Unit;

  /**
   * Sets an option to a new value.
   */
  def setOption(option : String, value : String) : Unit;

  /**
   * The help text for language specific options.
   *
   * If the text is the empty string, no options are provided by this generator.
   * Hence, it is omitted in option parsing.
   */
  def helpText : String;

  /**
   * Returns the custom field manual for this generator.
   */
  def customFieldManual : String;

  /**
   * Provides a string representation of the default value of f.
   */
  protected def defaultValue(f : Field) : String;

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   */
  def escaped(target : String) : String;

  /**
   * Escapes words, that appear without prefix or suffix.
   */
  def escapedLonely(target : String) : String = escaped(target)

  /**
   * maximum line length in emitted output
   */
  var lineLength = 80

  /**
   * Transform a comment of a declaration into the language's comment system
   */
  protected def comment(d : Declaration) : String;

  /**
   * Transform a comment of a field into the language's comment system
   */
  protected def comment(d : FieldLike) : String;

  /**
   * Makes the output. Use trait stacking, i.e. traits must invoke super.make!!!
   *
   * This function is called after options have been set.
   */
  def make : Unit;

  /**
   * The clean mode preferred by this back-end.
   */
  def defaultCleanMode : String;

  /**
   * The generated binding will also contain visitors for given types
   */
  var visitors : Seq[String] = _;
}
