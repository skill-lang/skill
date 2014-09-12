/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.doxygen

/**
 * Creates user type equivalents.
 *
 * @author Timm Felden
 */
trait UserTypeMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open(s"""${packagePrefix}-api.ads""")

    out.write(s"""
--
--  This package is empty, but necessary for the compiler.
--

package ${packagePrefix.capitalize}.Api is

end ${packagePrefix.capitalize}.Api;
""")

    out.close()
  }
}
