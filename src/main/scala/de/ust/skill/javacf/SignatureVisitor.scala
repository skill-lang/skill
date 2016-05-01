package de.ust.skill.javacf

import scala.collection.JavaConversions._
import de.ust.skill.ir.Type
import sun.reflect.generics.tree.LongSignature
import sun.reflect.generics.tree.FloatSignature
import sun.reflect.generics.tree.VoidDescriptor
import sun.reflect.generics.tree.BooleanSignature
import sun.reflect.generics.tree.DoubleSignature
import sun.reflect.generics.tree.ShortSignature
import sun.reflect.generics.tree.ClassTypeSignature
import sun.reflect.generics.tree.BottomSignature
import sun.reflect.generics.tree.IntSignature
import sun.reflect.generics.tree.TypeVariableSignature
import sun.reflect.generics.tree.ArrayTypeSignature
import sun.reflect.generics.tree.SimpleClassTypeSignature
import sun.reflect.generics.tree.FormalTypeParameter
import sun.reflect.generics.tree.CharSignature
import sun.reflect.generics.tree.ByteSignature
import sun.reflect.generics.tree.Wildcard
import javassist.CtClass
import sun.reflect.generics.tree.MethodTypeSignature
import sun.reflect.generics.tree.ClassSignature
import de.ust.skill.ir.TypeContext
import de.ust.skill.ir.ListType
import scala.collection.mutable.ArrayLike
import scala.collection.mutable.ListBuffer
import javassist.ClassPool
import scala.collection.mutable.HashMap
import de.ust.skill.ir.UserType
import de.ust.skill.ir.SetType
import de.ust.skill.ir.MapType
import javassist.NotFoundException

/**
 * This is a nasty stateful visitor (thanks to the lack of generic arguments to the visit methods).
 */
class SignatureVisitor(tc: TypeContext, pool: ClassPool, mapInUserContext: CtClass ⇒ Option[Type])
    extends sun.reflect.generics.visitor.Visitor[Type] {

  var topLevel: Boolean = true
  var typeargs = new ListBuffer[Type]
  var result: Type = null

  val utilPool = new ClassPool(true)
  utilPool.importPackage("java.util.*")
  val listt = utilPool.get("java.util.List")
  val sett = utilPool.get("java.util.Set")
  val mapt = utilPool.get("java.util.Map")

  override def getResult(): Type = { result }

  override def visitClassSignature(cs: ClassSignature): Unit = {
    cs.getSuperInterfaces.foreach { x => x.accept(this) }
    cs.getFormalTypeParameters.foreach { x => x.accept(this) }
    cs.getSuperclass.accept(this)
  }

  override def visitClassTypeSignature(ct: ClassTypeSignature): Unit = {
    ct.getPath.foreach { x => x.accept(this) }
  }

  override def visitSimpleClassTypeSignature(sct: SimpleClassTypeSignature): Unit = {
    if (topLevel) {
      topLevel = false
      val clazz = utilPool.get(sct.getName)
      if (clazz.subtypeOf(listt)) {
        sct.getTypeArguments.foreach { _.accept(this) }
        assert(typeargs.size == 1)
        result = ListType.make(tc, typeargs.get(0))
      } else if (clazz.subtypeOf(sett)) {
        sct.getTypeArguments.foreach { _.accept(this) }
        assert(typeargs.size == 1)
        result = SetType.make(tc, typeargs.get(0))
      } else if (clazz.subtypeOf(mapt)) {
        sct.getTypeArguments.foreach { _.accept(this) }
        result = MapType.make(tc, typeargs)
      }
    } else {
      try {
        val clazz = utilPool.get(sct.getName)
        if (clazz.subtypeOf(mapt)) {
          sct.getTypeArguments.foreach { _.accept(this) }
          return
        }
      } catch {
        case e: NotFoundException ⇒ // do nothing!
      }
      val ta = mapInUserContext(pool.get(sct.getName))
      typeargs += ta.get
    }
  }

  override def visitArrayTypeSignature(a: ArrayTypeSignature): Unit = {}
  override def visitTypeVariableSignature(tv: TypeVariableSignature): Unit = {}
  override def visitWildcard(w: Wildcard): Unit = {}
  override def visitMethodTypeSignature(ms: MethodTypeSignature): Unit = {}
  override def visitFormalTypeParameter(ftp: FormalTypeParameter): Unit = {}
  override def visitBottomSignature(b: BottomSignature): Unit = {}
  override def visitByteSignature(b: ByteSignature): Unit = {}
  override def visitBooleanSignature(b: BooleanSignature): Unit = {}
  override def visitShortSignature(s: ShortSignature): Unit = {}
  override def visitCharSignature(c: CharSignature): Unit = {}
  override def visitIntSignature(i: IntSignature): Unit = {}
  override def visitLongSignature(l: LongSignature): Unit = {}
  override def visitFloatSignature(f: FloatSignature): Unit = {}
  override def visitDoubleSignature(d: DoubleSignature): Unit = {}
  override def visitVoidDescriptor(v: VoidDescriptor): Unit = {}

}
