package de.ust.skill.main

import java.io.File
import de.ust.skill.ir.TypeContext
import de.ust.skill.common.scala.api.Read
import de.ust.skill.common.scala.api.Write
import de.ust.skill.sir.api.SkillFile
import de.ust.skill.common.scala.api.ReadOnly
import de.ust.skill.common.scala.api.Create
import java.io.IOException

/**
 * Handler for a skill intermediate representation state.
 *
 * @author Timm Felden
 */
class SIRHandler private () {

  def this(path : String) {
    this()

    sf = SkillFile.open(path, Read, Write)
  }

  def this(tc : TypeContext, path : String) {
    this()
    try {
      new File(path).delete()
    } catch {
      case e : IOException ⇒ // not important
    }
    sf = SkillFile.open(path, Create, Write)

    // copy type context to file
    ???
  }

  private var sf : SkillFile = null

}