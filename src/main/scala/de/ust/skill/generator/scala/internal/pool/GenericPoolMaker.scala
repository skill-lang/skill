/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait GenericPoolMaker extends GeneralOutputMaker {
  override def make {
    super.make
    val out = open("internal/pool/GenericPool.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.pool

import ${packagePrefix}internal.UserType

/**
 * This kind of pool is used to carry all instances of unknown types.
 *
 * @note pretty much not implemented!
 *
 * @author Timm Felden
 */
final class GenericPool(
    userType: UserType,
    _superPool: Option[AbstractPool],
    blockCount: Int) extends AbstractPool(userType, blockCount) {

  private[internal] def superPool: Option[AbstractPool] = _superPool

  override def getByID(index: Long) = ???
}
""")

    //class prefix
    out.close()
  }
}
