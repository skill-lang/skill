/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-18 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.doxygen

import scala.collection.JavaConversions.asScalaBuffer

import de.ust.skill.ir.ConstantLengthArrayType
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.FieldLike
import de.ust.skill.ir.GroundType
import de.ust.skill.ir.ListType
import de.ust.skill.ir.MapType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.Type
import de.ust.skill.ir.VariableLengthArrayType
import de.ust.skill.main.HeaderInfo

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * The used language is C++ (or rather something that doxygen recoginezes as C++).
 *
 * @note Using C++ has the effect, that neither interfaces nor enums with fields can be represented correctly.
 * @note None of the languages supported by doxygen seems to provide all required features. We may fix this by
 * switching to scaladoc or by hoping that doxygen will support scala or swift eventually.
 *
 * @author Timm Felden
 */
class Main extends FakeMain
    with EnumTypeMaker
    with InterfaceTypeMaker
    with TypedefMaker
    with UserTypeMaker {

  lineLength = 80
  override def comment(d : Declaration) : String =
    d.getComment.format("/*!\n", " * ", lineLength, " */\n").replace('<', '⟨').replace('>', '⟩')
  override def comment(f : FieldLike) : String =
    f.getComment.format("    /*!\n", "     * ", lineLength, "     */\n").replace('<', '⟨').replace('>', '⟩')

  override def packageDependentPathPostfix = ""
  override def defaultCleanMode = "wipe";

  /**
   * Translates the types into Ada types.
   */
  override protected def mapType(t : Type) : String = t match {
    case t : GroundType ⇒ t.getName.lower match {
      case "annotation" ⇒ "void*"

      case "bool"       ⇒ "bool"

      case "i8"         ⇒ "int8_t"
      case "i16"        ⇒ "int16_t"
      case "i32"        ⇒ "int32_t"
      case "i64"        ⇒ "int64_t"
      case "v64"        ⇒ "v64"

      case "f32"        ⇒ "float"
      case "f64"        ⇒ "double"

      case "string"     ⇒ "std::string"
    }

    case t : ConstantLengthArrayType ⇒ s"${mapType(t.getBaseType())}[${t.getLength()}]"
    case t : VariableLengthArrayType ⇒ s"${mapType(t.getBaseType())}[]"
    case t : ListType                ⇒ s"std::list<${mapType(t.getBaseType())}>"
    case t : SetType                 ⇒ s"std::set<${mapType(t.getBaseType())}>"
    case t : MapType                 ⇒ t.getBaseTypes().map(mapType(_)).reduceRight { (n, r) ⇒ s"std::map<$n, $r>" }

    case t : Declaration             ⇒ escaped(t.getName.capital)
  }

  /**
   * Provides the package prefix.
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    _packagePrefix = names.foldRight("")(_ + "." + _)
  }

  override def makeHeader(headerInfo : HeaderInfo) : String = headerInfo.format(this, "/*", "*\\", " *", "* ", "\\*", "*/")

  override def setOption(option : String, value : String) {
    // no options
  }
  override def helpText : String = ""

  override def customFieldManual : String = """(unsupported)"""

  // unused
  override protected def defaultValue(f : Field) = throw new NoSuchMethodError

  /**
   * Tries to escape a string without decreasing the usability of the generated identifier.
   *
   * Delegates to c++ escaping, because the generated code is parsed as c++.
   */
  val escaper = new de.ust.skill.generator.cpp.Main
  final def escaped(target : String) : String = {
    escaper.escaped(target)
  }
}
