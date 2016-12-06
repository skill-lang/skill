/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.statistics

import de.ust.skill.ir.Declaration
import de.ust.skill.ir.Field
import de.ust.skill.ir.FieldLike
import de.ust.skill.ir.Type

/**
 * Fake Main implementation required to make trait stacking work.
 */
abstract class FakeMain extends GeneralOutputMaker { def make {} }

/**
 * Skill specification pretty printing
 *
 * @author Timm Felden
 */
class Main extends FakeMain
    with CSVMaker {

  override def comment(d : Declaration) : String = throw new NoSuchMethodError
  override def comment(f : FieldLike) : String = throw new NoSuchMethodError

  /**
   * Translates the types into Ada types.
   */
  override protected def mapType(t : Type) : String = t.getName.capital

  /**
   * Provides the package prefix.
   */
  override protected def packagePrefix() : String = _packagePrefix
  private var _packagePrefix = ""

  override def setPackage(names : List[String]) {
    _packagePrefix = names.foldRight("")(_ + "." + _)
  }

  override def setOption(option : String, value : String) {}
  override def helpText : String = ""

  /**
   * stats do not require any escaping
   */
  override def escaped(target : String) : String = target;

  override def customFieldManual : String = "Custom fields will be ignored."

  // unused
  override protected def defaultValue(f : Field) = throw new NoSuchMethodError
}
