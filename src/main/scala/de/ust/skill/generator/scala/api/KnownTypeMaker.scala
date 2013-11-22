/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.api

import de.ust.skill.generator.scala.GeneralOutputMaker

trait KnownTypeMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("api/KnownType.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}api

import ${packagePrefix}internal.InternalInstanceProperties

/**
 * The top of the known types hierarchy.
 *
 * @author Timm Felden
 */
trait KnownType extends SkillType with InternalInstanceProperties;
""")

    out.close()
  }
}
