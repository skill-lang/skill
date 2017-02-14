/*  ___ _  ___ _ _                                                            *\
** / __| |/ (_) | |       The SKilL Generator                                 **
** \__ \ ' <| | | |__     (c) 2013-16 University of Stuttgart                 **
** |___/_|\_\_|_|____|    see LICENSE                                         **
\*                                                                            */
package de.ust.skill.jforeign

import scala.collection.mutable.HashMap

import de.ust.skill.ir.Field
import de.ust.skill.ir.Type
import javassist.CtClass

class ReflectionContext {

  private val reflectionMap : HashMap[Type, CtClass] = new HashMap

  private val fieldReflectionMap : HashMap[Field, CtClass] = new HashMap

  def add(t : Type, clazz : CtClass) { reflectionMap += (t → clazz) }

  def add(f : Field, clazz : CtClass) { fieldReflectionMap += (f → clazz) }

  def map(t : Type) : CtClass = reflectionMap(t)

  def map(f : Field) : CtClass = fieldReflectionMap(f)

}
