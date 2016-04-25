package de.ust.skill.javacf

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

class SignatureVisitor()
    extends sun.reflect.generics.visitor.Visitor[Type] {

  override def visitClassSignature(cs: ClassSignature): Unit = {}
  override def visitMethodTypeSignature(ms: MethodTypeSignature): Unit = {}
  override def getResult(): Type = { null }
  override def visitFormalTypeParameter(ftp: FormalTypeParameter): Unit = {}
  override def visitClassTypeSignature(ct: ClassTypeSignature): Unit = {}
  override def visitArrayTypeSignature(a: ArrayTypeSignature): Unit = {}
  override def visitTypeVariableSignature(tv: TypeVariableSignature): Unit = {}
  override def visitWildcard(w: Wildcard): Unit = {}
  override def visitSimpleClassTypeSignature(sct: SimpleClassTypeSignature): Unit = {}
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
