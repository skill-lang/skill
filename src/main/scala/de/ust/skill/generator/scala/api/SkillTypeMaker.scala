package de.ust.skill.generator.scala.api

import de.ust.skill.generator.scala.GeneralOutputMaker

trait SkillTypeMaker extends GeneralOutputMaker {
  override def make {
    super.make
    val out = open("api/SkillType.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}api

/**
 * The top of the skill type hierarchy.
 * @author Timm Felden
 */
trait SkillType {

  /**
   * provides a pretty representation of this
   */
  def prettyString: String

}
""")

    out.close()
  }
}
