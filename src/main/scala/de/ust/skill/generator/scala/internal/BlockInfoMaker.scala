package de.ust.skill.generator.scala.internal

import de.ust.skill.generator.scala.GeneralOutputMaker

trait BlockInfoMaker extends GeneralOutputMaker{
  override def make {
    super.make
    val out = open("internal/BlockInfo.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal

/**
 * This data contains information about the logical layout of indices found in the deserialized file.
 * The BlockInfo structure is created by the file parser and used by iterators and during pool creation.
 *
 * @param count the number of instances of this type (including subtypes)
 * @param bpsi the index into the base pool data store
 *
 * @note bpsi == 0 <==> count == 0
 *
 * @author Timm Felden
 */
class BlockInfo(var count: Long, var bpsi: Long) {}""")

    //class prefix
    out.close()
  }
}
