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
import de.ust.skill.ir.Declaration
import de.ust.skill.ir.InterfaceType

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

  val knownTypes = new HashMap[CtClass, UserType];

  val mappedTypes = new HashMap[CtClass, UserType];

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

  val ilistt = pool.get("java.util.List")
  val isett = pool.get("java.util.Set")
  val imapt = pool.get("java.util.Map")

  /**
   * Takes a list of class names and returns a TypeContext representing containing the IR of those types.
   */
  def mapClasses(list: List[String]): TypeContext = {
    list.foreach(collect)
    // map all given types
    knownTypes.keys.foreach(translateType)
    val decls = mappedTypes.values.map { _.asInstanceOf[Declaration] }.toList
    tc.setDefs(decls.asJava)
    tc
  }

  /**
   * Collect a type and its transitive supertype closure.
   */
  def collect(name: String): UserType = collect(loadType(name))

  /**
   * Collect a type and its transitive supertype closure.
   */
  def collect(clazz: CtClass): UserType = if (knownTypes contains clazz) knownTypes(clazz)
  else {
    // add declaration
    val comment = new Comment()
    comment.init(List().asJava)

    val ntype = UserType.newDeclaration(tc, new Name(List(clazz.getSimpleName).asJava, clazz.getSimpleName), comment,
      new java.util.ArrayList, new java.util.ArrayList)
    knownTypes += (clazz → ntype)
    if (clazz.getSuperclass != javaObjectType) collect(clazz.getSuperclass)
    ntype
  }

  /**
   * Loads a CtClass for a class name from the class path.
   */
  def loadType(name: String): CtClass = pool.get(name)

  def mapType(clazz: CtClass): Type = clazz match {
    case `boolt` | `Boolt` ⇒ tc.get("bool")
    case `bytet` | `Bytet` ⇒ tc.get("i8")
    case `shortt` | `Shortt` ⇒ tc.get("i16")
    case `intt` | `Intt` ⇒ tc.get("i32")
    case `longt` | `Longt` ⇒ tc.get("i64")
    case `floatt` | `Floatt` ⇒ tc.get("f32")
    case `doublet` | `Doublet` ⇒ tc.get("f64")
    case `stringt` ⇒ tc.get("string")
    case other: CtClass ⇒ knownTypes.get(other).orNull
  }

  def translateType(clazz: CtClass): Type = {
    if (mappedTypes contains clazz) mappedTypes(clazz) else {
      // get supertype or null if has only Object as supertype
      val supertype: UserType = if (clazz.getSuperclass != javaObjectType) {
        val skillsupertype = translateType(clazz.getSuperclass)
        if (skillsupertype.isInstanceOf[UserType]) skillsupertype.asInstanceOf[UserType]
        else throw new RuntimeException(s"Cannot inherit from non-usertype ${skillsupertype.getName}")
      } else null

      val skilltype = knownTypes(clazz)
      skilltype.initialize(supertype, new java.util.ArrayList[InterfaceType], mapFields(clazz).asJava)
      mappedTypes += (clazz → skilltype)
      skilltype
    }
  }

  /**
   * Returns the IR fields for a given class.
   */
  def mapFields(clazz: CtClass): List[Field] = clazz.getDeclaredFields.map { field ⇒
    val javatype = field.getType
    val skilltype = mapType(javatype)

    if (skilltype == null) {
      if (javatype.getInterfaces.contains(ilistt)) {
        // type is a list
      } else if (javatype.getInterfaces.contains(isett)) {
        // type is a set
      } else if (javatype.getInterfaces.contains(imapt)) {
        // type is a map
      }
    }

    val comment = new Comment()
    comment.init(List().asJava)
    new Field(skilltype, new Name(List(field.getName).asJava, field.getName),
      false, comment, new java.util.ArrayList[Restriction], new java.util.ArrayList[Hint])
  }.to
}
