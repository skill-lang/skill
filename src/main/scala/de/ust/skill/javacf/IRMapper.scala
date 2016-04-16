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
  
  /**
   * Takes a list of class names and returns a TypeContext representing containing the IR of those types.
   */
  def mapClasses(list: List[String]): TypeContext = {
    list.foreach(collectType)
    tc
  }

  def collectType(name: String): UserType = collectType(loadType(name))

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

  def loadType(name: String): CtClass = pool.get(name)

  def mapFields(clazz: CtClass): List[Field] = {
    clazz.getFields.map { field ⇒
      {
        val typ = field.getType.getName match {
          case "java.lang.Boolean" | "boolean" ⇒ tc.get("bool")
          case "java.lang.Byte" | "byte" ⇒ tc.get("i8")
          case "java.lang.Short" | "short" ⇒ tc.get("i16")
          case "java.lang.Integer" | "int" ⇒ tc.get("i32")
          case "java.lang.Long" | "long" ⇒ tc.get("i64")
          case "java.lang.Float" | "float"  ⇒ tc.get("f32")
          case "java.lang.Double" | "double" ⇒ tc.get("f64")
          case "java.lang.String" ⇒ tc.get("string")
          case _ ⇒ collectType(field.getType)
        }
        new Field(typ, new Name(List(field.getName).asJava, field.getName), false, new Comment(), new java.util.ArrayList[Restriction], new java.util.ArrayList[Hint])
      }
    }.to
  }
}
