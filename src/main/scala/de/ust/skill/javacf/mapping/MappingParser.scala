package de.ust.skill.javacf.mapping

import scala.io.Source

class MappingParser {

  def parseFile(path: String): Mapping = {
    val mapping = new Mapping
    var current: TypeMapping = null

    for (line <- Source.fromFile(path).getLines) {
      if (line.trim.length > 0) {
        if (line.charAt(0) == '*') {
          if (current == null) throw new RuntimeException("Must map a type first!")
          val fmapping = line.substring(1)
          val parts = fmapping.split("->")
          if (parts.size != 2) throw new RuntimeException("Field mapping lines must be: onefield -> anotherfield")
          current.mapField(parts(0).trim(), parts(1).trim())
        } else {
          val parts = line.split("->")
          if (parts.size != 2) throw new RuntimeException("Type mapping lines must be: onetype -> anothertype")
          current = mapping.mapType(parts(0).trim, parts(1).trim)
        }
      }
    }
    mapping
  }

}