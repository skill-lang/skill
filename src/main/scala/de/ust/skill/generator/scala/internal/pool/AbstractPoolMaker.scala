/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013 University of Stuttgart                    **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.generator.scala.internal.pool

import java.io.PrintWriter
import de.ust.skill.generator.scala.GeneralOutputMaker

trait AbstractPoolMaker extends GeneralOutputMaker {
  abstract override def make {
    super.make
    val out = open("internal/pool/AbstractPool.scala")
    //package & imports
    out.write(s"""package ${packagePrefix}internal.pool

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer

import ${packagePrefix}api.SkillType
import ${packagePrefix}internal._

/**
 * The super type of all storage pools. This type is necessary in order to have invariant type parameters at storage
 *  pools together with the ability to treat storage pools without knowing anything about the stored type.
 *
 *  @note pool interfaces are called just "XPool" instead of "XStoragePool" in order to avoid name clashes
 *
 *  TODO needs to be abstract, requires generic pools
 *
 *  @param name the name of the type represented by this pool
 *  @param fields the known fields of this type
 */
abstract class AbstractPool(val name: String, private[internal] val fields: HashMap[String, FieldDeclaration])
    extends TypeInfo {
  private[internal] def superPool: Option[AbstractPool];

  /**
   * the next pool regarding type order; for example A<:B, B<:D, A<:C may lead to A⇀B⇀D⇀C or A⇀C⇀B⇀D.
   */
  private[internal] var next: AbstractPool = superPool match {
    case None    ⇒ null
    case Some(p) ⇒ p.next
  }
  // we stole super's next, so we have to set ourselves as next
  superPool.foreach(_.next = this)

  /**
   * the sub pools are constructed during construction of all storage pools of a state
   */
  protected var subPools = new ArrayBuffer[AbstractPool];
  // update sub-pool relation
  if (superPool.isDefined) {
    superPool.get.subPools += this
  }
  def getSubPools = subPools

  /**
   * returns the skill object at position index
   */
  def getByID(index: Long): SkillType

  /**
   * the number of instances of exactly this type, excluding sub-types
   */
  def staticSize: Long = 0
  /**
   * the number of instances of this type, including sub-types
   * @note this is an O(t) operation, where t is the number of sub-types
   */
  final def dynamicSize: Long = subPools.map(_.dynamicSize).fold(staticSize)(_ + _)

  /**
   * The block layout of data.
   */
  private[internal] val blockInfos = new ListBuffer[BlockInfo]()

  /**
   * implementation of the TypeInfo interface
   */
  private[internal] var typeIndex: Long = -33L // results in typeID -1
  final override def typeId = 32L + typeIndex

  final override def hashCode = name.hashCode
  final override def equals(obj: Any) = obj match {
    case NamedUserType(n) ⇒ n == name
    case t: UserType      ⇒ t.name == name
    case t: AbstractPool  ⇒ t.name == name
    case _ ⇒ false
  }
}
""")

    //class prefix
    out.close()
  }
}
