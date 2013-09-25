package de.ust.skill.generator.scala.api

import de.ust.skill.generator.scala.GeneralOutputMaker

trait KnownTypeMaker extends GeneralOutputMaker {
  override def make {
    super.make
    val out = open("api/KnownType.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}api

/**
 * A virtual head of the generated type hierarchy.
 * This type is used to make the interface a bit more usable.
 *
 * @author Timm Felden
 */
trait KnownType {

  /**
   * provides a pretty representation of this
   */
  def prettyString: String
}""")

    out.close()
  }
}
