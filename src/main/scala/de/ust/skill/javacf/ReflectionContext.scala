package de.ust.skill.javacf

import scala.collection.mutable.HashMap
import javassist.CtClass
import de.ust.skill.ir.Type
import de.ust.skill.ir.Field

class ReflectionContext {

  private val reflectionMap: HashMap[Type, CtClass] = new HashMap
  
  private val fieldReflectionMap: HashMap[Field, CtClass] = new HashMap
  
  def add(t: Type, clazz: CtClass): Unit = reflectionMap += (t → clazz);

  def add(f: Field, clazz: CtClass): Unit = fieldReflectionMap += (f → clazz);
  
  def map(t: Type): CtClass = reflectionMap(t)

  def map(f: Field): CtClass = fieldReflectionMap(f)
  
}