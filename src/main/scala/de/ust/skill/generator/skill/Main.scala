/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.skill

import de.ust.skill.ir._
import de.ust.skill.parser.Parser
import java.io.File
import scala.collection.JavaConversions._
import scala.collection.mutable.MutableList
import de.ust.skill.generator.common.Generator
import java.util.Date

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
    with SpecificationMaker {

  lineLength = 80
  override def comment(d : Declaration) : String = d.getComment.format("/**\n", " * ", lineLength, " */\n")
  override def comment(f : FieldLike) : String = f.getComment.format("/**\n", "   * ", lineLength, "   */\n  ")

  /**
   * Translates the types into Ada types.
   */
  override protected def mapType(t : Type) : String = t match {
    case t : GroundType              ⇒ t.getSkillName
    case t : ConstantLengthArrayType ⇒ s"${mapType(t.getBaseType)}[${t.getLength}]"
    case t : VariableLengthArrayType ⇒ s"${mapType(t.getBaseType)}[]"
    case t : ListType                ⇒ s"list<${mapType(t.getBaseType)}>"
    case t : SetType                 ⇒ s"set<${mapType(t.getBaseType)}>"
    case t : MapType                 ⇒ t.getBaseTypes.mkString("map<", ", ", ">")
    case t                           ⇒ t.getName.capital
  }

  /**
   * Provides the package prefix.
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    _packagePrefix = names.foldRight("")(_ + "." + _)
  }

  override def setOption(option : String, value : String) = option match {
    case "drop" ⇒ value match {
      case "interfaces" ⇒ droppedKinds += Interfaces
    }
    case unknown ⇒ sys.error(s"unkown Argument: $unknown")
  }

  /**
   * stats do not require any escaping
   */
  override def escaped(target : String) : String = target;

  override def helpText = """
drop = (interfaces|enums|typedefs|views|all)
          drops the argument kind from the specification, defaults is none
"""

  override def customFieldManual = "will keep all custom fields as-is"

  // unused
  override protected def defaultValue(f : Field) = throw new NoSuchMethodError
}
