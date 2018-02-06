/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.common

import de.ust.skill.io.PrintingService
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.FieldLike
import de.ust.skill.ir.TypeContext
import de.ust.skill.main.HeaderInfo
import de.ust.skill.ir.UserType
import scala.collection.mutable.HashMap
import de.ust.skill.ir.Type
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.MapType

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
  var visitors : Array[UserType] = _;

  /**
   * The generated binding will also contain visitors for given types
   */
  val visited = new HashMap[String, UserType];

  /**
   * Turns skill types into typeIDs
   *
   * @note obviously, not recursive in container types
   * @note obviously, there is no typeID for user-defined types
   */
  def typeID(t : Type) : Int = t match {
    case t : GroundType ⇒ t.getSkillName match {
      case "annotation" ⇒ 5
      case "bool"       ⇒ 6
      case "i8"         ⇒ 7
      case "i16"        ⇒ 8
      case "i32"        ⇒ 9
      case "i64"        ⇒ 10
      case "v64"        ⇒ 11
      case "f32"        ⇒ 12
      case "f64"        ⇒ 13
      case "string"     ⇒ 14
    }
    case t : ConstantLengthArrayType ⇒ 15
    case t : VariableLengthArrayType ⇒ 17
    case t : ListType                ⇒ 18
    case t : SetType                 ⇒ 19
    case t : MapType                 ⇒ 20

    case t                           ⇒ throw new IllegalArgumentException(s"$t has no type id")
  }
}
