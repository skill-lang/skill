/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-15 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.common

object KnownGenerators {
  import de.ust.skill.generator._

  val all = List[Class[_ <: Generator]](
    classOf[ada.Main],
    classOf[c.Main],
    classOf[cpp.Main],
    classOf[doxygen.Main],
    classOf[java.Main],
    classOf[scala.Main],
    classOf[skill.Main],
    classOf[statistics.Main],
    classOf[javaforeign.Main]
  )
}
