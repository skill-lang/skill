package de.ust.skill.jforeign

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
import sun.reflect.generics.parser.SignatureParser
import javassist.NotFoundException

/**
 * Maps classes by name from a given classpath to IR representation.
 *
 * @author Constantin Weißer
 */
class IRMapper(classpaths: List[String]) {

  val pool = new ClassPool(true)
  classpaths.foreach { pool.appendClassPath }

  val tc = new TypeContext

  /** Types we've seen before. */
  val knownTypes = new HashMap[CtClass, UserType];

  /** Types that are already mapped. */
  val mappedTypes = new HashMap[CtClass, UserType];

  /** Reflection context keeps the mapping from IR types back to java types. */
  val rc = new ReflectionContext

  /** Basic types that must always be around. */
  val javaObjectType = pool.get("java.lang.Object")

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
  def mapClasses(list: List[String]): (TypeContext, ReflectionContext) = {
    list.foreach(collect)
    // map all given types
    knownTypes.keys.foreach(translateType)
    val decls = mappedTypes.values.map { _.asInstanceOf[Declaration] }.toList
    tc.setDefs(decls.asJava)
    (tc, rc)
  }

  /**
   * Collect a type and its transitive supertype closure.
   */
  def collect(name: String): UserType = try {
    collect(loadType(name))
  } catch {
    case e: NotFoundException ⇒
      throw new RuntimeException(s"IR-Mapper cannot load class $name: not found in any given class path:\n${classpaths.mkString("\n")}");
  }

  /**
   * Collect a type and its transitive supertype closure.
   */
  def collect(clazz: CtClass): UserType = if (knownTypes contains clazz) knownTypes(clazz)
  else {
    // add declaration
    val comment = new Comment()
    comment.init(List().asJava)

    val ntype = UserType.newDeclaration(tc, new Name(List(clazz.getSimpleName).asJava, clazz.getSimpleName, clazz.getPackageName),
      comment, new java.util.ArrayList, new java.util.ArrayList)
    rc.add(ntype, clazz)
    knownTypes += (clazz → ntype)

    if (clazz.getSuperclass != javaObjectType) collect(clazz.getSuperclass)
    ntype
  }

  /**
   * Loads a CtClass for a class name from the class path.
   */
  def loadType(name: String): CtClass = pool.get(name)

  def mapType(clazz: CtClass): Option[Type] = clazz match {
    case `boolt` | `Boolt` ⇒ Some(tc.get("bool"))
    case `bytet` | `Bytet` ⇒ Some(tc.get("i8"))
    case `shortt` | `Shortt` ⇒ Some(tc.get("i16"))
    case `intt` | `Intt` ⇒ Some(tc.get("i32"))
    case `longt` | `Longt` ⇒ Some(tc.get("i64"))
    case `floatt` | `Floatt` ⇒ Some(tc.get("f32"))
    case `doublet` | `Doublet` ⇒ Some(tc.get("f64"))
    case `stringt` ⇒ Some(tc.get("string"))
    case `javaObjectType` ⇒ None
    case other: CtClass ⇒ knownTypes.get(other)
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
      skilltype.initialize(supertype, List().asJava, mapFields(clazz).filter(_.isDefined).map(_.get).asJava,
          List().asJava, List().asJava)
      mappedTypes += (clazz → skilltype)
      skilltype
    }
  }

  /**
   * Returns the IR fields for a given class.
   */
  def mapFields(clazz: CtClass): List[Option[Field]] = clazz.getDeclaredFields.map { field ⇒
    val javatype = field.getType
    mapType(javatype).orElse({
      val signature = field.getGenericSignature
      if (signature != null) {
        val sigparser = SignatureParser.make();
        val fieldsig = sigparser.parseClassSig(signature)
        val sigvisitor = new SignatureVisitor(tc, pool, mapType)
        fieldsig.accept(sigvisitor)
        Option(sigvisitor.getResult())
      } else {
        None
      }
    }).map { t =>
      val comment = new Comment()
      comment.init(List().asJava)
      val f: Field = new Field(t, new Name(List(field.getName).asJava, field.getName),
        false, comment, new java.util.ArrayList[Restriction], new java.util.ArrayList[Hint])
      rc.add(f, field.getType)
      f
    }
  }.toList
}
