package de.ust.skill.parser

import de.ust.skill.ir

object ParseException {
  def apply(msg: String):Nothing = throw new ir.ParseException(msg);
}
