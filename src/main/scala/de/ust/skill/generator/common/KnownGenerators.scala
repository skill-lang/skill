/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
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
    classOf[ecore.Main],
    classOf[haskell.Main],
    classOf[java.Main],
    classOf[jforeign.Main],
    classOf[python.Main],
    classOf[scala.Main],
    classOf[sidl.Main],
    classOf[skill.Main],
    classOf[statistics.Main]
  )
}
