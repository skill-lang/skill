/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.skill

import scala.collection.JavaConverters.asScalaBufferConverter

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

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * Skill Specification pretty printing.
 *
 * @author Timm Felden
 */
class Main extends FakeMain
  with SpecificationMaker {

  lineLength = 80
  override def comment(d : Declaration) : String = d.getComment.format("/**\n", " * ", lineLength, " */\n")
  override def comment(f : FieldLike) : String = f.getComment.format("/**\n", "   * ", lineLength, "   */\n  ")

  override def packageDependentPathPostfix = ""
  override def defaultCleanMode = "none";

  /**
   * Translates the types into Ada types.
   */
  override protected def mapType(t : Type) : String = t match {
    case t : GroundType              ⇒ t.getSkillName
    case t : ConstantLengthArrayType ⇒ s"${mapType(t.getBaseType)}[${t.getLength}]"
    case t : VariableLengthArrayType ⇒ s"${mapType(t.getBaseType)}[]"
    case t : ListType                ⇒ s"list<${mapType(t.getBaseType)}>"
    case t : SetType                 ⇒ s"set<${mapType(t.getBaseType)}>"
    case t : MapType                 ⇒ t.getBaseTypes.asScala.mkString("map<", ", ", ">")
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

  override def setOption(option : String, value : String) {
    option match {
      case "drop" ⇒ value match {
        case "interfaces" ⇒ droppedKinds += Interfaces
        case "typedefs"   ⇒ droppedKinds += Typedefs
        case "enums"      ⇒ droppedKinds += Enums
        case "all"        ⇒ droppedKinds ++= Seq(Interfaces, Typedefs, Enums)
      }
      case unknown ⇒ sys.error(s"unkown Argument: $unknown")
    }
  }

  /**
   * stats do not require any escaping
   */
  override def escaped(target : String) : String = target;

  override def helpText : String = """
drop = (interfaces|enums|typedefs|views|all)
          drops the argument kind from the specification, defaults is none
"""

  override def customFieldManual : String = "will keep all custom fields as-is"

  // unused
  override protected def defaultValue(f : Field) = throw new NoSuchMethodError
}
