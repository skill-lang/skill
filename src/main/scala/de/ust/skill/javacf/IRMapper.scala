package de.ust.skill.javacf

import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import de.ust.skill.ir.Comment
import de.ust.skill.ir.Field
import de.ust.skill.ir.Hint
import de.ust.skill.ir.Name
import de.ust.skill.ir.Restriction
import de.ust.skill.ir.TypeContext
import de.ust.skill.ir.UserType
import javassist.ClassPool
import javassist.CtClass
import de.ust.skill.ir.Type

/**
 * Maps classes by name from a given classpath to IR representation.
 *
 * @author Constantin Weißer
 */
class IRMapper(classpaths: List[String]) {

  val pool = new ClassPool(true)
  classpaths.foreach { pool.appendClassPath }

  val tc = new TypeContext

  val javaObjectType = pool.get("java.lang.Object")

  val allTypes = new HashSet[UserType]

  val knownClasses = new HashMap[CtClass, UserType];
  
  val boolt = CtClass.booleanType
  val Boolt = pool.get("java.lang.Boolean")
  val bytet = CtClass.byteType
  val Bytet = pool.get("java.lang.Byte")
  val shortt = CtClass.shortType
  val Shortt = pool.get("java.lang.Short")
  val intt = CtClass.intType
  val Intt = pool.get("java.lang.Integer")
  val longt = CtClass.longType
  val Longt = pool.get("java.lang.Long")
  val floatt = CtClass.floatType
  val Floatt = pool.get("java.lang.Float")
  val doublet = CtClass.doubleType
  val Doublet = pool.get("java.lang.Double")
  val stringt = pool.get("java.lang.String")

  /**
   * Takes a list of class names and returns a TypeContext representing containing the IR of those types.
   */
  def mapClasses(list: List[String]): TypeContext = {
    list.foreach(collectType)
    tc
  }

  def collectType(name: String): UserType = collectType(loadType(name))

  /**
   * Returns the IR type for a class and possibly also adds its super types to the type context.
   */
  def collectType(clazz: CtClass): UserType = {
    if (knownClasses contains clazz) return knownClasses(clazz)

    println(clazz.getName)
    val superclazz = clazz.getSuperclass
    // recursively find all supertypes
    val supertype = if (superclazz != javaObjectType) collectType(superclazz) else null

    // add declaration
    val ntype = UserType.newDeclaration(tc, new Name(List(clazz.getName).asJava, clazz.getName), new Comment(), new java.util.ArrayList, new java.util.ArrayList)
    knownClasses += (clazz → ntype)
   
    // map all fields for this class
    val fields = mapFields(clazz)
    ntype.initialize(supertype, List().asJava, fields.asJava)
    
    ntype
  }

  /**
   * Loads a CtClass for a class name from the class path.
   */
  def loadType(name: String): CtClass = pool.get(name)
  
  def mapType(clazz: CtClass): Type = {
        clazz match {
          case `boolt` | `Boolt` ⇒ tc.get("bool")
          case `bytet` | `Bytet` ⇒ tc.get("i8")
          case `shortt` | `Shortt` ⇒ tc.get("i16")
          case `intt` | `Intt` ⇒ tc.get("i32")
          case `longt` | `Longt` ⇒ tc.get("i64")
          case `floatt` | `Floatt` ⇒ tc.get("f32")
          case `doublet` | `Doublet` ⇒ tc.get("f64")
          case `stringt` ⇒ tc.get("string")
          case other : CtClass  ⇒ collectType(other)
        }
  }

  /**
   * Returns the IR fields for a given class.
   */
  def mapFields(clazz: CtClass): List[Field] = {
    clazz.getFields.map { field ⇒ new Field(mapType(field.getType), new Name(List(field.getName).asJava, field.getName),
        false, new Comment(), new java.util.ArrayList[Restriction], new java.util.ArrayList[Hint]) }.to
  }
}
