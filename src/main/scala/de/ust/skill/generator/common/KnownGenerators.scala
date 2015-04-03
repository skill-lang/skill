package de.ust.skill.generator.common

object KnownGenerators {
  import de.ust.skill.generator._

  val all = List[Class[_ <: Generator]](
    classOf[ada.Main],
    classOf[c.Main],
    classOf[doxygen.Main],
    classOf[java.Main],
    classOf[scala.Main],
    classOf[statistics.Main]
  )
}